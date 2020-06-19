/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldContext;
import org.hibernate.search.backend.lucene.types.sort.comparatorsource.impl.LuceneFieldComparatorSource;
import org.hibernate.search.backend.lucene.types.sort.comparatorsource.impl.LuceneGeoPointDistanceComparatorSource;
import org.hibernate.search.engine.search.common.SortMode;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.spi.DistanceSortBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class LuceneGeoPointDistanceSort extends AbstractLuceneDocumentValueSort {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	LuceneGeoPointDistanceSort(Builder builder) {
		super( builder );
	}

	public static class Builder extends AbstractBuilder implements DistanceSortBuilder {
		private final GeoPoint location;

		Builder(LuceneSearchContext searchContext, LuceneSearchFieldContext<GeoPoint> field, GeoPoint location) {
			super( searchContext, field );
			this.location = location;
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
					throw log.cannotComputeSumForDistanceSort( getEventContext() );
			}
		}

		@Override
		public SearchSort build() {
			return new LuceneGeoPointDistanceSort( this );
		}

		@Override
		protected LuceneFieldComparatorSource toFieldComparatorSource() {
			return new LuceneGeoPointDistanceComparatorSource( nestedDocumentPath, location, getMultiValueMode(),
					getNestedFilter() );
		}
	}
}
