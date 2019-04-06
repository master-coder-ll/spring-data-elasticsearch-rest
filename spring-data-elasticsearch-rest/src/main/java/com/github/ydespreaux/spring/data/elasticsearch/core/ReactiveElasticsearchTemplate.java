/*
 * Copyright (C) 2018 Yoann Despréaux
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; see the file COPYING . If not, write to the
 * Free Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * Please send bugreports with examples or suggestions to yoann.despreaux@believeit.fr
 */

package com.github.ydespreaux.spring.data.elasticsearch.core;

import com.github.ydespreaux.spring.data.elasticsearch.client.reactive.ReactiveRestElasticsearchClient;
import com.github.ydespreaux.spring.data.elasticsearch.core.converter.ElasticsearchConverter;
import com.github.ydespreaux.spring.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import com.github.ydespreaux.spring.data.elasticsearch.core.query.*;
import com.github.ydespreaux.spring.data.elasticsearch.core.request.config.RolloverConfig;
import com.github.ydespreaux.spring.data.elasticsearch.core.triggers.TriggerManager;
import com.github.ydespreaux.spring.data.elasticsearch.core.triggers.reactive.ReactiveRolloverTrigger;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.admin.indices.rollover.RolloverRequest;
import org.elasticsearch.action.admin.indices.rollover.RolloverResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchResponseSections;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.GetAliasesResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.reactivestreams.Publisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

import static org.elasticsearch.client.Requests.refreshRequest;

@Slf4j
public class ReactiveElasticsearchTemplate extends ElasticsearchTemplateSupport implements ReactiveElasticsearchOperations {

    private final ReactiveRestElasticsearchClient client;

    private final ElasticsearchExceptionTranslator exceptionTranslator;

    /**
     * Construct an instance with the given client and elasticsearchConverter parameters.
     *
     * @param client                 the given client.
     * @param elasticsearchConverter the given elasticsearchConverter.
     * @param resultsMapper          the given result mapper
     */
    public ReactiveElasticsearchTemplate(
            ReactiveRestElasticsearchClient client,
            ElasticsearchConverter elasticsearchConverter,
            ResultsMapper resultsMapper,
            TriggerManager triggerManager) {
        super(elasticsearchConverter, resultsMapper, triggerManager);
        this.client = client;
        this.exceptionTranslator = new ElasticsearchExceptionTranslator();
    }

    /**
     * Obtain the {@link ReactiveRestElasticsearchClient} to operate upon.
     *
     * @return never {@literal null}.
     */
    protected ReactiveRestElasticsearchClient getClient() {
        return this.client;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations#exctute(ClientCallback)
     */
    @Override
    public <T> Publisher<T> execute(ClientCallback<Publisher<T>> callback) {
        return Flux.defer(() -> callback.doWithClient(client)).onErrorMap(this::translateException);
    }

    private Throwable translateException(Throwable throwable) {
        RuntimeException exception = throwable instanceof RuntimeException ? (RuntimeException) throwable
                : new RuntimeException(throwable.getMessage(), throwable);
        Throwable potentiallyTranslatedException = exceptionTranslator.translateExceptionIfPossible(exception);
        return potentiallyTranslatedException != null ? potentiallyTranslatedException : throwable;
    }


    /**
     * method checking the existance of the given indexName.
     *
     * @param indexName the given indexName.
     * @return true if indexName exist in elastic continueScroll.
     */
    @Override
    public Mono<Boolean> indexExists(String indexName) {
        Objects.requireNonNull(indexName);
        GetIndexRequest request = this.requestsBuilder().getIndexRequest(indexName);
        return Mono.from(execute(client -> client.indicesExist(request, RequestOptions.DEFAULT)))
                .onErrorResume(error -> Mono.error(new ElasticsearchException("Failed to get index request: " + request.toString(), error)));
    }

    /**
     * @param indexName the index name
     * @return true if the index name was created
     */
    @Override
    public Mono<Boolean> createIndex(Alias alias, String indexName) {
        Assert.notNull(indexName, "No index defined for Query");
        return doCreateIndex(this.requestsBuilder().createIndexRequest(alias, indexName));
    }

    /**
     * @param clazz
     * @return
     */
    @Override
    public <T> Mono<Boolean> createIndex(Class<T> clazz) {
        ElasticsearchPersistentEntity<T> persistentEntity = getPersistentEntityFor(clazz);
        if (persistentEntity.isRolloverIndex()) {
            return createRolloverIndex(persistentEntity);
        } else {
            return createIndex(persistentEntity);
        }
    }

    /**
     * @param aliasReader
     * @param aliasWriter
     * @param indexName
     * @return
     */
    @Override
    public Mono<Boolean> createRolloverIndex(Alias aliasReader, Alias aliasWriter, String indexName) {
        return doCreateIndex(this.requestsBuilder().createRolloverIndex(aliasReader, aliasWriter, generateRolloverIndexName(indexName)));
    }

    @Override
    public Mono<Boolean> createIndexWithSettingsAndMapping(Alias alias, String indexName, String indexPath) {
        return doCreateIndex(this.requestsBuilder().createIndexRequest(alias, indexName, indexPath));
    }

    /**
     * @param aliasReader
     * @param aliasWriter
     * @param indexName
     * @param indexPath
     * @return
     */
    @Override
    public Mono<Boolean> createRolloverIndexWithSettingsAndMapping(Alias aliasReader, Alias aliasWriter, String indexName, String indexPath) {
        return doCreateIndex(this.requestsBuilder().createRolloverIndex(aliasReader, aliasWriter, generateRolloverIndexName(indexName), indexPath));
    }

    /**
     * @param request
     * @return
     */
    private Mono<Boolean> doCreateIndex(CreateIndexRequest request) {
        return Mono.from(execute(client -> client.createIndex(request, RequestOptions.DEFAULT)))
                .map(CreateIndexResponse::isAcknowledged)
                .onErrorResume(error -> Mono.error(new ElasticsearchException("Failed to create index: " + request.toString(), error)));
    }

    /**
     * @param persistentEntity
     * @param <T>
     * @return
     */
    private <T> Mono<Boolean> createIndex(ElasticsearchPersistentEntity<T> persistentEntity) {
        String indexReader = persistentEntity.getAliasOrIndexReader();
        if (!persistentEntity.createIndex()) {
            return Mono.just(false);
        }
        return indexExists(indexReader)
                .filter(exists -> !exists)
                .map(exists -> persistentEntity)
                .flatMap(pe -> {
                    String newIndexName = persistentEntity.getIndexName();
                    if (persistentEntity.isIndexTimeBased()) {
                        return createIndex(newIndexName);
                    } else if (StringUtils.hasText(persistentEntity.getIndexSettingAndMappingPath())) {
                        return createIndexWithSettingsAndMapping(persistentEntity.getAlias(), newIndexName, persistentEntity.getIndexSettingAndMappingPath());
                    } else {
                        return createIndex(persistentEntity.getAlias(), newIndexName);
                    }
                });
    }

    /**
     * @param persistentEntity
     * @param <T>
     * @return
     */
    private <T> Mono<Boolean> createRolloverIndex(ElasticsearchPersistentEntity<T> persistentEntity) {
        if (!persistentEntity.createIndex()) {
            return Mono.just(false);
        }
        RolloverConfig rolloverConfig = persistentEntity.getRolloverConfig();
        return indexExists(rolloverConfig.getAlias().getName())
                .filter(exists -> !exists)
                .map(exists -> persistentEntity)
                .flatMap(pe -> {
                    String newIndexName = persistentEntity.getIndexName();
                    if (persistentEntity.isIndexTimeBased()) {
                        return createRolloverIndex(null, rolloverConfig.getDefaultAlias(), newIndexName)
                                .doOnSuccess(created -> { startRolloverTrigger(persistentEntity); });
                    } else if (!StringUtils.isEmpty(persistentEntity.getIndexSettingAndMappingPath())) {
                        return createRolloverIndexWithSettingsAndMapping(persistentEntity.getAlias(), rolloverConfig.getDefaultAlias(), newIndexName, persistentEntity.getIndexSettingAndMappingPath())
                                .doOnSuccess(created -> { startRolloverTrigger(persistentEntity); });
                    } else {
                        return createRolloverIndex(persistentEntity.getAlias(), rolloverConfig.getDefaultAlias(), newIndexName)
                                .doOnSuccess(created -> { startRolloverTrigger(persistentEntity); });
                    }
                });
    }

    private <T> void startRolloverTrigger(ElasticsearchPersistentEntity<T> persistentEntity){
        RolloverConfig rolloverConfig = persistentEntity.getRolloverConfig();
        RolloverConfig.TriggerConfig triggerConfig = rolloverConfig.getTrigger();
        if (triggerConfig.isEnabled()) {
            getTriggerManager().startTrigger(new ReactiveRolloverTrigger(this, persistentEntity, triggerConfig.getCronExpression()));
        }
    }

    /**
     * @param aliasName
     * @param newIndexName
     * @param indexPath
     * @param conditions
     * @return
     */
    @Override
    public Mono<Boolean> rolloverIndex(String aliasName, String newIndexName, String indexPath, RolloverConfig.RolloverConditions conditions) {
        RolloverRequest request = this.requestsBuilder().rolloverRequest(aliasName, newIndexName, indexPath, conditions);
        return Mono.from(execute(client -> client.rollover(request, RequestOptions.DEFAULT)))
                .map(RolloverResponse::isAcknowledged)
                .onErrorResume(error -> Mono.error(new ElasticsearchException("Failed to rollover index request: " + request.toString(), error)));
    }

    /**
     * @param entityClass
     * @return
     */
    @Override
    public <T> Mono<Boolean> rolloverIndex(Class<T> entityClass) {
        ElasticsearchPersistentEntity<T> persistentEntity = getPersistentEntityFor(entityClass);
        RolloverConfig rollover = persistentEntity.getRolloverConfig();
        return rolloverIndex(
                rollover.getAlias().getName(),
                null,
                persistentEntity.getIndexSettingAndMappingPath(),
                rollover.getConditions()
        );
    }

    /**
     * Delete index
     *
     * @param indexName index name
     * @return true if the index was deleted
     */
    @Override
    public Mono<Boolean> deleteIndexByName(String indexName) {
        Objects.requireNonNull(indexName);
        DeleteIndexRequest request = this.requestsBuilder().deleteIndexRequest(indexName);
        return Mono.from(execute(client -> client.deleteIndex(request, RequestOptions.DEFAULT)))
                .map(AcknowledgedResponse::isAcknowledged)
                .onErrorResume(IndexNotFoundException.class, error -> Mono.just(false))
                .onErrorResume(error -> Mono.error(buildDeleteException((Exception)error, request)));
    }

    /**
     * Delete all indices for a aliasOrIndex
     *
     * @param aliasName he given aliasName.
     */
    @Override
    public Mono<Boolean> deleteIndexByAlias(String aliasName) {
        Objects.requireNonNull(aliasName);
        GetAliasesRequest request = new GetAliasesRequest(aliasName);
        return Mono.from(execute(client -> client.getAlias(request, RequestOptions.DEFAULT)))
                .map(GetAliasesResponse::getAliases)
                .map(Map::keySet)
                .flatMapMany(Flux::fromIterable)
                .flatMap(this::deleteIndexByName)
                .then(Mono.just(true))
                .onErrorResume(error -> Mono.error(new ElasticsearchException("Error for get aliases request: " + request.toString(), error)));
    }

    /**
     * @see ElasticsearchOperations#refresh(Class)  method
     */
    @Override
    public <T> Mono<Void> refresh(Class<T> clazz) {
        ElasticsearchPersistentEntity<T> persistentEntity = getPersistentEntityFor(clazz);
        if (persistentEntity.isRolloverIndex()) {
            return this.refresh(persistentEntity.getAliasOrIndexWriter());
        } else {
            return this.refresh(persistentEntity.getAliasOrIndexReader());
        }
    }

    /**
     * @param indexName the index name
     */
    @Override
    public Mono<Void> refresh(String indexName) {
        Assert.notNull(indexName, "No index defined for refresh()");
        return Mono.from(execute(client -> client.refresh(refreshRequest(indexName), RequestOptions.DEFAULT)))
                .onErrorResume(IndexNotFoundException.class, error -> Mono.empty())
                .onErrorResume(error -> Mono.error(new ElasticsearchException("failed to refresh index: " + indexName, error)))
                .then();
    }

    /**
     * Index the given T entity, for the geiven clazz.
     *
     * @param entity the given entity.
     * @param clazz  the gievn {@link Class}.
     * @return T the indexed entity.
     */
    @Override
    public <T> Mono<T> index(T entity, Class<T> clazz) {
        ElasticsearchPersistentEntity<T> persistentEntity = getPersistentEntityFor(clazz);
        IndexRequest request = this.requestsBuilder().indexRequest(entity, persistentEntity, getResultsMapper());
        return doIndex(request)
                .map(response -> {
                    persistentEntity.setPersistentEntity(entity, response);
                    return entity;
                })
                .onErrorResume(error -> Mono.error(new ElasticsearchException("Error while index for request: " + request.toString(), error)));
    }

    /**
     * Bulk index operation for the given {@link List} of entities, and gievn {@link Class}.
     *
     * @param publisher the given entities {@link List}.
     * @param clazz    the given {@link Class}.
     * @return documents indexed
     */
    @Override
    public <T> Flux<T> bulkIndex(Flux<T> publisher, Class<T> clazz) {
        ElasticsearchPersistentEntity<T> persistentEntity = getPersistentEntityFor(clazz);
        return publisher
                .collectList()
                .map(entities -> {
                    BulkRequest bulkRequest = new BulkRequest();
                    entities.forEach(entity -> bulkRequest.add(this.requestsBuilder().indexRequest(entity, persistentEntity, this.getResultsMapper())));
                    return bulkRequest;
                })
                .flatMap(bulkRequest -> Mono.from(execute(client -> client.bulk(bulkRequest, RequestOptions.DEFAULT))))
                .flatMapMany(response -> {
                    checkForBulkUpdateFailure(response);
                    BulkItemResponse[] items = response.getItems();
                    for (int i = 0; i < publisher.count().block(); i++) {
                        persistentEntity.setPersistentEntity(publisher.elementAt(i).block(), items[i]);
                    }
                    return publisher;
                })
                .onErrorResume(error -> Mono.error(new ElasticsearchException("Error while bulk", error)));
    }

    /**
     * @param publisher all entities to index
     * @return the entities indexed
     */
    @Override
    public Flux<?> bulkIndex(Flux<?> publisher) {
        return publisher
                .collectList()
                .map(entities -> {
                    BulkRequest bulkRequest = new BulkRequest();
                    entities.forEach(entity -> {
                        Class entityClass = entity.getClass();
                        ElasticsearchPersistentEntity persistentEntity = getPersistentEntityFor(entityClass);
                        bulkRequest.add(this.requestsBuilder().indexRequest(entity, persistentEntity, this.getResultsMapper()));
                    });
                    return bulkRequest;
                })
                .flatMap(bulkRequest -> Mono.from(execute(client -> client.bulk(bulkRequest, RequestOptions.DEFAULT))))
                .flatMapMany(response -> {
                    checkForBulkUpdateFailure(response);
                    BulkItemResponse[] items = response.getItems();
                    for (int i = 0; i < publisher.count().block(); i++) {
                        Object entity = publisher.elementAt(i).block();
                        ElasticsearchPersistentEntity persistentEntity = getPersistentEntityFor(entity.getClass());
                        persistentEntity.setPersistentEntity(entity, items[i]);
                    }
                    return publisher;
                })
                .onErrorResume(error -> Mono.error(new ElasticsearchException("Error while bulk", error)));
    }


    /**
     * Find an elasticsearch document for the given clazz, and documentId.
     *
     * @param id         the given documentId.
     * @param entityType the given clazz.
     * @return the entity for the given documentId or null.
     */
    @Override
    public <T> Mono<T> findById(String id, Class<T> entityType) {
        Assert.notNull(id, "Id must not be null!");
        ElasticsearchPersistentEntity<T> persistentEntity = getPersistentEntityFor(entityType);
        return doFindById(id, persistentEntity.getAliasOrIndexReader(), persistentEntity.getTypeName())
                .filter(GetResponse::isExists)
                .map(it -> (T) this.getResultsMapper().mapResult(it, entityType))
                .onErrorResume(IndexNotFoundException.class, ex -> Mono.empty());
    }


    @Override
    public <T> Mono<T> findOne(CriteriaQuery query, Class<T> clazz) {
        return this.search(query, clazz).next();
    }

    @Override
    public <T> Mono<T> findOne(SearchQuery query, Class<T> clazz) {
        return this.search(query, clazz).next();
    }

    @Override
    public <T> Mono<T> findOne(StringQuery query, Class<T> clazz) {
        return this.search(query, clazz).next();
    }

    /**
     * @param query
     * @param clazz
     * @return
     */
    @Override
    public <T> Mono<Long> count(SearchQuery query, Class<T> clazz) {
        return executeCount(doCount(prepareCount(query, clazz), query));
    }

    /**
     *
     * @param query
     * @param clazz
     * @param <T>
     * @return
     */
    @Override
    public <T> Mono<Long> count(CriteriaQuery query, Class<T> clazz) {
        return executeCount(doCount(prepareCount(query, clazz), query));
    }

    /**
     *
     * @param query
     * @param clazz
     * @param <T>
     * @return
     */
    @Override
    public <T> Mono<Long> count(StringQuery query, Class<T> clazz) {
        return executeCount(doCount(prepareCount(query, clazz), query));
    }

    /**
     * @param clazz      the domain type
     * @param documentId the document id.
     * @return true if the document corresponding to the id exists
     */
    @Override
    public <T> Mono<Boolean> existsById(Class<T> clazz, String documentId) {
        Objects.requireNonNull(documentId, "documentId parameter canno't be null !");
        ElasticsearchPersistentEntity<T> persistentEntity = getPersistentEntityFor(clazz);
        if (persistentEntity.isIndexTimeBased() || persistentEntity.isRolloverIndex()) {
            FetchSourceFilter.FetchSourceFilterBuilder sourceBuilder = new FetchSourceFilter.FetchSourceFilterBuilder();
            sourceBuilder.withIncludes("id");
            SearchQuery query = new NativeSearchQuery.NativeSearchQueryBuilder()
                    .withQuery(QueryBuilders.termQuery("_id", documentId))
                    .withSourceFilter(sourceBuilder.build())
                    .withPageable(PageRequest.of(0, 1))
                    .build();
            return existsByQuery(query, clazz);
        } else {
            GetRequest request = this.requestsBuilder().getRequest(persistentEntity.getAliasOrIndexReader(), persistentEntity.getTypeName(), documentId);
            return Mono.from(execute(client -> client.exists(request, RequestOptions.DEFAULT)))
                    .onErrorResume(IndexNotFoundException.class, error -> Mono.just(false))
                    .onErrorResume(error -> Mono.error( new ElasticsearchException("Error for delete index request: " + request.toString(), error)));
        }
    }

    /**
     * @param query
     * @param javaType
     * @return
     */
    @Override
    public <T> Mono<Boolean> existsByQuery(CriteriaQuery query, Class<T> javaType) {
        return count(query, javaType).map(totalHits -> totalHits > 0);
    }

    @Override
    public <T> Mono<Boolean> existsByQuery(SearchQuery query, Class<T> javaType) {
        return count(query, javaType).map(totalHits -> totalHits > 0);
    }

    @Override
    public <T> Mono<Boolean> existsByQuery(StringQuery query, Class<T> javaType) {
        return count(query, javaType).map(totalHits -> totalHits > 0);
    }

    /**
     * Delete all the documents for the given clazz
     *
     * @param clazz the given clazz.
     */
    @Override
    public <T> Mono<Void> deleteAll(Class<T> clazz) {
        ElasticsearchPersistentEntity<T> persistentEntity = getPersistentEntityFor(clazz);
        if (persistentEntity.isRolloverIndex()) {
            RolloverConfig rollover = persistentEntity.getRolloverConfig();
            return this.deleteIndexByAlias(persistentEntity.getAliasOrIndexWriter())
                .flatMap(deleted -> this.rolloverIndex(
                    persistentEntity.getAliasOrIndexWriter(),
                    persistentEntity.getIndexName(),
                    persistentEntity.getIndexSettingAndMappingPath(),
                    rollover.getConditions()))
                .then();
        } else if (persistentEntity.isIndexTimeBased()) {
            GetAliasesRequest request = this.requestsBuilder().getAliasesRequest(persistentEntity.getAliasOrIndexReader());
            return Mono.from(execute(client -> client.getAlias(request, RequestOptions.DEFAULT)))
                    .map(GetAliasesResponse::getAliases)
                    .map(Map::keySet)
                    .flatMapMany(Flux::fromIterable)
                    .flatMap(this::deleteIndexByName)
                    .then()
                    .onErrorResume(error -> Mono.error(new ElasticsearchException("Error for get aliases request: " + request.toString(), error)));
        } else {
            return deleteByQuery(persistentEntity.getAliasOrIndexReader(), persistentEntity.getTypeName(), QueryBuilders.matchAllQuery());
        }
    }

    /**
     * Delete all the {@link List} of entities, for the given clazz.
     *
     * @param entities the {@link List} of entities.
     * @param clazz    the given clazz.
     */
    @Override
    public <T> Mono<Void> deleteAll(Collection<T> entities, Class<T> clazz) {
        ElasticsearchPersistentEntity<T> persistentEntity = getPersistentEntityFor(clazz);
        Set<String> ids = new HashSet<>();
        entities.forEach(entity -> {
            String id = persistentEntity.getPersistentEntityId(entity);
            if (id != null) {
                ids.add(id);
            }
        });
        if (!ids.isEmpty()) {
            if (persistentEntity.isRolloverIndex()) {
                return deleteByQuery(persistentEntity.getAliasOrIndexWriter(), persistentEntity.getTypeName(), QueryBuilders.termsQuery("_id", ids));
            } else {
                return deleteByQuery(persistentEntity.getAliasOrIndexReader(), persistentEntity.getTypeName(), QueryBuilders.termsQuery("_id", ids));
            }
        }
        return Mono.empty();
    }

    /**
     * delete the document for the given entity, and clazz
     *
     * @param entity the given entity.
     * @param clazz  the given clazz.
     */
    @Override
    public <T> Mono<Void> delete(T entity, Class<T> clazz) {
        ElasticsearchPersistentEntity<T> persistentEntity = getPersistentEntityFor(clazz);
        String id = persistentEntity.getPersistentEntityId(entity);
        if (id != null) {
            return this.deleteById(id, clazz);
        }
        return Mono.empty();
    }

    /**
     * delete the document for the given entity, and clazz
     *
     * @param query the given query.
     * @param clazz the given clazz.
     */
    @Override
    public <T> Mono<Void> delete(CriteriaQuery query, Class<T> clazz) {
        ElasticsearchPersistentEntity<T> persistentEntity = getPersistentEntityFor(clazz);
        Optional<QueryBuilder> queryBuilder = new CriteriaQueryProcessor().createQueryFromCriteria(query.getCriteria());
        Optional<QueryBuilder> filterBuilder = new CriteriaFilterProcessor().createFilterFromCriteria(query.getCriteria());
        if (persistentEntity.isRolloverIndex()) {
            return deleteByQuery(persistentEntity.getAliasOrIndexWriter(), persistentEntity.getTypeName(), queryBuilder.orElse(filterBuilder.orElse(null)));
        } else {
            return deleteByQuery(persistentEntity.getAliasOrIndexReader(), persistentEntity.getTypeName(), queryBuilder.orElse(filterBuilder.orElse(null)));
        }
    }

    /**
     * delete the document with the given documentId and clazz.
     *
     * @param documentId the given documentId.
     * @param clazz      the given clazz.
     */
    @Override
    public <T> Mono<Void> deleteById(String documentId, Class<T> clazz) {
        Assert.notNull(documentId, "documentId must not be null!");
        ElasticsearchPersistentEntity<T> persistentEntity = getPersistentEntityFor(clazz);
        QueryBuilder queryBuilder = QueryBuilders.termQuery("_id", documentId);
        if (persistentEntity.isIndexTimeBased()) {
            return deleteByQuery(persistentEntity.getAliasOrIndexReader(), persistentEntity.getTypeName(), queryBuilder);
        } else {
            String indexName = persistentEntity.isRolloverIndex() ? persistentEntity.getAliasOrIndexWriter() : persistentEntity.getAliasOrIndexReader();
            DeleteRequest request = this.requestsBuilder().deleteRequest(indexName, persistentEntity.getTypeName(), documentId);
            return Mono.from(execute(client -> client.delete(request, RequestOptions.DEFAULT)))
                    .then()
                    .onErrorResume(error -> Mono.error(new ElasticsearchException("Error for delete request: " + request.toString(), error)));
        }
    }

    /**
     * @param query
     */
    private Mono<Void> deleteByQuery(String indexName, String typeName, QueryBuilder query) {
        DeleteByQueryRequest request = this.requestsBuilder().deleteBy(indexName, typeName, query);
        return Mono.from(execute(client -> client.deleteBy(request)))
                .then()
                .onErrorResume(IndexNotFoundException.class, error -> Mono.empty())
                .onErrorResume(error -> Mono.error(new ElasticsearchException("Error while deleting bulk: " + request.toString(), error)));
    }

    /**
     * @param searchRequest
     * @return
     */
    private Mono<Long> executeCount(SearchRequest searchRequest) {
        return Mono.from(execute(client -> client.search(searchRequest)))
                .map(searchResponse -> searchResponse.getHits().getTotalHits())
                .onErrorResume(IndexNotFoundException.class, error -> Mono.just(0L));
    }

    /**
     * Search with the given {@link SearchRequest} continueScroll, and given {@link Class} clazz.
     *
     * @param query the given {@link SearchRequest} instance.
     * @param clazz  the given clazz.
     * @return a {@link List} of the method generic type.
     */
    @Override
    public <S extends T, T> Flux<S> search(SearchQuery query, Class<T> clazz) {
        return executeSearch(doSearch(prepareSearch(query, clazz), query))
                .map(SearchResponse::getHits)
                .flatMapMany(hits -> Flux.fromIterable(getResultsMapper().mapEntity(hits, clazz)));
    }

    /**
     * @param query
     * @param clazz
     * @return
     */
    @Override
    public <S extends T, T> Flux<S> search(CriteriaQuery query, Class<T> clazz) {
        return executeSearch(doSearch(prepareSearch(query, clazz), query))
                .map(SearchResponse::getHits)
                .flatMapMany(hits -> Flux.fromIterable(getResultsMapper().mapEntity(hits, clazz)));
    }

    /**
     * @param query
     * @param clazz
     * @return
     */
    @Override
    public <S extends T, T> Flux<S> search(StringQuery query, Class<T> clazz) {
        return executeSearch(doSearch(prepareSearch(query, clazz), query))
                .map(SearchResponse::getHits)
                .flatMapMany(hits -> Flux.fromIterable(getResultsMapper().mapEntity(hits, clazz)));
    }

    /**
     * @param query
     * @param resultsExtractor
     * @return
     */
    @Override
    public <T> Flux<T> search(SearchQuery query, ResultsExtractor<Flux<T>> resultsExtractor) {
        return executeSearch(doSearch(prepareSearch(query, Optional.of(query.getQuery())), query))
                .flatMapMany(response -> resultsExtractor.extract(response));
    }

    /**
     * @param request
     * @return
     */
    private Mono<SearchResponse> executeSearch(SearchRequest request) {
        return Mono.from(execute(client -> client.search(request, RequestOptions.DEFAULT)))
                .onErrorResume(IndexNotFoundException.class, error -> Mono.just(new SearchResponse(
                        new SearchResponseSections(SearchHits.empty(), null, null, false, true, null, 0),
                        null, 1, 1, 0, 1000, null, null)))
                .onErrorResume(error -> Mono.error(buildSearchException((Exception)error, request)));
    }

    /**
     *
     * @param query
     * @param builder
     * @return
     */
    protected SearchRequest prepareSearch(Query query, Optional<QueryBuilder> builder) {
        SearchRequest request = super.prepareSearch(query, builder);
        SearchSourceBuilder sourceBuilder = request.source();
        sourceBuilder.size(10000);
        return request;
    }


//    /**
//     * @param query
//     * @param clazz
//     * @return
//     */
//    @Override
//    public <T> Mono<Long> count(SearchQuery query, Class<T> clazz) {
//        return null;
//    }
//
//    /**
//     * @param query
//     * @param clazz
//     * @return
//     */
//    @Override
//    public <T> Mono<Long> count(CriteriaQuery query, Class<T> clazz) {
//        return null;
//    }
//
//    /**
//     * @param clazz      the domain type
//     * @param documentId the document id.
//     * @return true if the document corresponding to the id exists
//     */
//    @Override
//    public <T> Mono<Boolean> existsById(Class<T> clazz, String documentId) {
//        return null;
//    }
//
//    /**
//     * @param query
//     * @param javaType
//     * @return
//     */
//    @Override
//    public <T> Mono<Boolean> existsByQuery(CriteriaQuery query, Class<T> javaType) {
//        return null;
//    }
//
//    @Override
//    public <T> Mono<Boolean> existsByQuery(SearchQuery query, Class<T> javaType) {
//        return null;
//    }
//
//    @Override
//    public <T> Mono<Boolean> existsByQuery(StringQuery query, Class<T> javaType) {
//        return null;
//    }
//
//    /**
//     * Delete all the documents for the given clazz
//     *
//     * @param clazz the given clazz.
//     */
//    @Override
//    public <T> Mono<Void> deleteAll(Class<T> clazz) {
//        return null;
//    }
//
//    /**
//     * Delete all the {@link List} of entities, for the given clazz.
//     *
//     * @param entities the {@link List} of entities.
//     * @param clazz    the given clazz.
//     */
//    @Override
//    public <T> Mono<Void> deleteAll(List<T> entities, Class<T> clazz) {
//        return null;
//    }
//
//    /**
//     * delete the document for the given entity, and clazz
//     *
//     * @param entity the given entity.
//     * @param clazz  the given clazz.
//     */
//    @Override
//    public <T> Mono<Void> delete(T entity, Class<T> clazz) {
//        return null;
//    }
//
//    /**
//     * delete the document for the given entity, and clazz
//     *
//     * @param query the given query.
//     * @param clazz the given clazz.
//     */
//    @Override
//    public <T> Mono<Void> delete(CriteriaQuery query, Class<T> clazz) {
//        return null;
//    }
//
//    /**
//     * delete the document with the given documentId and clazz.
//     *
//     * @param documentId the given documentId.
//     * @param clazz      the given clazz.
//     */
//    @Override
//    public <T> Mono<Void> deleteById(String documentId, Class<T> clazz) {
//        return null;
//    }
//
//    /**
//     * @param suggestion
//     * @param indices
//     * @return
//     */
//    @Override
//    public Mono<SearchResponse> suggest(SuggestBuilder suggestion, String... indices) {
//        return null;
//    }
//
//    /**
//     * @param suggestion
//     * @param indices
//     * @param extractor
//     * @return
//     */
//    @Override
//    public <R> Flux<R> suggest(SuggestBuilder suggestion, String[] indices, ResultsExtractor<R> extractor) {
//        return null;
//    }
//
//    /**
//     * @param suggestion
//     * @param clazz
//     * @param extractor
//     * @return
//     */
//    @Override
//    public <R, T> Flux<R> suggest(SuggestBuilder suggestion, Class<T> clazz, ResultsExtractor<R> extractor) {
//        return null;
//    }

    /**
     * Customization hook on the actual execution result {@link Publisher}.
     * You know what you're doing here? Well fair enough, go ahead on your own risk.
     *
     * @param request the already prepared {@link IndexRequest} ready to be executed.
     * @return a {@link Mono} emitting the result of the operation.
     */
    protected Mono<IndexResponse> doIndex(IndexRequest request) {
        return Mono.from(execute(client -> client.index(request)));
    }

    private Mono<GetResponse> doFindById(String id, String indexName, String typeName) {
        return Mono.defer(() -> doFindById(new GetRequest(indexName, typeName, id)));
    }

    /**
     * Customization hook on the actual execution result {@link Publisher}.
     *
     * @param request the already prepared {@link GetRequest} ready to be executed.
     * @return a {@link Mono} emitting the result of the operation.
     */
    protected Mono<GetResponse> doFindById(GetRequest request) {
        return Mono.from(execute(client -> client.get(request)));
    }

}
