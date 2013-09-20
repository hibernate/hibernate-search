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

	public EntitySpatialMapping topGridLevel(int topGridLevel) {
		spatial.put( "topQuadTreeLevel", topGridLevel );
		return this;
	}

	public EntitySpatialMapping bottomGridLevel(int bottomGridLevel) {
		spatial.put( "bottomQuadTreeLevel", bottomGridLevel );
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
	 * @experimental This method is considered experimental and it may be altered or removed in future releases
	 * @throws org.hibernate.search.SearchException in case the same bridge instance is passed more than once for the
	 * currently configured entity type
	 */
	public ClassBridgeMapping classBridgeInstance(FieldBridge instance) {
		return new ClassBridgeMapping( mapping, entity, instance );
	}
}
