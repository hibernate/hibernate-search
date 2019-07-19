/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexFieldType;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchStandardFieldPredicateBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.projection.impl.ElasticsearchStandardFieldProjectionBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.sort.impl.ElasticsearchStandardFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;

abstract class AbstractElasticsearchScalarFieldTypeOptionsStep<S extends AbstractElasticsearchScalarFieldTypeOptionsStep<? extends S, F>, F>
		extends AbstractElasticsearchSimpleStandardFieldTypeOptionsStep<S, F> {

	AbstractElasticsearchScalarFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext,
			Class<F> fieldType, String dataType) {
		super( buildContext, fieldType, dataType );
	}

	@Override
	protected final ElasticsearchIndexFieldType<F> toIndexFieldType(PropertyMapping mapping) {
		ElasticsearchFieldCodec<F> codec = complete( mapping );

		ToDocumentFieldValueConverter<?, ? extends F> dslToIndexConverter =
				createDslToIndexConverter();
		ToDocumentFieldValueConverter<F, ? extends F> rawDslToIndexConverter =
				createToDocumentRawConverter();
		FromDocumentFieldValueConverter<? super F, ?> indexToProjectionConverter =
				createIndexToProjectionConverter();
		FromDocumentFieldValueConverter<? super F, F> rawIndexToProjectionConverter =
				createFromDocumentRawConverter();

		return new ElasticsearchIndexFieldType<>(
				codec,
				new ElasticsearchStandardFieldPredicateBuilderFactory<>(
						resolvedSearchable, dslToIndexConverter, rawDslToIndexConverter, codec
				),
				new ElasticsearchStandardFieldSortBuilderFactory<>(
						resolvedSortable, dslToIndexConverter, rawDslToIndexConverter, codec
				),
				new ElasticsearchStandardFieldProjectionBuilderFactory<>(
						resolvedProjectable, indexToProjectionConverter, rawIndexToProjectionConverter, codec
				),
				mapping
		);
	}

	protected abstract ElasticsearchFieldCodec<F> complete(PropertyMapping mapping);
}
