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
import org.hibernate.search.engine.BoostStrategy;

/**
 * @author Emmanuel Bernard
 */
public class EntityMapping {
	private SearchMapping mapping;
	private EntityDescriptor entity;
	
	public EntityMapping(Class<?> entityType, SearchMapping mapping) {
		this.mapping = mapping;
		entity = mapping.getEntity(entityType);
	}
	
	public IndexedMapping indexed() {
		return new IndexedMapping(mapping,entity, this);
	}
	
	public EntityMapping similarity(Class<?> impl) {
		Map<String, Object> similarity = new HashMap<String, Object>(1);
		similarity.put( "impl", impl );
		entity.setSimilariy(similarity);
		return this;
	}

	public EntityMapping boost(float boost) {
		final Map<String, Object> boostAnn = new HashMap<String, Object>();
		boostAnn.put( "value", boost );
		entity.setBoost(boostAnn);
		return this;
	}

	public EntityMapping dynamicBoost(Class<? extends BoostStrategy>  impl) {
		final Map<String, Object> dynamicBoost = new HashMap<String, Object>();
		dynamicBoost.put("impl", impl);
		entity.setDynamicBoost(dynamicBoost);
		return this;
	}
	
	public EntityMapping analyzerDiscriminator(Class<? extends Discriminator> discriminator) {
		final Map<String, Object> discriminatorAnn = new HashMap<String, Object>();
		discriminatorAnn.put( "impl", discriminator );
		entity.setAnalyzerDiscriminator(discriminatorAnn);
		return this;
	}
	
	
	public FullTextFilterDefMapping fullTextFilterDef(String name, Class<?> impl) {
		return new FullTextFilterDefMapping(mapping,name, impl);
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

	public ClassBridgeMapping classBridge(Class<?> impl) {
		return new ClassBridgeMapping(mapping, entity, impl, this);
	}
	
}
