package org.hibernate.search.cfg;

import java.lang.annotation.ElementType;
import java.util.HashMap;
import java.util.Map;

import org.apache.solr.analysis.TokenizerFactory;

public class ProvidedIdMapping {

	private final SearchMapping searchMapping;
	private final Map<String,Object> providedIdMapping;
	private EntityDescriptor entity;
	
	public ProvidedIdMapping(SearchMapping searchMapping, EntityDescriptor entity) {
		this.searchMapping = searchMapping;
		this.entity =entity;
		providedIdMapping = new HashMap<String,Object>();
		entity.setProvidedId(providedIdMapping);
	}
	
	public ProvidedIdMapping name(String name) {
		this.providedIdMapping.put("name", name);
		return this;
	}

	public FieldBridgeMapping bridge(Class<?> impl) {
		return new FieldBridgeMapping( impl, providedIdMapping, null, null, entity, searchMapping );
	}
	
	public FullTextFilterDefMapping fullTextFilterDef(String name, Class<?> impl) {
		return new FullTextFilterDefMapping(searchMapping, entity, name, impl);
	}
	
	public PropertyMapping property(String name, ElementType type) {
		return new PropertyMapping(name, type, entity, searchMapping);
	}

	public AnalyzerDefMapping analyzerDef(String name, Class<? extends TokenizerFactory> tokenizerFactory) {
		return new AnalyzerDefMapping(name, tokenizerFactory, searchMapping);
	}

	public EntityMapping entity(Class<?> entityType) {
		return new EntityMapping(entityType, searchMapping);
	}
	
}
