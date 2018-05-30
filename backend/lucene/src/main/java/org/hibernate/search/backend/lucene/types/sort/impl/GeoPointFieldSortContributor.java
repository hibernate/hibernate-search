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
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortCollector;
import org.hibernate.search.engine.search.dsl.sort.SortOrder;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.impl.common.LoggerFactory;

public class GeoPointFieldSortContributor implements LuceneFieldSortContributor {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public static final GeoPointFieldSortContributor INSTANCE = new GeoPointFieldSortContributor();

	private GeoPointFieldSortContributor() {
	}

	@Override
	public void contribute(LuceneSearchSortCollector collector, String absoluteFieldPath, SortOrder order, Object missingValue) {
		throw log.traditionalSortNotSupportedByGeoPoint( absoluteFieldPath );
	}

	@Override
	public void contributeDistanceSort(LuceneSearchSortCollector collector, String absoluteFieldPath, GeoPoint location, SortOrder order) {
		collector.collectSortField( LatLonDocValuesField.newDistanceSort( absoluteFieldPath, location.getLatitude(), location.getLongitude() ) );
	}

}
