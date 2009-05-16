package org.hibernate.search.cfg;

import java.lang.annotation.ElementType;
import java.util.HashMap;
import java.util.Map;

import org.apache.solr.analysis.TokenizerFactory;

/**
 * @author Emmanuel Bernard
 */
public class DocumentIdMapping {
	private final SearchMapping mapping;
	private final EntityDescriptor entity;
	private final PropertyDescriptor property;
	private final Map<String, Object> documentId = new HashMap<String, Object>();

	public DocumentIdMapping(PropertyDescriptor property, EntityDescriptor entity, SearchMapping mapping) {
		this.mapping = mapping;
		this.entity = entity;
		this.property = property;
		property.setDocumentId( documentId );
	}

	public DocumentIdMapping name(String fieldName) {
		documentId.put( "name", fieldName );
		return this;
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