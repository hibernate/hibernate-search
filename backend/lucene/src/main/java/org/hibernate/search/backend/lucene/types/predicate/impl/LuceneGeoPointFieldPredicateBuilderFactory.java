/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateBuilder;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneGeoPointFieldCodec;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinBoundingBoxPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinCirclePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinPolygonPredicateBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class LuceneGeoPointFieldPredicateBuilderFactory
		extends AbstractLuceneFieldPredicateBuilderFactory<GeoPoint> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneGeoPointFieldCodec codec;

	public LuceneGeoPointFieldPredicateBuilderFactory(boolean searchable, LuceneGeoPointFieldCodec codec) {
		super( searchable );
		this.codec = codec;
	}

	@Override
	public MatchPredicateBuilder<LuceneSearchPredicateBuilder> createMatchPredicateBuilder(
			LuceneSearchContext searchContext, LuceneSearchFieldContext<GeoPoint> field) {
		throw log.directValueLookupNotSupportedByGeoPoint( field.eventContext() );
	}

	@Override
	public RangePredicateBuilder<LuceneSearchPredicateBuilder> createRangePredicateBuilder(
			LuceneSearchContext searchContext, LuceneSearchFieldContext<GeoPoint> field) {
		throw log.rangesNotSupportedByGeoPoint( field.eventContext() );
	}

	@Override
	public SpatialWithinCirclePredicateBuilder<LuceneSearchPredicateBuilder> createSpatialWithinCirclePredicateBuilder(
			LuceneSearchFieldContext<GeoPoint> field) {
		checkSearchable( field );
		return new LuceneGeoPointSpatialWithinCirclePredicateBuilder( field );
	}

	@Override
	public SpatialWithinPolygonPredicateBuilder<LuceneSearchPredicateBuilder> createSpatialWithinPolygonPredicateBuilder(
			LuceneSearchFieldContext<GeoPoint> field) {
		checkSearchable( field );
		return new LuceneGeoPointSpatialWithinPolygonPredicateBuilder( field );
	}

	@Override
	public SpatialWithinBoundingBoxPredicateBuilder<LuceneSearchPredicateBuilder> createSpatialWithinBoundingBoxPredicateBuilder(
			LuceneSearchFieldContext<GeoPoint> field) {
		checkSearchable( field );
		return new LuceneGeoPointSpatialWithinBoundingBoxPredicateBuilder( field );
	}

	@Override
	protected LuceneFieldCodec<?> getCodec() {
		return codec;
	}
}
