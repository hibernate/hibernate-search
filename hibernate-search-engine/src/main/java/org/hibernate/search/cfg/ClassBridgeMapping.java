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

import java.lang.annotation.ElementType;
import java.util.HashMap;
import java.util.Map;

import org.apache.solr.analysis.TokenizerFactory;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Norms;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TermVector;

public class ClassBridgeMapping {
	
	private final SearchMapping mapping;
	private final EntityDescriptor entity;
	private final Map<String, Object> classBridge;
	private final EntityMapping entityMapping;
	
	
	public ClassBridgeMapping(SearchMapping mapping, EntityDescriptor entity, Class<?> impl, EntityMapping entityMapping) {
		this.mapping = mapping;
		this.entity = entity;
		this.entityMapping = entityMapping;
		this.classBridge = new HashMap<String,Object>();
		entity.addClassBridgeDef(classBridge);
		if (impl != null) {
			this.classBridge.put("impl", impl);
		}
		
	}
	
	public ClassBridgeMapping name(String name) {
		this.classBridge.put("name", name);
		return this;
	}
	
	public ClassBridgeMapping store(Store store) {
		this.classBridge.put("store", store);
		return this;
	}
	
	public ClassBridgeMapping index(Index index) {
		this.classBridge.put("index", index);
		return this;
	}
	
	public ClassBridgeMapping analyze(Analyze analyze) {
		this.classBridge.put("analyze", analyze);
		return this;
	}

	public ClassBridgeMapping norms(Norms norms) {
		this.classBridge.put("norms", norms);
		return this;
	}

	public ClassBridgeMapping termVector(TermVector termVector) {
		this.classBridge.put("termVector", termVector);
		return this;
	}
	
	public ClassBridgeMapping boost(float boost) {
		final Map<String, Object> boostAnn = new HashMap<String, Object>();
		boostAnn.put( "value", boost );
		classBridge.put( "boost", boostAnn );
		return this;
	}
	
	public ClassBridgeMapping analyzer(Class<?> analyzerClass) {
		final Map<String, Object> analyzer = new HashMap<String, Object>();
		analyzer.put( "impl", analyzerClass );
		classBridge.put( "analyzer", analyzer );
		return this;
	}

	public ClassBridgeMapping analyzer(String analyzerDef) {
		final Map<String, Object> analyzer = new HashMap<String, Object>();
		analyzer.put( "definition", analyzerDef );
		classBridge.put( "analyzer", analyzer );
		return this;
	}
	
	
	public ClassBridgeMapping param(String name, String value) {
		Map<String, Object> param = SearchMapping.addElementToAnnotationArray(classBridge, "params");
		param.put("name", name);
		param.put("value", value);
		return this;
	}
	
	
	public ClassBridgeMapping classBridge(Class<?> impl) {
		return new ClassBridgeMapping(mapping, entity,impl,entityMapping );
	}
	
	public FullTextFilterDefMapping fullTextFilterDef(String name, Class<?> impl) {
		return new FullTextFilterDefMapping(mapping,name, impl);
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

	public IndexedMapping indexed() {
		return new IndexedMapping(mapping, entity, entityMapping);
	}
	
}
