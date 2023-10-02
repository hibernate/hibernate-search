/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.sort.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.MultiValueMode;
import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneValueFieldSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.types.sort.comparatorsource.impl.LuceneFieldComparatorSource;
import org.hibernate.search.backend.lucene.types.sort.comparatorsource.impl.LuceneGeoPointDistanceComparatorSource;
import org.hibernate.search.engine.search.common.SortMode;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.dsl.SortOrder;
import org.hibernate.search.engine.search.sort.spi.DistanceSortBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.search.Query;
import org.apache.lucene.util.SloppyMath;

public class LuceneGeoPointDistanceSort extends AbstractLuceneDocumentValueSort {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );
	private final double effectiveMissingValue;
	private final GeoPoint center;

	private LuceneGeoPointDistanceSort(Builder builder) {
		super( builder );
		effectiveMissingValue = builder.getEffectiveMissingValue();
		center = builder.center;
	}

	@Override
	protected LuceneFieldComparatorSource doCreateFieldComparatorSource(String nestedDocumentPath,
			MultiValueMode multiValueMode, Query nestedFilter) {
		return new LuceneGeoPointDistanceComparatorSource( nestedDocumentPath, center, effectiveMissingValue, multiValueMode,
				nestedFilter );
	}

	public static class Factory
			extends AbstractLuceneValueFieldSearchQueryElementFactory<DistanceSortBuilder, GeoPoint> {
		@Override
		public DistanceSortBuilder create(LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexValueFieldContext<GeoPoint> field) {
			return new Builder( scope, field );
		}
	}

	private static class Builder extends AbstractBuilder implements DistanceSortBuilder {
		private GeoPoint center;
		private Object missingValue;

		private Builder(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<GeoPoint> field) {
			super( scope, field );
		}

		@Override
		public void center(GeoPoint center) {
			this.center = center;
		}

		@Override
		public void missingFirst() {
			missingValue = SortMissingValue.MISSING_FIRST;
		}

		@Override
		public void missingLast() {
			missingValue = SortMissingValue.MISSING_LAST;
		}

		@Override
		public void missingHighest() {
			missingValue = SortMissingValue.MISSING_HIGHEST;
		}

		@Override
		public void missingLowest() {
			missingValue = SortMissingValue.MISSING_LOWEST;
		}

		@Override
		public void missingAs(GeoPoint value) {
			missingValue = value;
		}

		@Override
		public void mode(SortMode mode) {
			switch ( mode ) {
				case MIN:
				case MAX:
				case AVG:
				case MEDIAN:
					super.mode( mode );
					break;
				case SUM:
				default:
					throw log.invalidSortModeForDistanceSort( mode, getEventContext() );
			}
		}

		@Override
		public SearchSort build() {
			return new LuceneGeoPointDistanceSort( this );
		}

		private double getEffectiveMissingValue() {
			if ( missingValue == null ) {
				// missing value implicit distance (same as ES):
				return Double.POSITIVE_INFINITY;
			}

			if ( missingValue == SortMissingValue.MISSING_FIRST ) {
				return ( order == SortOrder.DESC ) ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
			}

			if ( missingValue == SortMissingValue.MISSING_LAST ) {
				return ( order == SortOrder.DESC ) ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
			}

			if ( missingValue == SortMissingValue.MISSING_LOWEST ) {
				return Double.NEGATIVE_INFINITY;
			}

			if ( missingValue == SortMissingValue.MISSING_HIGHEST ) {
				return Double.POSITIVE_INFINITY;
			}

			if ( missingValue instanceof GeoPoint ) {
				GeoPoint geoPointMissingValue = (GeoPoint) missingValue;

				return SloppyMath.haversinMeters(
						geoPointMissingValue.latitude(), geoPointMissingValue.longitude(),
						center.latitude(), center.longitude()
				);
			}

			throw new AssertionFailure( "Unexpected missing value: " + missingValue );
		}
	}
}
