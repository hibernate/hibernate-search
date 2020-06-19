/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldContext;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneGeoPointFieldCodec;
import org.hibernate.search.engine.search.sort.spi.DistanceSortBuilder;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;

public class LuceneGeoPointFieldSortBuilderFactory
		extends AbstractLuceneFieldSortBuilderFactory<GeoPoint, LuceneGeoPointFieldCodec> {

	public LuceneGeoPointFieldSortBuilderFactory(boolean sortable, LuceneGeoPointFieldCodec codec) {
		super( sortable, codec );
	}

	@Override
	public FieldSortBuilder createFieldSortBuilder(
			LuceneSearchContext searchContext, LuceneSearchFieldContext<GeoPoint> field) {
		throw log.traditionalSortNotSupportedByGeoPoint( field.eventContext() );
	}

	@Override
	public DistanceSortBuilder createDistanceSortBuilder(LuceneSearchContext searchContext,
			LuceneSearchFieldContext<GeoPoint> field, GeoPoint center) {
		checkSortable( field );
		return new LuceneGeoPointDistanceSort.Builder( searchContext, field, center );
	}

}
