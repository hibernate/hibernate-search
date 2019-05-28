/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchYearMonthFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexFieldType;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchStandardFieldPredicateBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.projection.impl.ElasticsearchStandardFieldProjectionBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.sort.impl.ElasticsearchStandardFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;

class ElasticsearchYearMonthIndexFieldTypeContext
		extends AbstractElasticsearchTemporalIndexFieldTypeContext<ElasticsearchYearMonthIndexFieldTypeContext, YearMonth> {

	ElasticsearchYearMonthIndexFieldTypeContext(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, YearMonth.class );
	}

	@Override
	protected ElasticsearchIndexFieldType<YearMonth> toIndexFieldType(PropertyMapping mapping, DateTimeFormatter formatter) {
		ElasticsearchYearMonthFieldCodec codec = new ElasticsearchYearMonthFieldCodec( formatter );

		ToDocumentFieldValueConverter<?, ? extends YearMonth> dslToIndexConverter =
				createDslToIndexConverter();
		FromDocumentFieldValueConverter<? super YearMonth, ?> indexToProjectionConverter =
				createIndexToProjectionConverter();

		return new ElasticsearchIndexFieldType<>(
				codec,
				new ElasticsearchStandardFieldPredicateBuilderFactory<>( resolvedSearchable, dslToIndexConverter, createToDocumentRawConverter(), codec ),
				new ElasticsearchStandardFieldSortBuilderFactory<>( resolvedSortable, dslToIndexConverter, createToDocumentRawConverter(), codec ),
				new ElasticsearchStandardFieldProjectionBuilderFactory<>( resolvedProjectable, indexToProjectionConverter, createFromDocumentRawConverter(), codec ),
				mapping
		);
	}

	@Override
	protected ElasticsearchYearMonthIndexFieldTypeContext thisAsS() {
		return this;
	}
}
