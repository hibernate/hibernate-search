/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.sort.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.types.sort.impl.LuceneFieldSortContributor;
import org.hibernate.search.engine.logging.spi.FailureContexts;
import org.hibernate.search.engine.search.dsl.sort.SortOrder;
import org.hibernate.search.engine.search.sort.spi.DistanceSortBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.impl.common.LoggerFactory;

class DistanceSortBuilderImpl extends AbstractSearchSortBuilder
		implements DistanceSortBuilder<LuceneSearchSortCollector> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String absoluteFieldPath;

	private final GeoPoint location;

	private final LuceneFieldSortContributor fieldSortContributor;

	DistanceSortBuilderImpl(String absoluteFieldPath, GeoPoint location, LuceneFieldSortContributor fieldSortContributor) {
		this.absoluteFieldPath = absoluteFieldPath;
		this.location = location;
		this.fieldSortContributor = fieldSortContributor;
	}

	@Override
	public void order(SortOrder order) {
		// TODO contribute the support of descending order to Lucene
		if ( SortOrder.DESC == order ) {
			throw log.descendingOrderNotSupportedByDistanceSort(
					FailureContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
			);
		}
	}

	@Override
	public void contribute(LuceneSearchSortCollector collector) {
		fieldSortContributor.contributeDistanceSort( collector, absoluteFieldPath, location, order );
	}
}
