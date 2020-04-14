/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import org.hibernate.search.backend.lucene.types.aggregation.impl.LuceneGeoPointFieldAggregationBuilderFactory;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneGeoPointFieldCodec;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexFieldType;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneGeoPointFieldPredicateBuilderFactory;
import org.hibernate.search.backend.lucene.types.projection.impl.LuceneGeoPointFieldProjectionBuilderFactory;
import org.hibernate.search.backend.lucene.types.sort.impl.LuceneGeoPointFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
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

		DslConverter<?, ? extends GeoPoint> dslConverter = createDslConverter();
		ProjectionConverter<? super GeoPoint, ?> projectionConverter = createProjectionConverter();
		ProjectionConverter<? super GeoPoint, GeoPoint> rawProjectionConverter = createRawProjectionConverter();
		LuceneGeoPointFieldCodec codec = new LuceneGeoPointFieldCodec(
				resolvedProjectable, resolvedSearchable, resolvedSortable, indexNullAsValue
		);

		return new LuceneIndexFieldType<>(
				codec,
				new LuceneGeoPointFieldPredicateBuilderFactory(
						resolvedSearchable, dslConverter, codec
				),
				new LuceneGeoPointFieldSortBuilderFactory( resolvedSortable ),
				new LuceneGeoPointFieldProjectionBuilderFactory(
						resolvedProjectable, codec, projectionConverter, rawProjectionConverter
				),
				new LuceneGeoPointFieldAggregationBuilderFactory(
						resolvedAggregable, dslConverter, projectionConverter, codec
				)
		);
	}

	@Override
	protected LuceneGeoPointIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
