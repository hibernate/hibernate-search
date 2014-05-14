/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.cfg;

import java.util.Map;
import java.util.HashMap;
import java.lang.annotation.ElementType;

import org.apache.lucene.analysis.util.TokenizerFactory;

import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.TermVector;

/**
 * @author Emmanuel Bernard
 */
public class FieldBridgeMapping {
	private final SearchMapping mapping;
	private final EntityDescriptor entity;
	private final PropertyDescriptor property;
	private final FieldMapping fieldMapping;
	private final Map<String, Object> bridge = new HashMap<String, Object>();

	public FieldBridgeMapping(Class<?> impl, Map<String, Object> field,
							FieldMapping fieldMapping,
							PropertyDescriptor property,
							EntityDescriptor entity,
							SearchMapping mapping) {
		this.mapping = mapping;
		this.entity = entity;
		this.property = property;
		this.fieldMapping = fieldMapping;
		bridge.put( "impl", impl );
		field.put( "bridge", bridge );
	}

	public FieldBridgeMapping param(String name, String value) {
		Map<String, Object> param = SearchMapping.addElementToAnnotationArray( bridge, "params" );
		param.put( "name", name );
		param.put( "value", value );
		return this;
	}

	//FieldMapping level
	public FieldMapping name(String fieldName) {
		return fieldMapping.name( fieldName );
	}

	public FieldMapping store(Store store) {
		return fieldMapping.store( store );
	}

	public FieldMapping index(Index index) {
		return fieldMapping.index( index );
	}

	public FieldMapping termVector(TermVector termVector) {
		return fieldMapping.termVector( termVector );
	}

	public FieldMapping boost(float boost) {
		return fieldMapping.boost( boost );
	}

	public FieldMapping analyzer(Class<?> analyzerClass) {
		return fieldMapping.analyzer( analyzerClass );
	}

	public FieldMapping analyzer(String analyzerDef) {
		return fieldMapping.analyzer( analyzerDef );
	}

	//PropertyMapping level
	public FieldMapping field() {
		return new FieldMapping(property, entity, mapping);
	}

	//EntityMapping level
	public PropertyMapping property(String name, ElementType type) {
		return new PropertyMapping(name, type, entity, mapping);
	}

	//Global level
	public AnalyzerDefMapping analyzerDef(String name, Class<? extends TokenizerFactory> tokenizerFactory) {
		return new AnalyzerDefMapping(name, tokenizerFactory, mapping);
	}

	public EntityMapping entity(Class<?> entityType) {
		return new EntityMapping(entityType, mapping);
	}

}
