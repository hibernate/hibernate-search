/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.cfg;

import java.lang.annotation.ElementType;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.util.TokenizerFactory;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Norms;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TermVector;
import org.hibernate.search.bridge.FieldBridge;

public class IndexedClassBridgeMapping {

	private final SearchMapping mapping;
	private final EntityDescriptor entity;
	private final Map<String, Object> classBridge;
	private final IndexedMapping indexedMapping;

	public IndexedClassBridgeMapping(SearchMapping mapping, EntityDescriptor entity, Class<?> impl, IndexedMapping indexedMapping) {
		this( mapping, entity, indexedMapping );

		entity.addClassBridgeDef( classBridge );

		if ( impl != null ) {
			this.classBridge.put( "impl", impl );
		}
	}

	public IndexedClassBridgeMapping(SearchMapping mapping, EntityDescriptor entity, FieldBridge instance, IndexedMapping indexedMapping) {
		this( mapping, entity, indexedMapping );

		entity.addClassBridgeInstanceDef( instance, classBridge );

		// the given bridge instance is actually used, a class object is still required to instantiate the annotation
		this.classBridge.put( "impl", void.class );
	}

	private IndexedClassBridgeMapping(SearchMapping mapping, EntityDescriptor entity, IndexedMapping indexedMapping) {
		this.mapping = mapping;
		this.entity = entity;
		this.indexedMapping = indexedMapping;
		this.classBridge = new HashMap<String, Object>();
	}

	public IndexedClassBridgeMapping name(String name) {
		this.classBridge.put( "name", name );
		return this;
	}

	public IndexedClassBridgeMapping store(Store store) {
		this.classBridge.put( "store", store );
		return this;
	}

	public IndexedClassBridgeMapping index(Index index) {
		this.classBridge.put( "index", index );
		return this;
	}

	public IndexedClassBridgeMapping analyze(Analyze analyze) {
		this.classBridge.put( "analyze", analyze );
		return this;
	}

	public IndexedClassBridgeMapping norms(Norms norms) {
		this.classBridge.put( "norms", norms );
		return this;
	}

	public IndexedClassBridgeMapping termVector(TermVector termVector) {
		this.classBridge.put( "termVector", termVector );
		return this;
	}

	public IndexedClassBridgeMapping boost(float boost) {
		final Map<String, Object> boostAnn = new HashMap<String, Object>();
		boostAnn.put( "value", boost );
		classBridge.put( "boost", boostAnn );
		return this;
	}

	public IndexedClassBridgeMapping analyzer(Class<?> analyzerClass) {
		final Map<String, Object> analyzer = new HashMap<String, Object>();
		analyzer.put( "impl", analyzerClass );
		classBridge.put( "analyzer", analyzer );
		return this;
	}

	public IndexedClassBridgeMapping analyzer(String analyzerDef) {
		final Map<String, Object> analyzer = new HashMap<String, Object>();
		analyzer.put( "definition", analyzerDef );
		classBridge.put( "analyzer", analyzer );
		return this;
	}

	public IndexedClassBridgeMapping param(String name, String value) {
		Map<String, Object> param = SearchMapping.addElementToAnnotationArray( classBridge, "params" );
		param.put( "name", name );
		param.put( "value", value );
		return this;
	}

	public IndexedClassBridgeMapping classBridge(Class<?> impl) {
		return new IndexedClassBridgeMapping( mapping, entity, impl, indexedMapping );
	}

	/**
	 * Registers the given class bridge for the currently configured entity type. Any subsequent analyzer, parameter
	 * etc. configurations apply to this class bridge.
	 *
	 * @param instance a class bridge instance
	 * @return a new {@link ClassBridgeMapping} following the method chaining pattern
	 * @experimental This method is considered experimental and it may be altered or removed in future releases
	 * @throws org.hibernate.search.exception.SearchException in case the same bridge instance is passed more than once for the
	 * currently configured entity type
	 */
	public IndexedClassBridgeMapping classBridgeInstance(FieldBridge instance) {
		return new IndexedClassBridgeMapping( mapping, entity, instance, indexedMapping );
	}

	public FullTextFilterDefMapping fullTextFilterDef(String name, Class<?> impl) {
		return new FullTextFilterDefMapping( mapping, name, impl );
	}

	public PropertyMapping property(String name, ElementType type) {
		return new PropertyMapping( name, type, entity, mapping );
	}

	public AnalyzerDefMapping analyzerDef(String name, Class<? extends TokenizerFactory> tokenizerFactory) {
		return new AnalyzerDefMapping( name, tokenizerFactory, mapping );
	}

	public EntityMapping entity(Class<?> entityType) {
		return new EntityMapping( entityType, mapping );
	}

	public ProvidedIdMapping providedId() {
		return new ProvidedIdMapping( mapping, entity );
	}

}
