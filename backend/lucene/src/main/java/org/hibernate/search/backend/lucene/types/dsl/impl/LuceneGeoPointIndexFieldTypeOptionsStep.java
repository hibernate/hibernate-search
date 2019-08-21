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
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
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

		ToDocumentFieldValueConverter<?, ? extends GeoPoint> dslToIndexConverter =
				createDslToIndexConverter();
		FromDocumentFieldValueConverter<? super GeoPoint, ?> indexToProjectionConverter =
				createIndexToProjectionConverter();
		FromDocumentFieldValueConverter<? super GeoPoint, GeoPoint> rawIndexToProjectionConverter =
				createFromDocumentRawConverter();
		LuceneGeoPointFieldCodec codec = new LuceneGeoPointFieldCodec(
				resolvedProjectable, resolvedSearchable, resolvedSortable, indexNullAsValue
		);

		return new LuceneIndexFieldType<>(
				codec,
				new LuceneGeoPointFieldPredicateBuilderFactory(
						resolvedSearchable, dslToIndexConverter, codec
				),
				new LuceneGeoPointFieldSortBuilderFactory( resolvedSortable ),
				new LuceneGeoPointFieldProjectionBuilderFactory(
						resolvedProjectable, codec, indexToProjectionConverter, rawIndexToProjectionConverter
				),
				new LuceneGeoPointFieldAggregationBuilderFactory(
						resolvedAggregable, dslToIndexConverter, indexToProjectionConverter, codec
				),
				resolvedAggregable
		);
	}

	@Override
	protected LuceneGeoPointIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
