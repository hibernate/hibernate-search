/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import org.hibernate.search.backend.lucene.search.projection.impl.LuceneDistanceToFieldProjection;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneFieldProjection;
import org.hibernate.search.backend.lucene.types.codec.impl.DocValues;
import org.hibernate.search.backend.lucene.types.codec.impl.Indexing;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneGeoPointFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.Storage;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexValueFieldType;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneExistsPredicate;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneGeoPointSpatialWithinBoundingBoxPredicate;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneGeoPointSpatialWithinCirclePredicate;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneGeoPointSpatialWithinPolygonPredicate;
import org.hibernate.search.backend.lucene.types.sort.impl.LuceneGeoPointDistanceSort;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;
import org.hibernate.search.engine.search.projection.spi.ProjectionTypeKeys;
import org.hibernate.search.engine.search.sort.spi.SortTypeKeys;
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
	public LuceneIndexValueFieldType<GeoPoint> toIndexFieldType() {
		boolean resolvedSortable = resolveDefault( sortable );
		boolean resolvedProjectable = resolveDefault( projectable );
		boolean resolvedSearchable = resolveDefault( searchable );
		boolean resolvedAggregable = resolveDefault( aggregable );


		Indexing indexing = resolvedSearchable ? Indexing.ENABLED : Indexing.DISABLED;
		// When projectable, we need distance projections; thus we need docValues.
		// CAUTION: we don't enable docValues when aggregable at the moment, because there are no GeoPoint aggregations...
		DocValues docValues = resolvedSortable || resolvedProjectable ? DocValues.ENABLED : DocValues.DISABLED;
		Storage storage = resolvedProjectable ? Storage.ENABLED : Storage.DISABLED;

		LuceneGeoPointFieldCodec codec = new LuceneGeoPointFieldCodec( indexing, docValues, storage, indexNullAsValue );
		builder.codec( codec );

		if ( resolvedSearchable ) {
			builder.searchable( true );
			builder.queryElementFactory( PredicateTypeKeys.EXISTS,
					DocValues.ENABLED.equals( docValues )
							? new LuceneExistsPredicate.DocValuesOrNormsBasedFactory<>()
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
