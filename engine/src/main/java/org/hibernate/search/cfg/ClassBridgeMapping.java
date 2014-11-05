/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

public class ClassBridgeMapping {

	private final SearchMapping mapping;
	private final EntityDescriptor entity;
	private final Map<String, Object> classBridge;

	public ClassBridgeMapping(SearchMapping mapping, EntityDescriptor entity, Class<?> impl) {
		this( mapping, entity );

		entity.addClassBridgeDef( classBridge );

		if ( impl != null ) {
			this.classBridge.put( "impl", impl );
		}
	}

	public ClassBridgeMapping(SearchMapping mapping, EntityDescriptor entity, FieldBridge instance) {
		this( mapping, entity );

		entity.addClassBridgeInstanceDef( instance, classBridge );

		// the given bridge instance is actually used, a class object is still required to instantiate the annotation
		this.classBridge.put( "impl", void.class );
	}

	private ClassBridgeMapping(SearchMapping mapping, EntityDescriptor entity) {
		this.mapping = mapping;
		this.entity = entity;
		this.classBridge = new HashMap<String, Object>();
	}

	public ClassBridgeMapping name(String name) {
		this.classBridge.put( "name", name );
		return this;
	}

	public ClassBridgeMapping store(Store store) {
		this.classBridge.put( "store", store );
		return this;
	}

	public ClassBridgeMapping index(Index index) {
		this.classBridge.put( "index", index );
		return this;
	}

	public ClassBridgeMapping analyze(Analyze analyze) {
		this.classBridge.put( "analyze", analyze );
		return this;
	}

	public ClassBridgeMapping norms(Norms norms) {
		this.classBridge.put( "norms", norms );
		return this;
	}

	public ClassBridgeMapping termVector(TermVector termVector) {
		this.classBridge.put( "termVector", termVector );
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
		Map<String, Object> param = SearchMapping.addElementToAnnotationArray( classBridge, "params" );
		param.put( "name", name );
		param.put( "value", value );
		return this;
	}

	public ClassBridgeMapping classBridge(Class<?> impl) {
		return new ClassBridgeMapping( mapping, entity, impl );
	}

	/**
	 * Registers the given class bridge for the currently configured entity type. Any subsequent analyzer, parameter
	 * etc. configurations apply to this class bridge.
	 *
	 * @param instance a class bridge instance
	 * @return a new {@link ClassBridgeMapping} following the method chaining pattern
	 * @hsearch.experimental This method is considered experimental and it may be altered or removed in future releases
	 * @throws org.hibernate.search.exception.SearchException in case the same bridge instance is passed more than once for the
	 * currently configured entity type
	 */
	public ClassBridgeMapping classBridgeInstance(FieldBridge instance) {
		return new ClassBridgeMapping( mapping, entity, instance );
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

	public IndexedMapping indexed() {
		return new IndexedMapping( mapping, entity );
	}

}
