/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.impl;

import java.lang.invoke.MethodHandles;

import org.apache.lucene.document.LatLonDocValuesField;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.sort.impl.AbstractSearchSortBuilder;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortBuilder;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortCollector;
import org.hibernate.search.engine.logging.spi.EventContexts;
import org.hibernate.search.engine.search.dsl.sort.SortOrder;
import org.hibernate.search.engine.search.sort.spi.DistanceSortBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.impl.common.LoggerFactory;

public class GeoPointDistanceSortBuilder extends AbstractSearchSortBuilder
		implements DistanceSortBuilder<LuceneSearchSortBuilder> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String absoluteFieldPath;

	private final GeoPoint location;

	GeoPointDistanceSortBuilder(String absoluteFieldPath, GeoPoint location) {
		this.absoluteFieldPath = absoluteFieldPath;
		this.location = location;
	}

	@Override
	public void order(SortOrder order) {
		// TODO contribute the support of descending order to Lucene
		if ( SortOrder.DESC == order ) {
			throw log.descendingOrderNotSupportedByDistanceSort(
					EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
			);
		}
	}

	@Override
	public void buildAndContribute(LuceneSearchSortCollector collector) {
		collector.collectSortField( LatLonDocValuesField.newDistanceSort( absoluteFieldPath, location.getLatitude(),
				location.getLongitude() ) );
	}
}
