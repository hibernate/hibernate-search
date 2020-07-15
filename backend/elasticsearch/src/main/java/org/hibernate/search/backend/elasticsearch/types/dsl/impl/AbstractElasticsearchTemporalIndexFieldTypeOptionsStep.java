/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DataTypes;
import org.hibernate.search.backend.elasticsearch.types.aggregation.impl.ElasticsearchStandardFieldAggregationBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.format.impl.ElasticsearchDefaultFieldFormatProvider;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchStandardFieldPredicateBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.projection.impl.ElasticsearchStandardFieldProjectionBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.sort.impl.ElasticsearchTemporalFieldSortBuilderFactory;

abstract class AbstractElasticsearchTemporalIndexFieldTypeOptionsStep<
				S extends AbstractElasticsearchTemporalIndexFieldTypeOptionsStep<?, F>, F extends TemporalAccessor
		>
		extends AbstractElasticsearchSimpleStandardFieldTypeOptionsStep<S, F> {

	AbstractElasticsearchTemporalIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext,
			Class<F> fieldType) {
		super( buildContext, fieldType, DataTypes.DATE );
	}

	@Override
	protected final void complete() {
		ElasticsearchDefaultFieldFormatProvider defaultFieldFormatProvider =
				buildContext.getDefaultFieldFormatProvider();

		// TODO HSEARCH-2354 add method to allow customization of the format and formatter
		builder.mapping().setFormat( defaultFieldFormatProvider.getDefaultMappingFormat( builder.valueType() ) );

		DateTimeFormatter formatter = defaultFieldFormatProvider.getDefaultDateTimeFormatter( builder.valueType() );

		ElasticsearchFieldCodec<F> codec = createCodec( formatter );
		builder.codec( codec );

		builder.predicateBuilderFactory(
				new ElasticsearchStandardFieldPredicateBuilderFactory<>( resolvedSearchable, codec ) );
		builder.sortBuilderFactory(
				new ElasticsearchTemporalFieldSortBuilderFactory<>( resolvedSortable, codec ) );
		builder.projectionBuilderFactory(
				new ElasticsearchStandardFieldProjectionBuilderFactory<>( resolvedProjectable, codec ) );
		builder.aggregationBuilderFactory(
				new ElasticsearchStandardFieldAggregationBuilderFactory<>( resolvedAggregable, codec ) );
	}

	protected abstract ElasticsearchFieldCodec<F> createCodec(DateTimeFormatter formatter);

}
