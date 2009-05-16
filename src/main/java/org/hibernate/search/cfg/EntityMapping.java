package org.hibernate.search.cfg;

import java.lang.annotation.ElementType;
import java.util.HashMap;
import java.util.Map;

import org.apache.solr.analysis.TokenizerFactory;

/**
 * @author Emmanuel Bernard
 */
public class EntityMapping {
	private SearchMapping mapping;
	private EntityDescriptor entity;

	public EntityMapping(Class<?> entityType, String name, SearchMapping mapping) {
		this.mapping = mapping;
		entity = mapping.getEntity(entityType);
		Map<String, Object> indexed = new HashMap<String, Object>();
		if (name != null) indexed.put( "index", name );
		entity.setIndexed(indexed);
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

	public PropertyMapping property(String name, ElementType type) {
		return new PropertyMapping(name, type, entity, mapping);
	}

	public AnalyzerDefMapping analyzerDef(String name, Class<? extends TokenizerFactory> tokenizerFactory) {
		return new AnalyzerDefMapping(name, tokenizerFactory, mapping);
	}

	public EntityMapping indexedClass(Class<?> entityType) {
		return new EntityMapping(entityType, null, mapping);
	}

	public EntityMapping indexedClass(Class<?> entityType, String indexName) {
		return new EntityMapping(entityType, indexName,  mapping);
	}
}
