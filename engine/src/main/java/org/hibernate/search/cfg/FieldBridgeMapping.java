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

import java.util.Map;
import java.util.HashMap;
import java.lang.annotation.ElementType;

import org.apache.solr.analysis.TokenizerFactory;

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
