/* * Copyright (C) 2018 Yoann Despréaux * * This program is free software; you can redistribute it and/or modify * it under the terms of the GNU General Public License as published by * the Free Software Foundation; either version 2 of the License, or * (at your option) any later version. * * This program is distributed in the hope that it will be useful, * but WITHOUT ANY WARRANTY; without even the implied warranty of * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the * GNU General Public License for more details. * * You should have received a copy of the GNU General Public License * along with this program; see the file COPYING . If not, write to the * Free Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA. * * Please send bugreports with examples or suggestions to yoann.despreaux@believeit.fr */package com.github.ydespreaux.spring.data.elasticsearch.repository;import com.github.ydespreaux.spring.data.elasticsearch.core.query.Criteria;import com.github.ydespreaux.spring.data.elasticsearch.core.scroll.ScrolledPage;import org.elasticsearch.index.query.QueryBuilder;import org.springframework.data.domain.Pageable;import org.springframework.data.domain.Sort;import org.springframework.data.repository.Repository;import org.springframework.lang.NonNull;import org.springframework.lang.Nullable;import java.time.Duration;import java.util.Collection;import java.util.List;import java.util.Optional;/** * @param <T> entity generic class * @param <K> key generic class * @author Yoann Despréaux * @since 1.0.0 */public interface ElasticsearchRepository<T, K> extends Repository<T, K> {    /**     * Retrieves an entity by its id.     *     * @param id must not be {@literal null}.     * @return the entity with the given id or {@literal Optional#empty()} if none found     * @throws IllegalArgumentException if {@code id} is {@literal null}.     */    Optional<T> findById(K id);    /**     * @param id the identifier     * @return true if the document exists     */    boolean existsById(K id);    /**     * @return     */    long count();    /**     * @param query     * @return     */    long count(QueryBuilder query);    /**     * @param criteria     * @return     */    long count(Criteria criteria);    /**     * Saves a given entity. Use the returned instance for further operations as the save operation might have changed the     * entity instance completely.     *     * @param entity must not be {@literal null}.     * @return the saved entity will never be {@literal null}.     */    T save(T entity);    /**     * Saves all given entities.     *     * @param entities must not be {@literal null}.     * @return the saved entities will never be {@literal null}.     * @throws IllegalArgumentException in case the given entity is {@literal null}.     */    List<T> save(List<T> entities);    /**     * Deletes the entity with the given id.     *     * @param id must not be {@literal null}.     * @throws IllegalArgumentException in case the given {@code id} is {@literal null}     */    void deleteById(K id);    /**     * Deletes a given entity.     *     * @param entity the entity     * @throws IllegalArgumentException in case the given entity is {@literal null}.     */    void delete(T entity);    /**     * Deletes the given entities.     *     * @param entities the entities     * @throws IllegalArgumentException in case the given {@link Iterable} is {@literal null}.     */    void deleteAll(Collection<T> entities);    /**     * Deletes all entities managed by the repository.     */    void deleteAll();    /**     *     */    void refresh();    /**     * @return     */    List<T> findAll();    /**     * @param pageable     * @return     */    ScrolledPage<T> findAll(Pageable pageable);    /**     * @param query the query     * @param sort  the sort     * @return items for the query     */    <S extends T> List<S> findByQuery(QueryBuilder query, @Nullable Sort sort);    /**     * @param criteria the query     * @param sort     the sort     * @return items for the query     */    <S extends T> List<S> findByQuery(Criteria criteria, @Nullable Sort sort);    /**     * @param query     * @param sort     * @return     */    <S extends D, D> List<S> findByQuery(QueryBuilder query, @Nullable Sort sort, Class<D> domainClass);    /**     * @param criteria     * @param sort     * @return     */    <S extends D, D> List<S> findByQuery(Criteria criteria, @Nullable Sort sort, Class<D> domainClass);    /**     * Start new continueScroll with scroll api     *     * @param query    the query     * @param pageable the pageable     * @return a new {@link ScrolledPage}     */    <S extends T> ScrolledPage<S> findByQuery(QueryBuilder query, Pageable pageable);    /**     * Start new continueScroll with scroll api     *     * @param criteria the query     * @param pageable the pageable     * @return a new {@link ScrolledPage}     */    <S extends T> ScrolledPage<S> findByQuery(Criteria criteria, Pageable pageable);    /**     * Start new continueScroll with scroll api     *     * @param query    the query     * @param pageable the pageable     * @return a new {@link ScrolledPage}     */    <S extends D, D> ScrolledPage<S> findByQuery(QueryBuilder query, Pageable pageable, Class<D> domainClass);    /**     * Start new continueScroll with scroll api     *     * @param criteria the query     * @param pageable the pageable     * @return a new {@link ScrolledPage}     */    <S extends D, D> ScrolledPage<S> findByQuery(Criteria criteria, Pageable pageable, Class<D> domainClass);    /**     * Continue continueScroll with scroll api     *     * @param scrollId     * @param scrollTime     * @return a new {@link ScrolledPage}     */    <S extends T> ScrolledPage<S> continueScroll(String scrollId, Duration scrollTime);    /**     * @param scrollId     * @param scrollTime     * @param domainClass     * @param <D>     * @return     */    <S extends D, D> ScrolledPage<S> continueScroll(String scrollId, Duration scrollTime, Class<D> domainClass);    /**     * @param scrollId the scroll id     */    void clearSearch(String scrollId);    /**     * @param query     * @return     */    List<T> suggest(String query);    /**     * @param query     * @return     */    <D> List<D> suggest(String query, Class<D> domainClass);    /**     * Returns all parents     *     * @return     */    List hasChild();    /**     * Returns parent documents which associated children have matched with query.     *     * @param query     * @return     */    List hasChildByQuery(@NonNull QueryBuilder query);    /**     * Returns parent documents which associated children have matched with query.     *     * @param criteria     * @return     */    List hasChildByQuery(@NonNull Criteria criteria);    /**     * Returns all children     *     * @param <S>     * @return     */    <S extends T> List<S> hasParent();    /**     * Returns child documents which associated parents have matched with query.     *     * @param query     * @param <S>     * @return     */    <S extends T> List<S> hasParentByQuery(@NonNull QueryBuilder query);    /**     * Returns child documents which associated parents have matched with query.     *     * @param criteria     * @param <S>     * @return     */    <S extends T> List<S> hasParentByQuery(@NonNull Criteria criteria);    /**     * Return child documents which associated parent id     *     * @param parentId     * @return     */    List<T> hasParentId(@NonNull String parentId);    /**     * Return child documents which associated parent id and children have matched with query.     *     * @param parentId     * @param criteria     * @return     */    List<T> hasParentId(@NonNull String parentId, @Nullable Criteria criteria);    /**     * Return child documents which associated parent id and children have matched with query.     *     * @param parentId     * @param query     * @return     */    List<T> hasParentId(@NonNull String parentId, @Nullable QueryBuilder query);}