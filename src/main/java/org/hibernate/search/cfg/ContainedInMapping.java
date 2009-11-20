package org.hibernate.search.cfg;

import java.lang.annotation.ElementType;
import java.util.HashMap;
import java.util.Map;

import org.apache.solr.analysis.TokenizerFactory;

public class ContainedInMapping {

	private final SearchMapping mapping;
	private final PropertyDescriptor property;
	private final EntityDescriptor entity;

	public ContainedInMapping(SearchMapping mapping,PropertyDescriptor property, EntityDescriptor entity) {
		this.mapping = mapping;
		this.property = property;
		this.entity = entity;
		Map<String, Object> containedIn = new HashMap<String, Object>();
		property.setContainedIn(containedIn);
	}
	
	public FieldMapping field() {
		return new FieldMapping(property, entity, mapping);
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

}
