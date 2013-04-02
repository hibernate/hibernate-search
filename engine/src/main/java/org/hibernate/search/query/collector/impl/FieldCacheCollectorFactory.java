/*
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

package org.hibernate.search.query.collector.impl;

import org.apache.lucene.search.Collector;

import org.hibernate.search.ProjectionConstants;
import org.hibernate.search.bridge.TwoWayStringBridge;
import org.hibernate.search.bridge.builtin.StringBridge;
import org.hibernate.search.query.fieldcache.impl.FieldCacheLoadingType;
import org.hibernate.search.query.fieldcache.impl.FieldLoadingStrategy;

/**
 * Every search needs a fresh instance of a Collector, still for
 * each field the same name and type are going to be used.
 * So reuse a {@code FieldCollectorFactory} for each field, to create
 * {@code Collector} instances as needed.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class FieldCacheCollectorFactory {

	public static final FieldCacheCollectorFactory CLASS_TYPE_FIELD_CACHE_COLLECTOR_FACTORY
			= new FieldCacheCollectorFactory(
			ProjectionConstants.OBJECT_CLASS,
			FieldCacheLoadingType.STRING, new StringBridge()
	);

	/**
	 * when the Query is going to collect more than this amount of
	 * documents, it seems an array based collector is better suited.
	 * Below that, we save some memory by using a Map keyed on the array index.
	 * TODO: make this configurable?
	 */
	private static final int DEFAULT_IMPLEMENTATION_SWITCH_THRESHOLD = 100;

	private final String fieldName;
	private final FieldCacheLoadingType type;
	private final int implementationSwitchThreshold;
	private final TwoWayStringBridge twoWayStringBridge;

	public FieldCacheCollectorFactory(String fieldName, FieldCacheLoadingType type, TwoWayStringBridge twoWayStringBridge) {
		this( fieldName, type, twoWayStringBridge, DEFAULT_IMPLEMENTATION_SWITCH_THRESHOLD );
	}

	public FieldCacheCollectorFactory(String fieldName, FieldCacheLoadingType type, TwoWayStringBridge twoWayStringBridge, int implementationSwitchThreshold) {
		if ( fieldName == null ) {
			throw new IllegalArgumentException( "fieldName is mandatory" );
		}
		if ( type == null ) {
			throw new IllegalArgumentException( "type is mandatory" );
		}
		this.fieldName = fieldName;
		this.type = type;
		this.twoWayStringBridge = twoWayStringBridge;
		this.implementationSwitchThreshold = implementationSwitchThreshold;
	}

	public FieldCacheCollector createFieldCollector(Collector collector, int totalMaxDocs, int expectedMatchesCount) {
		FieldCacheCollector fieldCollector = createDefaultFieldCollector(
				collector, totalMaxDocs, expectedMatchesCount,
				type.createLoadingStrategy( fieldName )
		);
		if ( twoWayStringBridge != null ) {
			return new TwoWayTransformingFieldCacheCollector( fieldCollector, twoWayStringBridge );
		}
		else {
			return fieldCollector;
		}
	}

	/**
	 * There are two possible implementations of {@code FieldCacheCollector},
	 * one is more efficient for large and one for small results.
	 * Here we try to guesstimate the most appropriate implementation,
	 * which doesn't depend on the type but on the estimated result size
	 * (so it's a per-query decision)
	 *
	 * @param collector the collector to delegate to
	 * @param totalMaxDocs the maximum document count
	 * @param expectedMatchesCount the expected matching document count
	 * @param loadingStrategy the default loading strategy
	 *
	 * @return the most suitable implementation of {@code FieldCacheCollector} depending on the expected count of
	 *         collected documents
	 */
	private FieldCacheCollector createDefaultFieldCollector(
			Collector collector, int totalMaxDocs, int expectedMatchesCount,
			FieldLoadingStrategy loadingStrategy) {

		if ( expectedMatchesCount > implementationSwitchThreshold ) {
			return new BigArrayFieldCacheCollectorImpl(
					collector, loadingStrategy, new String[totalMaxDocs]
			);
		}
		else {
			return new MapFieldCacheCollectorImpl( collector, loadingStrategy );
		}
	}

	// HashCode and Equals are used to detect same kind of FieldCollectorFactory applied on different indexed classes *
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ( ( fieldName == null ) ? 0 : fieldName.hashCode() );
		result = prime * result + ( ( type == null ) ? 0 : type.hashCode() );
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		FieldCacheCollectorFactory other = (FieldCacheCollectorFactory) obj;
		if ( fieldName == null ) {
			if ( other.fieldName != null ) {
				return false;
			}
		}
		else if ( !fieldName.equals( other.fieldName ) ) {
			return false;
		}
		if ( type != other.type ) {
			return false;
		}
		return true;
	}
}
