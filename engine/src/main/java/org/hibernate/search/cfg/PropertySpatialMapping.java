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
public class PropertySpatialMapping {
	private final SearchMapping mapping;
	private final EntityDescriptor entity;
	private final PropertyDescriptor property;
	private final Map<String, Object> spatial = new HashMap<String, Object>();

	public PropertySpatialMapping(PropertyDescriptor property, EntityDescriptor entity, SearchMapping mapping) {
		this.mapping = mapping;
		this.entity = entity;
		this.property = property;
		this.property.setSpatial( spatial );
	}

	public PropertySpatialMapping spatial() {
		return new PropertySpatialMapping(property, entity, mapping);
	}

	public PropertySpatialMapping name(String fieldName) {
		spatial.put( "name", fieldName );
		return this;
	}

	public PropertySpatialMapping store(Store store) {
		spatial.put( "store", store );
		return this;
	}

	public PropertySpatialMapping boost(float boost) {
		final Map<String, Object> boostAnn = new HashMap<String, Object>();
		boostAnn.put( "value", boost );
		spatial.put( "boost", boostAnn );
		return this;
	}

	public PropertySpatialMapping spatialMode(SpatialMode spatialMode) {
		spatial.put( "spatialMode", spatialMode );
		return this;
	}

	public PropertySpatialMapping topSpatialHashLevel(int topSpatialHashLevel) {
		spatial.put( "topSpatialHashLevel", topSpatialHashLevel );
		return this;
	}

	public PropertySpatialMapping bottomSpatialHashLevel(int bottomSpatialHashLevel) {
		spatial.put( "bottomSpatialHashLevel", bottomSpatialHashLevel );
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

	public EntityMapping entity(Class<?> entityType) {
		return new EntityMapping(entityType, mapping);
	}

	public PropertyMapping bridge(Class<? extends FieldBridge> fieldBridge) {
		return new FieldBridgeDirectMapping( property, entity, mapping, fieldBridge );
	}

}
