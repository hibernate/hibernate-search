package org.hibernate.search.cfg;

import java.lang.annotation.ElementType;
import java.util.HashMap;
import java.util.Map;

import org.apache.solr.analysis.TokenizerFactory;
import org.hibernate.search.analyzer.Discriminator;

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
	
	public EntityMapping indexName(String indexName) {
		this.indexed.put("index", indexName);
		return entityMapping;
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

	public IndexedMapping analyzerDiscriminator(Class<? extends Discriminator> discriminator) {
		final Map<String, Object> discriminatorAnn = new HashMap<String, Object>();
		discriminatorAnn.put( "impl", discriminator );
		entity.setAnalyzerDiscriminator(discriminatorAnn);
		return this;
	}
	
	
	public FullTextFilterDefMapping fullTextFilterDef(String name, Class<?> impl) {
		return new FullTextFilterDefMapping(mapping, entity, name, impl);
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
