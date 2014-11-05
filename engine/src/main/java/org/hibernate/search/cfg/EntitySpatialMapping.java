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
import org.hibernate.search.annotations.SpatialMode;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.bridge.FieldBridge;

/**
 * @author Nicolas Helleringer
 */
public class EntitySpatialMapping {

	private final SearchMapping mapping;
	private final EntityDescriptor entity;
	private final Map<String, Object> spatial = new HashMap<String, Object>();

	public EntitySpatialMapping(SearchMapping mapping, EntityDescriptor entity) {
		this.mapping = mapping;
		this.entity = entity;
		this.entity.addSpatial( spatial );
	}

	public EntitySpatialMapping spatial() {
		return new EntitySpatialMapping( mapping, entity );
	}

	public EntitySpatialMapping name(String fieldName) {
		spatial.put( "name", fieldName );
		return this;
	}

	public EntitySpatialMapping store(Store store) {
		spatial.put( "store", store );
		return this;
	}

	public EntitySpatialMapping boost(float boost) {
		final Map<String, Object> boostAnn = new HashMap<String, Object>();
		boostAnn.put( "value", boost );
		spatial.put( "boost", boostAnn );
		return this;
	}

	public EntitySpatialMapping spatialMode(SpatialMode spatialMode) {
		spatial.put( "spatialMode", spatialMode );
		return this;
	}

	public EntitySpatialMapping topSpatialHashLevel(int topSpatialHashLevel) {
		spatial.put( "topSpatialHashLevel", topSpatialHashLevel );
		return this;
	}

	public EntitySpatialMapping bottomSpatialHashLevel(int bottomSpatialHashLevel) {
		spatial.put( "bottomSpatialHashLevel", bottomSpatialHashLevel );
		return this;
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
}
