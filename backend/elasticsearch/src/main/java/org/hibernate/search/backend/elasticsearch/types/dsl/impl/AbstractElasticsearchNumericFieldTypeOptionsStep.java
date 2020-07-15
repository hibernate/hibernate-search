/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.search.aggregation.impl.AggregationTypeKeys;
import org.hibernate.search.backend.elasticsearch.search.aggregation.impl.ElasticsearchRangeAggregation;
import org.hibernate.search.backend.elasticsearch.search.aggregation.impl.ElasticsearchTermsAggregation;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchStandardFieldPredicateBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.projection.impl.ElasticsearchStandardFieldProjectionBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.sort.impl.ElasticsearchStandardFieldSortBuilderFactory;

abstract class AbstractElasticsearchNumericFieldTypeOptionsStep<S extends AbstractElasticsearchNumericFieldTypeOptionsStep<?, F>, F>
		extends AbstractElasticsearchSimpleStandardFieldTypeOptionsStep<S, F> {

	AbstractElasticsearchNumericFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext,
			Class<F> fieldType, String dataType) {
		super( buildContext, fieldType, dataType );
	}

	@Override
	protected final void complete() {
		ElasticsearchFieldCodec<F> codec = completeCodec();
		builder.codec( codec );

		builder.predicateBuilderFactory(
				new ElasticsearchStandardFieldPredicateBuilderFactory<>( resolvedSearchable, codec ) );
		builder.sortBuilderFactory(
				new ElasticsearchStandardFieldSortBuilderFactory<>( resolvedSortable, codec ) );
		builder.projectionBuilderFactory(
				new ElasticsearchStandardFieldProjectionBuilderFactory<>( resolvedProjectable, codec ) );

		if ( resolvedAggregable ) {
			builder.aggregable( true );
			builder.queryElementFactory( AggregationTypeKeys.TERMS, new ElasticsearchTermsAggregation.Factory<>( codec ) );
			builder.queryElementFactory( AggregationTypeKeys.RANGE, new ElasticsearchRangeAggregation.Factory<>( codec ) );
		}
	}

	protected abstract ElasticsearchFieldCodec<F> completeCodec();
}
