/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.projection.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldContext;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneDistanceToFieldProjectionBuilder;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.engine.search.projection.spi.DistanceToFieldProjectionBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;

public class LuceneGeoPointFieldProjectionBuilderFactory extends AbstractLuceneFieldProjectionBuilderFactory<GeoPoint> {

	public LuceneGeoPointFieldProjectionBuilderFactory(boolean projectable, LuceneFieldCodec<GeoPoint> codec) {
		super( projectable, codec );
	}

	@Override
	public DistanceToFieldProjectionBuilder createDistanceProjectionBuilder(LuceneSearchContext searchContext,
			LuceneSearchFieldContext<GeoPoint> field, GeoPoint center) {
		checkProjectable( field );
		return new LuceneDistanceToFieldProjectionBuilder( searchContext, field, codec, center );
	}
}
