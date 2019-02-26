/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.util.Arrays;
import java.util.Locale;

import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.DataType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchZonedDateTimeFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexFieldType;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchStandardFieldPredicateBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.projection.impl.ElasticsearchStandardFieldProjectionBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.sort.impl.ElasticsearchStandardFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;

class ElasticsearchZonedDateTimeIndexFieldTypeContext
		extends AbstractElasticsearchScalarFieldTypeContext<ElasticsearchZonedDateTimeIndexFieldTypeContext, ZonedDateTime> {

	private static final ElasticsearchZonedDateTimeFieldCodec DEFAULT_CODEC = new ElasticsearchZonedDateTimeFieldCodec(
		new DateTimeFormatterBuilder()
				.append( ElasticsearchOffsetDateTimeIndexFieldTypeContext.FORMATTER )
				// ZoneRegionId is optional
				.optionalStart()
					.appendLiteral( '[' )
					.parseCaseSensitive()
					.appendZoneRegionId()
					.appendLiteral( ']' )
				.optionalEnd()
				.toFormatter( Locale.ROOT )
				.withResolverStyle( ResolverStyle.STRICT )
	);

	private final ElasticsearchZonedDateTimeFieldCodec codec = DEFAULT_CODEC; // TODO HSEARCH-2354 add method to allow customization

	ElasticsearchZonedDateTimeIndexFieldTypeContext(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, ZonedDateTime.class, DataType.DATE );
	}

	@Override
	protected ElasticsearchIndexFieldType<ZonedDateTime> toIndexFieldType(PropertyMapping mapping) {
		mapping.setFormat( Arrays.asList( "yyyy-MM-dd'T'HH:mm:ss.SSSZZ'['ZZZ']'", "yyyyyyyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSZZ'['ZZZ']'" ) );

		ToDocumentFieldValueConverter<?, ? extends ZonedDateTime> dslToIndexConverter =
				createDslToIndexConverter();
		FromDocumentFieldValueConverter<? super ZonedDateTime, ?> indexToProjectionConverter =
				createIndexToProjectionConverter();

		return new ElasticsearchIndexFieldType<>(
				codec,
				new ElasticsearchStandardFieldPredicateBuilderFactory<>( dslToIndexConverter, createRawConverter(), codec ),
				new ElasticsearchStandardFieldSortBuilderFactory<>( resolvedSortable, dslToIndexConverter, createRawConverter(), codec ),
				new ElasticsearchStandardFieldProjectionBuilderFactory<>( resolvedProjectable, indexToProjectionConverter, codec ),
				mapping
		);
	}

	@Override
	protected ElasticsearchZonedDateTimeIndexFieldTypeContext thisAsS() {
		return this;
	}
}
