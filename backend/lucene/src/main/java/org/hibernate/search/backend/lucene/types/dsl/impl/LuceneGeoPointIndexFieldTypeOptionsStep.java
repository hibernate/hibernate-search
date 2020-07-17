/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import org.hibernate.search.backend.lucene.search.predicate.impl.PredicateTypeKeys;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneDistanceToFieldProjection;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneFieldProjection;
import org.hibernate.search.backend.lucene.search.projection.impl.ProjectionTypeKeys;
import org.hibernate.search.backend.lucene.search.sort.impl.SortTypeKeys;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneGeoPointFieldCodec;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexFieldType;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneExistsPredicate;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneGeoPointSpatialWithinBoundingBoxPredicate;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneGeoPointSpatialWithinCirclePredicate;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneGeoPointSpatialWithinPolygonPredicate;
import org.hibernate.search.backend.lucene.types.sort.impl.LuceneGeoPointDistanceSort;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.spatial.GeoPoint;


class LuceneGeoPointIndexFieldTypeOptionsStep
		extends AbstractLuceneStandardIndexFieldTypeOptionsStep<LuceneGeoPointIndexFieldTypeOptionsStep, GeoPoint> {

	private Sortable sortable = Sortable.DEFAULT;

	LuceneGeoPointIndexFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext) {
		super( buildContext, GeoPoint.class );
	}

	@Override
	public LuceneGeoPointIndexFieldTypeOptionsStep sortable(Sortable sortable) {
		this.sortable = sortable;
		return this;
	}

	@Override
	public LuceneIndexFieldType<GeoPoint> toIndexFieldType() {
		boolean resolvedSortable = resolveDefault( sortable );
		boolean resolvedProjectable = resolveDefault( projectable );
		boolean resolvedSearchable = resolveDefault( searchable );
		boolean resolvedAggregable = resolveDefault( aggregable );

		// When projectable, we need distance projections; thus we need docValues.
		boolean docValues = resolvedSortable || resolvedProjectable;

		LuceneGeoPointFieldCodec codec = new LuceneGeoPointFieldCodec(
				resolvedProjectable, resolvedSearchable, resolvedSortable, indexNullAsValue
		);
		builder.codec( codec );

		if ( resolvedSearchable ) {
			builder.searchable( true );
			builder.queryElementFactory( PredicateTypeKeys.EXISTS,
					docValues ? new LuceneExistsPredicate.DocValuesBasedFactory<>()
							: new LuceneExistsPredicate.DefaultFactory<>() );
			builder.queryElementFactory( PredicateTypeKeys.SPATIAL_WITHIN_CIRCLE,
					new LuceneGeoPointSpatialWithinCirclePredicate.Factory() );
			builder.queryElementFactory( PredicateTypeKeys.SPATIAL_WITHIN_POLYGON,
					new LuceneGeoPointSpatialWithinPolygonPredicate.Factory() );
			builder.queryElementFactory( PredicateTypeKeys.SPATIAL_WITHIN_BOUNDING_BOX,
					new LuceneGeoPointSpatialWithinBoundingBoxPredicate.Factory() );
		}

		if ( resolvedSortable ) {
			builder.sortable( true );
			builder.queryElementFactory( SortTypeKeys.DISTANCE, new LuceneGeoPointDistanceSort.Factory() );
		}

		if ( resolvedProjectable ) {
			builder.projectable( true );
			builder.queryElementFactory( ProjectionTypeKeys.FIELD, new LuceneFieldProjection.Factory<>( codec ) );
			builder.queryElementFactory( ProjectionTypeKeys.DISTANCE, new LuceneDistanceToFieldProjection.Factory( codec ) );
		}

		if ( resolvedAggregable ) {
			builder.aggregable( true );
			// No supported aggregation at the moment.
		}

		return builder.build();
	}

	@Override
	protected LuceneGeoPointIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
