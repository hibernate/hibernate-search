/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.util.Arrays;
import java.util.Locale;

import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.DataType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchOffsetTimeFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexFieldType;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchStandardFieldPredicateBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.projection.impl.ElasticsearchStandardFieldProjectionBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.sort.impl.ElasticsearchStandardFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;

class ElasticsearchOffsetTimeIndexFieldTypeContext
		extends AbstractElasticsearchScalarFieldTypeContext<ElasticsearchOffsetTimeIndexFieldTypeContext, OffsetTime> {

	private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
			.append( ElasticsearchLocalTimeIndexFieldTypeContext.FORMATTER )
			// OffsetId is mandatory
			.appendOffsetId()
			.toFormatter( Locale.ROOT )
			.withResolverStyle( ResolverStyle.STRICT );

	private static final ElasticsearchOffsetTimeFieldCodec DEFAULT_CODEC = new ElasticsearchOffsetTimeFieldCodec( FORMATTER );

	private final ElasticsearchOffsetTimeFieldCodec codec = DEFAULT_CODEC; // TODO HSEARCH-2354 add method to allow customization

	ElasticsearchOffsetTimeIndexFieldTypeContext(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, OffsetTime.class, DataType.DATE );
	}

	@Override
	protected ElasticsearchIndexFieldType<OffsetTime> toIndexFieldType(PropertyMapping mapping) {
		mapping.setFormat( Arrays.asList( "strict_time" ) );

		ToDocumentFieldValueConverter<?, ? extends OffsetTime> dslToIndexConverter =
				createDslToIndexConverter();
		FromDocumentFieldValueConverter<? super OffsetTime, ?> indexToProjectionConverter =
				createIndexToProjectionConverter();

		return new ElasticsearchIndexFieldType<>(
				codec,
				new ElasticsearchStandardFieldPredicateBuilderFactory<>( dslToIndexConverter, createToDocumentRawConverter(), codec ),
				new ElasticsearchStandardFieldSortBuilderFactory<>( resolvedSortable, dslToIndexConverter, createToDocumentRawConverter(), codec ),
				new ElasticsearchStandardFieldProjectionBuilderFactory<>( resolvedProjectable, indexToProjectionConverter, createFromDocumentRawConverter(), codec ),
				mapping
		);
	}

	@Override
	protected ElasticsearchOffsetTimeIndexFieldTypeContext thisAsS() {
		return this;
	}
}
