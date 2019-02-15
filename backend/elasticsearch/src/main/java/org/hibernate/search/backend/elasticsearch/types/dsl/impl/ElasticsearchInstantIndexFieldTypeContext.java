/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.DataType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchInstantFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexFieldType;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchStandardFieldPredicateBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.projection.impl.ElasticsearchStandardFieldProjectionBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.sort.impl.ElasticsearchStandardFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;

class ElasticsearchInstantIndexFieldTypeContext
		extends AbstractElasticsearchScalarFieldTypeContext<ElasticsearchInstantIndexFieldTypeContext, Instant> {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_INSTANT;

	private static final ElasticsearchInstantFieldCodec DEFAULT_CODEC = new ElasticsearchInstantFieldCodec( FORMATTER );

	private final ElasticsearchInstantFieldCodec codec = DEFAULT_CODEC; // TODO HSEARCH-2354 add method to allow customization

	ElasticsearchInstantIndexFieldTypeContext(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, Instant.class, DataType.DATE );
	}

	@Override
	protected ElasticsearchIndexFieldType<Instant> toIndexFieldType(PropertyMapping mapping) {
		// Use default formats ("strict_date_optional_time||epoch_millis")

		ToDocumentFieldValueConverter<?, ? extends Instant> dslToIndexConverter =
				createDslToIndexConverter();
		FromDocumentFieldValueConverter<? super Instant, ?> indexToProjectionConverter =
				createIndexToProjectionConverter();

		return new ElasticsearchIndexFieldType<>(
				codec,
				new ElasticsearchStandardFieldPredicateBuilderFactory<>( dslToIndexConverter, codec ),
				new ElasticsearchStandardFieldSortBuilderFactory<>( resolvedSortable, dslToIndexConverter, codec ),
				new ElasticsearchStandardFieldProjectionBuilderFactory<>( resolvedProjectable, indexToProjectionConverter, codec ),
				mapping
		);
	}

	@Override
	protected ElasticsearchInstantIndexFieldTypeContext thisAsS() {
		return this;
	}
}
