/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.util.Arrays;
import java.util.Locale;

import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.DataType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchLocalDateTimeFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexFieldType;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchStandardFieldPredicateBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.projection.impl.ElasticsearchStandardFieldProjectionBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.sort.impl.ElasticsearchStandardFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;

class ElasticsearchLocalDateTimeIndexFieldTypeContext
		extends AbstractElasticsearchScalarFieldTypeContext<ElasticsearchLocalDateTimeIndexFieldTypeContext, LocalDateTime> {

	private static final ElasticsearchLocalDateTimeFieldCodec DEFAULT_CODEC = new ElasticsearchLocalDateTimeFieldCodec(
					new DateTimeFormatterBuilder()
							.append( ElasticsearchLocalDateIndexFieldTypeContext.FORMATTER )
							.appendLiteral( 'T' )
							.appendValue( HOUR_OF_DAY, 2 )
							.appendLiteral( ':' )
							.appendValue( MINUTE_OF_HOUR, 2 )
							.optionalStart()
							.appendLiteral( ':' )
							.appendValue( SECOND_OF_MINUTE, 2 )
							.optionalStart()
							.appendFraction( NANO_OF_SECOND, 3, 9, true )
							.toFormatter( Locale.ROOT )
							.withResolverStyle( ResolverStyle.STRICT )
			);

	private final ElasticsearchLocalDateTimeFieldCodec codec = DEFAULT_CODEC; // TODO add method to allow customization

	ElasticsearchLocalDateTimeIndexFieldTypeContext(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, LocalDateTime.class, DataType.DATE );
	}

	@Override
	protected ElasticsearchIndexFieldType<LocalDateTime> toIndexFieldType(PropertyMapping mapping) {
		mapping.setFormat( Arrays.asList( "strict_date_hour_minute_second_fraction", "yyyyyyyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS" ) );

		ToDocumentFieldValueConverter<?, ? extends LocalDateTime> dslToIndexConverter =
				createDslToIndexConverter();
		FromDocumentFieldValueConverter<? super LocalDateTime, ?> indexToProjectionConverter =
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
	protected ElasticsearchLocalDateTimeIndexFieldTypeContext thisAsS() {
		return this;
	}
}
