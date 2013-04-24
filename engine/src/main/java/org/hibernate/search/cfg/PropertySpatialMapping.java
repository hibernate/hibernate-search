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

	public PropertySpatialMapping topGridLevel(int topGridLevel) {
		spatial.put( "topQuadTreeLevel", topGridLevel );
		return this;
	}

	public PropertySpatialMapping bottomGridLevel(int bottomGridLevel) {
		spatial.put( "bottomQuadTreeLevel", bottomGridLevel );
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
