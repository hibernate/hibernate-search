package org.hibernate.search.cfg;

import java.lang.annotation.ElementType;

import org.apache.solr.analysis.TokenizerFactory;

/**
 * @author Emmanuel Bernard
 */
public class PropertyMapping {
	private SearchMapping mapping;
	private EntityDescriptor entity;
	private PropertyDescriptor property;

	public PropertyMapping(String name, ElementType type, EntityDescriptor entity, SearchMapping mapping) {
		this.mapping = mapping;
		this.entity = entity;
		property = entity.getProperty(name, type);
	}

	public DocumentIdMapping documentId() {
		return new DocumentIdMapping( property, entity, mapping );
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

	public EntityMapping indexedClass(Class<?> entityType) {
		return new EntityMapping(entityType, null, mapping);
	}

	public EntityMapping indexedClass(Class<?> entityType, String indexName) {
		return new EntityMapping(entityType, indexName,  mapping);
	}
}
