/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;

import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.util.Arrays;
import java.util.Locale;

import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.DataType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchMonthDayFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexFieldType;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchStandardFieldPredicateBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.projection.impl.ElasticsearchStandardFieldProjectionBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.sort.impl.ElasticsearchStandardFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;

class ElasticsearchMonthDayIndexFieldTypeContext
		extends AbstractElasticsearchScalarFieldTypeContext<ElasticsearchMonthDayIndexFieldTypeContext, MonthDay> {

	static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
			.appendLiteral( "--" )
			.appendValue( MONTH_OF_YEAR, 2 )
			.appendLiteral( '-' )
			.appendValue( DAY_OF_MONTH, 2 )
			.toFormatter( Locale.ROOT )
			.withResolverStyle( ResolverStyle.STRICT );

	private static final ElasticsearchMonthDayFieldCodec DEFAULT_CODEC = new ElasticsearchMonthDayFieldCodec( FORMATTER );

	private final ElasticsearchMonthDayFieldCodec codec = DEFAULT_CODEC; // TODO add method to allow customization

	ElasticsearchMonthDayIndexFieldTypeContext(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, MonthDay.class, DataType.DATE );
	}

	@Override
	protected ElasticsearchIndexFieldType<MonthDay> toIndexFieldType(PropertyMapping mapping) {
		/*
		 * This seems to be the ISO-8601 format for dates without year.
		 * It's also the default format for Java's MonthDay, see MonthDay.PARSER.
		 */
		mapping.setFormat( Arrays.asList( "--MM-dd" ) );

		ToDocumentFieldValueConverter<?, ? extends MonthDay> dslToIndexConverter =
				createDslToIndexConverter();
		FromDocumentFieldValueConverter<? super MonthDay, ?> indexToProjectionConverter =
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
	protected ElasticsearchMonthDayIndexFieldTypeContext thisAsS() {
		return this;
	}
}
