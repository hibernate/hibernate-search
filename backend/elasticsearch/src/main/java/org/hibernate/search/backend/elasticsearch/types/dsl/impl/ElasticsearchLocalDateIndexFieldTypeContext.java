/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;

import java.time.LocalDate;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.format.SignStyle;
import java.util.Arrays;
import java.util.Locale;

import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.DataType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchLocalDateFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexFieldType;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchStandardFieldPredicateBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.projection.impl.ElasticsearchStandardFieldProjectionBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.sort.impl.ElasticsearchStandardFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;

/**
 * @author Yoann Rodiere
 * @author Guillaume Smet
 */
class ElasticsearchLocalDateIndexFieldTypeContext
		extends AbstractElasticsearchScalarFieldTypeContext<ElasticsearchLocalDateIndexFieldTypeContext, LocalDate> {

	private static final ElasticsearchLocalDateFieldCodec DEFAULT_CODEC = new ElasticsearchLocalDateFieldCodec(
					new DateTimeFormatterBuilder()
							.appendValue( YEAR, 4, 9, SignStyle.EXCEEDS_PAD )
							.appendLiteral( '-' )
							.appendValue( MONTH_OF_YEAR, 2 )
							.appendLiteral( '-' )
							.appendValue( DAY_OF_MONTH, 2 )
							.toFormatter( Locale.ROOT )
							.withResolverStyle( ResolverStyle.STRICT )
			);

	private final ElasticsearchLocalDateFieldCodec codec = DEFAULT_CODEC; // TODO add method to allow customization

	ElasticsearchLocalDateIndexFieldTypeContext(ElasticsearchIndexFieldTypeBuildContext buildContext,
			ElasticsearchIndexSchemaFieldDslBackReference<LocalDate> fieldDslBackReference) {
		super( buildContext, LocalDate.class, DataType.DATE, fieldDslBackReference );
	}

	@Override
	protected ElasticsearchIndexFieldType<LocalDate> toIndexFieldType(PropertyMapping mapping) {
		mapping.setFormat( Arrays.asList( "strict_date", "yyyyyyyyy-MM-dd" ) );

		ToDocumentFieldValueConverter<?, ? extends LocalDate> dslToIndexConverter =
				createDslToIndexConverter();
		FromDocumentFieldValueConverter<? super LocalDate, ?> indexToProjectionConverter =
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
	protected ElasticsearchLocalDateIndexFieldTypeContext thisAsS() {
		return this;
	}
}
