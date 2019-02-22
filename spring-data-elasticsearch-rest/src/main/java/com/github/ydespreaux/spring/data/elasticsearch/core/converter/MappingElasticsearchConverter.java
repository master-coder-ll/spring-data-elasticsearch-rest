/* * Copyright (C) 2018 Yoann Despréaux * * This program is free software; you can redistribute it and/or modify * it under the terms of the GNU General Public License as published by * the Free Software Foundation; either version 2 of the License, or * (at your option) any later version. * * This program is distributed in the hope that it will be useful, * but WITHOUT ANY WARRANTY; without even the implied warranty of * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the * GNU General Public License for more details. * * You should have received a copy of the GNU General Public License * along with this program; see the file COPYING . If not, write to the * Free Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA. * * Please send bugreports with examples or suggestions to yoann.despreaux@believeit.fr */package com.github.ydespreaux.spring.data.elasticsearch.core.converter;import com.github.ydespreaux.spring.data.elasticsearch.core.EntityMapper;import com.github.ydespreaux.spring.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;import com.github.ydespreaux.spring.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;import lombok.extern.slf4j.Slf4j;import org.springframework.context.ApplicationContext;import org.springframework.context.ApplicationContextAware;import org.springframework.data.mapping.context.MappingContext;import org.springframework.util.Assert;import java.util.Map;import java.util.concurrent.ConcurrentHashMap;/** * @author Yoann Despréaux * @since 1.0.0 */@Slf4jpublic class MappingElasticsearchConverter implements ElasticsearchConverter, ApplicationContextAware {    private final MappingContext<? extends ElasticsearchPersistentEntity, ElasticsearchPersistentProperty> mappingContext;    private final Map<Class<?>, ElasticsearchPersistentEntity<?>> context = new ConcurrentHashMap<>();    private final EntityMapper mapper;    public MappingElasticsearchConverter(MappingContext<? extends ElasticsearchPersistentEntity, ElasticsearchPersistentProperty> mappingContext, final EntityMapper mapper) {        Assert.notNull(mappingContext, "MappingContext must not be null!");        Assert.notNull(mapper, "Mapper must not be null!");        this.mappingContext = mappingContext;        this.mapper = mapper;    }    @Override    public <T> ElasticsearchPersistentEntity<T> getRequiredPersistentEntity(Class<T> clazz) {        if (context.containsKey(clazz)) {            return (ElasticsearchPersistentEntity<T>) context.get(clazz);        }        ElasticsearchPersistentEntity<T> persistentEntity = (ElasticsearchPersistentEntity<T>) this.mappingContext.getRequiredPersistentEntity(clazz);        this.mapper.register(persistentEntity);        this.context.put(clazz, persistentEntity);        return persistentEntity;    }    /**     * Returns the underlying {@link MappingContext} used by the converter.     *     * @return never {@literal null}     */    @Override    public MappingContext<? extends ElasticsearchPersistentEntity, ElasticsearchPersistentProperty> getMappingContext() {        return this.mappingContext;    }    @Override    public void setApplicationContext(ApplicationContext applicationContext) {        if (mappingContext instanceof ApplicationContextAware) {            ((ApplicationContextAware) mappingContext).setApplicationContext(applicationContext);        }    }}