/* * Copyright (C) 2018 Yoann Despréaux * * This program is free software; you can redistribute it and/or modify * it under the terms of the GNU General Public License as published by * the Free Software Foundation; either version 2 of the License, or * (at your option) any later version. * * This program is distributed in the hope that it will be useful, * but WITHOUT ANY WARRANTY; without even the implied warranty of * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the * GNU General Public License for more details. * * You should have received a copy of the GNU General Public License * along with this program; see the file COPYING . If not, write to the * Free Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA. * * Please send bugreports with examples or suggestions to yoann.despreaux@believeit.fr */package com.github.ydespreaux.spring.data.elasticsearch.core;import com.github.ydespreaux.spring.data.elasticsearch.core.converter.ElasticsearchConverter;import com.github.ydespreaux.spring.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;import com.github.ydespreaux.spring.data.elasticsearch.core.query.*;import com.github.ydespreaux.spring.data.elasticsearch.core.request.config.RolloverConfig;import com.github.ydespreaux.spring.data.elasticsearch.core.triggers.TriggerManager;import org.elasticsearch.action.admin.indices.alias.Alias;import org.elasticsearch.action.search.SearchRequest;import org.elasticsearch.client.Request;import org.elasticsearch.client.Response;import org.elasticsearch.common.Nullable;import org.springframework.core.io.Resource;import org.springframework.data.domain.Page;import java.io.IOException;import java.time.Duration;import java.util.Collection;import java.util.List;import java.util.Optional;/** * define the {@link ElasticsearchOperations} methods. * * @author Yoann Despréaux * @since 1.0.0 */public interface ElasticsearchOperations {    /**     * Give the {@link ElasticsearchPersistentEntity} for the given {@link Class}.     *     * @param clazz the given {@link Class}.     * @param <T>   method generic     * @return ElasticsearchPersistentEntity the persitant entity for the given {@link Class} parameter.     */    <T> ElasticsearchPersistentEntity<T> getPersistentEntityFor(Class<T> clazz);    /**     * @return the elasticsearch converter     */    ElasticsearchConverter getElasticsearchConverter();    /**     * @return     */    TriggerManager getTriggerManager();    /**     * @return     */    ResultsMapper getResultsMapper();    //***************************************    // Request operations    //***************************************    /**     * @param request the request     * @return the response     * @throws IOException if the request failed     */    Response performRequest(Request request) throws IOException;    //***************************************    // Administration operations    //***************************************    /**     * the template is existing.     *     * @param templateName the given template name.     * @return true if the given templateName exist, else false.     */    boolean templateExists(String templateName);    /**     * Create a new template in elastic continueScroll, with the gievn templateName, location, and createOnly parameters.     *     * @param templateName the given templateName     * @param location     the given location.     * @param createOnly   true if only ceate the template esle false.     */    void createTemplate(String templateName, String location, Boolean createOnly);    /**     * Create a new template in elastic continueScroll, with the givens templateName, locations,    nd createOnly parameters.     *     * @param templateName the given templateName     * @param locations    the given location.     * @param createOnly   true if only ceate the template esle false.     */    void createTemplate(String templateName, List<Resource> locations, Boolean createOnly);    /**     * Delete the template with the given templateName parameter.     *     * @param templateName the given templateName.     */    void deleteTemplate(String templateName);    /**     * Delete index     *     * @param indexName index name     * @return true if the index was deleted     */    boolean deleteIndexByName(String indexName);    /**     * Delete all indices for a aliasOrIndex     *     * @param aliasName he given aliasName.     */    void deleteIndexByAlias(String aliasName);    /**     * method checking the existance of the given indexName.     *     * @param indexName the given indexName.     * @return true if indexName exist in elastic continueScroll.     */    boolean indexExists(String indexName);    /**     * @param indexName the index name     * @return true if the index name was created     */    default boolean createIndex(String indexName) {        return createIndex(null, indexName);    }    /**     * @param alias     * @param indexName     * @return     */    boolean createIndex(Alias alias, String indexName);    /**     * @param clazz     * @param <T>     * @return     */    <T> boolean createIndex(Class<T> clazz);    /**     *     * @param aliasWriter     * @param indexName     * @return     */    default boolean createRolloverIndex(Alias aliasWriter, String indexName) {        return this.createRolloverIndex(null, aliasWriter, indexName);    }    /**     *     * @param aliasReader     * @param aliasWriter     * @param indexName     * @return     */    boolean createRolloverIndex(Alias aliasReader, Alias aliasWriter, String indexName);    /**     * @param indexName the index name     * @param indexPath the path of the json index file     * @return true if the index was created     */    default boolean createIndexWithSettingsAndMapping(String indexName, String indexPath) {        return this.createIndexWithSettingsAndMapping(null, indexName, indexPath);    }    boolean createIndexWithSettingsAndMapping(Alias alias, String indexName, String indexPath);    /**     *     * @param aliasWriter     * @param indexName     * @param indexPath     * @return     */    default boolean createRolloverIndexWithSettingsAndMapping(Alias aliasWriter, String indexName, String indexPath) {        return createRolloverIndexWithSettingsAndMapping(null, aliasWriter, indexName, indexPath);    }    /**     *     * @param aliasReader     * @param aliasWriter     * @param indexName     * @param indexPath     * @return     */    boolean createRolloverIndexWithSettingsAndMapping(Alias aliasReader, Alias aliasWriter, String indexName, String indexPath);    /**     * @param aliasName     * @param newIndexName     * @param indexPath     * @param conditions     * @return     */    boolean rolloverIndex(String aliasName, @Nullable String newIndexName, @Nullable String indexPath, RolloverConfig.RolloverConditions conditions);    /**     *     * @param entityClass     * @param <T>     * @return     */    <T> boolean rolloverIndex(Class<T> entityClass);    //***************************************    // Index / continueScroll operations    //***************************************    /**     * Index the given T entity, for the geiven clazz.     *     * @param entity the given entity.     * @param clazz  the gievn {@link Class}.     * @param <T>    generic method     * @return T the indexed entity.     */    <T> T index(T entity, Class<T> clazz);    /**     * Bulk index operation for the given {@link List} of entities, and gievn {@link Class}.     *     * @param entities the given entities {@link List}.     * @param clazz    the given {@link Class}.     * @param <T>      the {@link List} of indexed entities.     * @return documents indexed     */    <T> List<T> bulkIndex(List<T> entities, Class<T> clazz);    /**     * @param entities all entities to index     * @return the entities indexed     */    List bulkIndex(List<?> entities);    /**     * Find an elasticsearch document for the given clazz, and documentId.     *     * @param clazz      the given clazz.     * @param documentId the given documentId.     * @param <T>        the document     * @return the entity for the given documentId or null.     */    <T> Optional<T> findById(Class<T> clazz, String documentId);    <T> Optional<T> findOne(CriteriaQuery query, Class<T> clazz);    <T> Optional<T> findOne(SearchQuery query, Class<T> clazz);    <T> Optional<T> findOne(StringQuery query, Class<T> clazz);    /**     * @param query     * @param clazz     * @param <T>     * @return     */    <T> long count(SearchQuery query, Class<T> clazz);    /**     * @param query     * @param clazz     * @param <T>     * @return     */    <T> long count(CriteriaQuery query, Class<T> clazz);    /**     * @param clazz      the domain type     * @param documentId the document id.     * @param <T>        method generic.     * @return true if the document corresponding to the id exists     */    <T> Boolean existsById(Class<T> clazz, String documentId);    /**     * @param query     * @param javaType     * @param <T>     * @return     */    <T> Boolean existsByQuery(CriteriaQuery query, Class<T> javaType);    /**     * Delete all the documents for the given clazz     *     * @param clazz the given clazz.     * @param <T>   method generic.     */    <T> void deleteAll(Class<T> clazz);    /**     * Delete all the {@link List} of entities, for the given clazz.     *     * @param entities the {@link List} of entities.     * @param clazz    the given clazz.     * @param <T>      method generic.     */    <T> void deleteAll(Collection<T> entities, Class<T> clazz);    /**     * delete the document for the given entity, and clazz     *     * @param entity the given entity.     * @param clazz  the given clazz.     * @param <T>    method generic.     */    <T> void delete(T entity, Class<T> clazz);    /**     * delete the document for the given entity, and clazz     *     * @param query the given query.     * @param clazz the given clazz.     * @param <T>   method generic.     */    <T> void delete(CriteriaQuery query, Class<T> clazz);    /**     * delete the document with the given documentId and clazz.     *     * @param documentId the given documentId.     * @param clazz      the given clazz.     * @param <T>        method generic.     */    <T> void deleteById(String documentId, Class<T> clazz);    /**     * refresh the elasticsearch index for the given clazz     *     * @param clazz the given clazz.     * @param <T>   method generic.     */    <T> void refresh(Class<T> clazz);    /**     * @param indexName the index name     */    void refresh(String indexName);    <T> Boolean existsByQuery(SearchQuery query, Class<T> javaType);    <T> Boolean existsByQuery(StringQuery query, Class<T> javaType);    /**     * Search with the given {@link SearchRequest} continueScroll, and given {@link Class} clazz.     *     * @param search the given {@link SearchRequest} instance.     * @param clazz  the given clazz.     * @param <T>    generic method.     * @return a {@link List} of the method generic type.     */    <S extends T, T> List<S> search(SearchQuery search, Class<T> clazz);    /**     * @param search     * @param clazz     * @param <T>     * @return     */    <S extends T, T> List<S> search(CriteriaQuery search, Class<T> clazz);    /**     * @param stringQuery     * @param clazz     * @param <T>     * @return     */    <S extends T, T> List<S> search(StringQuery stringQuery, Class<T> clazz);    /**     * Start the {@link Page}, with the given scrollTime, size, builder and clazz.     *     * @param searchQuery the given query.     * @param clazz       the given {@link Class} clazz.     * @param <T>         method generic.     * @return a {@link Page} of T instances.     */    <S extends T, T> Page<S> startScroll(SearchQuery searchQuery, Class<T> clazz);    /**     * Start the {@link Page}, with the given scrollTime, size, builder and clazz.     *     * @param scrollTime  the scroll time.     * @param searchQuery the given query.     * @param clazz       the given {@link Class} clazz.     * @param <T>         method generic.     * @return a {@link Page} of T instances.     */    <S extends T, T> Page<S> startScroll(Duration scrollTime, SearchQuery searchQuery, Class<T> clazz);    /**     * Start the {@link Page}, with the given scrollTime, size, builder and clazz.     *     * @param criteriaQuery the given query.     * @param clazz         the given {@link Class} clazz.     * @param <T>           method generic.     * @return a {@link Page} of T instances.     */    <S extends T, T> Page<S> startScroll(CriteriaQuery criteriaQuery, Class<T> clazz);    /**     * Start the {@link Page}, with the given scrollTime, size, builder and clazz.     *     * @param scrollTime    the scroll time.     * @param criteriaQuery the given query.     * @param clazz         the given {@link Class} clazz.     * @param <T>           method generic.     * @return a {@link Page} of T instances.     */    <S extends T, T> Page<S> startScroll(Duration scrollTime, CriteriaQuery criteriaQuery, Class<T> clazz);    /**     * Start the {@link Page}, with the given scrollTime, size, builder and clazz.     *     * @param stringQuery the given query.     * @param clazz       the given {@link Class} clazz.     * @param <T>         method generic.     * @return a {@link Page} of T instances.     */    <S extends T, T> Page<S> startScroll(StringQuery stringQuery, Class<T> clazz);    /**     * Start the {@link Page}, with the given scrollTime, size, builder and clazz.     *     * @param scrollTime  the scroll time.     * @param stringQuery the given query.     * @param clazz       the given {@link Class} clazz.     * @param <T>         method generic.     * @return a {@link Page} of T instances.     */    <S extends T, T> Page<S> startScroll(Duration scrollTime, StringQuery stringQuery, Class<T> clazz);    /**     * @param scrollTime  the given scrollId.     * @param searchQuery the given query.     * @param clazz       the item domain type     * @param mapper      the mapper to transform results     * @param <T>         method generic     * @return a {@link Page} of T instancess.     */    <S extends T, T> Page<S> startScroll(Duration scrollTime, SearchQuery searchQuery, Class<T> clazz, SearchResultMapper mapper);    /**     * @param scrollTime    the given scrollId.     * @param criteriaQuery the given query.     * @param clazz         the item domain type     * @param mapper        the mapper to transform results     * @param <T>           method generic     * @return a {@link Page} of T instancess.     */    <S extends T, T> Page<S> startScroll(Duration scrollTime, CriteriaQuery criteriaQuery, Class<T> clazz, SearchResultMapper mapper);    /**     * @param scrollTime  the given scrollId.     * @param stringQuery the given query.     * @param clazz       the item domain type     * @param mapper      the mapper to transform results     * @param <T>         method generic     * @return a {@link Page} of T instancess.     */    <S extends T, T> Page<S> startScroll(Duration scrollTime, StringQuery stringQuery, Class<T> clazz, SearchResultMapper mapper);    /**     * Continue the {@link Page} for the given scrollId, scrollTime, and clazz.     *     * @param scrollId   the given scrollId.     * @param scrollTime the scrol time.     * @param clazz      the given clazz.     * @param <T>        method generic.     * @return a {@link Page} of T instancess.     */    <S extends T, T> Page<S> continueScroll(@Nullable String scrollId, Duration scrollTime, Class<T> clazz);    /**     * @param scrollId   the scroll id     * @param scrollTime the scroll time     * @param clazz      the item data type     * @param mapper     the mapper to transform results     * @param <T>        the type of items     * @return a {@link Page} of T instancess.     */    <S extends T, T> Page<S> continueScroll(@Nullable String scrollId, Duration scrollTime, Class<T> clazz, SearchResultMapper mapper);    /**     * Clear the {@link Page} for the given scrollId.     *     * @param scrollId the given scrollId.     */    void clearScroll(String scrollId);    /**     * @param query     * @param resultsExtractor     * @param <T>     * @return     */    <T> T search(SearchQuery query, ResultsExtractor<T> resultsExtractor);    /**     *     * @param <T>     * @param query     * @param extractor     * @return     */    <T> T suggest(SuggestQuery query, ResultsExtractor<T> extractor);    /**     *     * @param <R>     * @param <T>     * @param query     * @param clazz     * @param extractor     * @return     */    <R, T> R suggest(SuggestQuery query, Class<T> clazz, ResultsExtractor<R> extractor);    /**     * @param query     * @param clazz     * @param <T>     * @return     */    <T> List hasChild(HasChildQuery query, Class<T> clazz);    /**     * @param query     * @param clazz     * @param <T>     * @return     */    <S extends T, T> List<S> hasParent(HasParentQuery query, Class<T> clazz);    /**     * @param query     * @param clazz     * @param <T>     * @return     */    <T> List<T> hasParentId(ParentIdQuery query, Class<T> clazz);}