package org.hibernate.search.cfg;

import java.lang.annotation.ElementType;
import java.util.HashMap;
import java.util.Map;

import org.apache.solr.analysis.TokenizerFactory;

import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Store;

/**
 * @author Emmanuel Bernard
 */
public class FieldMapping {
	private SearchMapping mapping;
	private EntityDescriptor entity;
	private PropertyDescriptor property;
	private Map<String, Object> field = new HashMap<String, Object>();

	public FieldMapping(PropertyDescriptor property, EntityDescriptor entity, SearchMapping mapping) {
		this.mapping = mapping;
		this.entity = entity;
		this.mapping = mapping;
		this.property = property;
		property.addField(field);
	}

	public FieldMapping name(String fieldName) {
		field.put( "name", fieldName );
		return this;
	}

	public FieldMapping store(Store store) {
		field.put( "store", store );
		return this;
	}

	public FieldMapping index(Index index) {
		field.put( "index", index );
		return this;
	}

	public FieldMapping analyzer(Class<?> analyzerClass) {
		final Map<String, Object> analyzer = new HashMap<String, Object>();
		analyzer.put( "impl", analyzerClass );
		field.put( "analyzer", analyzer );
		return this;
	}

	public FieldMapping analyzer(String analyzerDef) {
		final Map<String, Object> analyzer = new HashMap<String, Object>();
		analyzer.put( "definition", analyzerDef );
		field.put( "analyzer", analyzer );
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
