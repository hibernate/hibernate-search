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
import java.util.Map;
import java.util.HashMap;

import org.apache.solr.analysis.TokenizerFactory;

import org.hibernate.search.analyzer.Discriminator;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.engine.BoostStrategy;

/**
 * @author Emmanuel Bernard
 */
public class PropertyMapping {

	protected final SearchMapping mapping;
	protected final EntityDescriptor entity;
	protected final PropertyDescriptor property;

	public PropertyMapping(String name, ElementType type, EntityDescriptor entity, SearchMapping mapping) {
		this.mapping = mapping;
		this.entity = entity;
		this.property = entity.getProperty( name, type );
	}

	protected PropertyMapping(PropertyDescriptor property, EntityDescriptor entity, SearchMapping mapping) {
		this.mapping = mapping;
		this.entity = entity;
		this.property = property;
	}

	public DocumentIdMapping documentId() {
		return new DocumentIdMapping( property, entity, mapping );
	}

	public FieldMapping field() {
		return new FieldMapping( property, entity, mapping );
	}

	public PropertySpatialMapping spatial() {
		return new PropertySpatialMapping( property, entity, mapping );
	}

	public PropertyLatitudeMapping latitude() {
		return new PropertyLatitudeMapping( property, entity, mapping );
	}

	public PropertyLongitudeMapping longitude() {
		return new PropertyLongitudeMapping( property, entity, mapping );
	}

	public DateBridgeMapping dateBridge(Resolution resolution) {
		return new DateBridgeMapping( mapping, entity, property, resolution );
	}

	public CalendarBridgeMapping calendarBridge(Resolution resolution) {
		return new CalendarBridgeMapping( mapping, entity, property, resolution );
	}

	public PropertyMapping analyzerDiscriminator(Class<? extends Discriminator> discriminator) {
		Map<String, Object> analyzerDiscriminatorAnn = new HashMap<String, Object>();
		analyzerDiscriminatorAnn.put( "impl", discriminator );
		property.setAnalyzerDiscriminator( analyzerDiscriminatorAnn );
		return this;
	}

	public PropertyMapping dynamicBoost(Class<? extends BoostStrategy> impl) {
		final Map<String, Object> dynamicBoostAnn = new HashMap<String, Object>();
		dynamicBoostAnn.put( "impl", impl );
		property.setDynamicBoost( dynamicBoostAnn );
		return this;
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

	public IndexEmbeddedMapping indexEmbedded() {
		return new IndexEmbeddedMapping( mapping, property, entity );
	}

	public ContainedInMapping containedIn() {
		return new ContainedInMapping( mapping, property, entity );
	}

	public PropertyMapping bridge(Class<? extends FieldBridge> fieldBridge) {
		return new FieldBridgeDirectMapping( property, entity, mapping, fieldBridge );
	}
}
