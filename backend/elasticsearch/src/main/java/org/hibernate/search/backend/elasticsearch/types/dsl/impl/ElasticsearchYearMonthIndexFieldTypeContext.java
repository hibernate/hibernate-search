/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import static java.time.temporal.ChronoField.MONTH_OF_YEAR;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.util.Arrays;
import java.util.Locale;

import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.DataType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchYearMonthFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexFieldType;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchStandardFieldPredicateBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.projection.impl.ElasticsearchStandardFieldProjectionBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.sort.impl.ElasticsearchStandardFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;

class ElasticsearchYearMonthIndexFieldTypeContext
		extends AbstractElasticsearchScalarFieldTypeContext<ElasticsearchYearMonthIndexFieldTypeContext, YearMonth> {

	static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
			.append( ElasticsearchYearIndexFieldTypeContext.FORMATTER )
			.appendLiteral( '-' )
			.appendValue( MONTH_OF_YEAR, 2 )
			.toFormatter( Locale.ROOT )
			.withResolverStyle( ResolverStyle.STRICT );

	private static final ElasticsearchYearMonthFieldCodec DEFAULT_CODEC = new ElasticsearchYearMonthFieldCodec( FORMATTER );

	private final ElasticsearchYearMonthFieldCodec codec = DEFAULT_CODEC; // TODO add method to allow customization

	ElasticsearchYearMonthIndexFieldTypeContext(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, YearMonth.class, DataType.DATE );
	}

	@Override
	protected ElasticsearchIndexFieldType<YearMonth> toIndexFieldType(PropertyMapping mapping) {
		mapping.setFormat( Arrays.asList( "strict_year_month", "yyyyyyyyy-MM" ) );

		ToDocumentFieldValueConverter<?, ? extends YearMonth> dslToIndexConverter =
				createDslToIndexConverter();
		FromDocumentFieldValueConverter<? super YearMonth, ?> indexToProjectionConverter =
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
	protected ElasticsearchYearMonthIndexFieldTypeContext thisAsS() {
		return this;
	}
}
