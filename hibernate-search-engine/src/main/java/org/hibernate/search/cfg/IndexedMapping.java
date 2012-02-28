/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.cfg;

import java.lang.annotation.ElementType;
import java.util.HashMap;
import java.util.Map;

import org.apache.solr.analysis.TokenizerFactory;
import org.hibernate.search.analyzer.Discriminator;
import org.hibernate.search.annotations.FieldCacheType;
import org.hibernate.search.engine.BoostStrategy;
import org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor;

public class IndexedMapping {
	
	private final SearchMapping mapping;
	private final EntityDescriptor entity;
	private final Map<String, Object> indexed;
	private final EntityMapping entityMapping;
	
	public IndexedMapping(SearchMapping mapping, EntityDescriptor entity, EntityMapping entityMapping) {
		this.entityMapping = entityMapping;
		this.mapping = mapping;
		this.entity = entity;
		indexed = new HashMap<String, Object>();
		entity.setIndexed(indexed);
	}
	
	public IndexedMapping indexName(String indexName) {
		this.indexed.put( "index", indexName );
		return this;
	}

	public IndexedMapping interceptor(Class<? extends EntityIndexingInterceptor> interceptor) {
		this.indexed.put("interceptor", interceptor);
		return this;
	}
	
	public IndexedMapping cacheFromIndex(FieldCacheType... type) {
		Map<String, Object> cacheInMemory = new HashMap<String, Object>(1);
		cacheInMemory.put( "value", type );
		entity.setCacheInMemory(cacheInMemory);
		return this;
	}
	
	public IndexedMapping similarity(Class<?> impl) {
		Map<String, Object> similarity = new HashMap<String, Object>(1);
		similarity.put( "impl", impl );
		entity.setSimilariy(similarity);
		return this;
	}

	public IndexedMapping boost(float boost) {
		final Map<String, Object> boostAnn = new HashMap<String, Object>();
		boostAnn.put( "value", boost );
		entity.setBoost(boostAnn);
		return this;
	}

	public IndexedMapping dynamicBoost(Class<? extends BoostStrategy>  impl) {
		final Map<String, Object> dynamicBoost = new HashMap<String, Object>();
		dynamicBoost.put("impl", impl);
		entity.setDynamicBoost(dynamicBoost);
		return this;
	}

	public IndexedMapping analyzerDiscriminator(Class<? extends Discriminator> discriminator) {
		final Map<String, Object> discriminatorAnn = new HashMap<String, Object>();
		discriminatorAnn.put( "impl", discriminator );
		entity.setAnalyzerDiscriminator(discriminatorAnn);
		return this;
	}

	public IndexedClassBridgeMapping classBridge(Class<?> impl) {
		return new IndexedClassBridgeMapping(mapping, entity, impl, this);
	}
	
	public FullTextFilterDefMapping fullTextFilterDef(String name, Class<?> impl) {
		return new FullTextFilterDefMapping(mapping, name, impl);
	}
	
	public PropertyMapping property(String name, ElementType type) {
		return new PropertyMapping(name, type, entity, mapping);
	}

	public AnalyzerDefMapping analyzerDef(String name, Class<? extends TokenizerFactory> tokenizerFactory) {
		return new AnalyzerDefMapping(name, tokenizerFactory, mapping);
	}

	public EntityMapping entity(Class<?> entityType) {
		return new EntityMapping(entityType, mapping);
	}

	public ProvidedIdMapping providedId() {
		return new ProvidedIdMapping(mapping,entity);
	}
	
}
