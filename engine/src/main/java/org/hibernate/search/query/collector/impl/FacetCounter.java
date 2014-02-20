/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.query.collector.impl;

import java.util.List;
import java.util.Map;

import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.query.dsl.impl.FacetRange;
import org.hibernate.search.query.dsl.impl.RangeFacetRequest;

import static org.hibernate.search.util.impl.CollectionHelper.newHashMap;

/**
 * @author Hardy Ferentschik
 */
public abstract class FacetCounter {
	private Map<String, IntegerWrapper> counts = newHashMap();

	Map<String, IntegerWrapper> getCounts() {
		return counts;
	}

	void initCount(String value) {
		if ( !counts.containsKey( value ) ) {
			counts.put( value, new IntegerWrapper() );
		}
	}

	void incrementCount(String value) {
		IntegerWrapper integerWrapper = counts.get( value );
		if ( integerWrapper == null ) {
			integerWrapper = new IntegerWrapper();
			counts.put( value, integerWrapper );
		}
		integerWrapper.incrementCount();
	}

	abstract void countValue(Object value);

	public static class SimpleFacetCounter extends FacetCounter {
		@Override
		void countValue(Object value) {
			if ( !( value instanceof String[] ) ) {
				throw new AssertionFailure( "Unexpected field value type " + value.getClass() );
			}
			String[] values = (String[]) value;
			for ( String stringValue : values ) {
				incrementCount( stringValue );
			}
		}
	}

	public static class RangeFacetCounter<T> extends FacetCounter {
		private final List<FacetRange<T>> ranges;
		private final Class<?> fieldCacheType;

		RangeFacetCounter(RangeFacetRequest<T> request) {
			this.fieldCacheType = request.getFieldCacheType();
			this.ranges = request.getFacetRangeList();
			for ( FacetRange<T> range : ranges ) {
				initCount( range.getRangeString() );
			}
		}

		@Override
		@SuppressWarnings("unchecked")
		void countValue(Object value) {
			for ( FacetRange<T> range : ranges ) {
				if ( String[].class.equals( fieldCacheType ) ) {
					String[] stringValues = (String[]) value;
					for ( String stringValue : stringValues ) {
						countIfInRange( (T) stringValue, range );
					}
				}
				else {
					countIfInRange( (T) value, range );
				}
			}
		}

		private void countIfInRange(T value, FacetRange<T> range) {
			if ( range.isInRange( value ) ) {
				incrementCount( range.getRangeString() );
			}
		}
	}
}


