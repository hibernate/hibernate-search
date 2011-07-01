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

import org.apache.solr.analysis.TokenizerFactory;

import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TermVector;

public class IndexedClassBridgeMapping {

	private final SearchMapping mapping;
	private final EntityDescriptor entity;
	private final Map<String, Object> classBridge;
	private final IndexedMapping indexedMapping;

	public IndexedClassBridgeMapping(SearchMapping mapping, EntityDescriptor entity, Class<?> impl, IndexedMapping indexedMapping) {
		this.mapping = mapping;
		this.entity = entity;
		this.indexedMapping = indexedMapping;
		this.classBridge = new HashMap<String, Object>();
		entity.addClassBridgeDef( classBridge );
		if ( impl != null ) {
			this.classBridge.put( "impl", impl );
		}
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
