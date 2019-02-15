/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import static java.time.temporal.ChronoField.YEAR;

import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.format.SignStyle;
import java.util.Arrays;
import java.util.Locale;

import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.DataType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchYearFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexFieldType;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchStandardFieldPredicateBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.projection.impl.ElasticsearchStandardFieldProjectionBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.sort.impl.ElasticsearchStandardFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;

class ElasticsearchYearIndexFieldTypeContext
		extends AbstractElasticsearchScalarFieldTypeContext<ElasticsearchYearIndexFieldTypeContext, Year> {

	static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
			.appendValue( YEAR, 4, 9, SignStyle.EXCEEDS_PAD )
			.toFormatter( Locale.ROOT )
			.withResolverStyle( ResolverStyle.STRICT );

	private static final ElasticsearchYearFieldCodec DEFAULT_CODEC = new ElasticsearchYearFieldCodec( FORMATTER );

	private final ElasticsearchYearFieldCodec codec = DEFAULT_CODEC; // TODO HSEARCH-2354 add method to allow customization

	ElasticsearchYearIndexFieldTypeContext(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, Year.class, DataType.DATE );
	}

	@Override
	protected ElasticsearchIndexFieldType<Year> toIndexFieldType(PropertyMapping mapping) {
		mapping.setFormat( Arrays.asList( "strict_year", "yyyyyyyyy" ) );

		ToDocumentFieldValueConverter<?, ? extends Year> dslToIndexConverter =
				createDslToIndexConverter();
		FromDocumentFieldValueConverter<? super Year, ?> indexToProjectionConverter =
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
	protected ElasticsearchYearIndexFieldTypeContext thisAsS() {
		return this;
	}
}
