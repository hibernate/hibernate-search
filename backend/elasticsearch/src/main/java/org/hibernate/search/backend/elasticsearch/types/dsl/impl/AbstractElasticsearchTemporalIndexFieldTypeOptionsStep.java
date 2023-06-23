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
import org.hibernate.search.backend.elasticsearch.search.aggregation.impl.ElasticsearchRangeAggregation;
import org.hibernate.search.backend.elasticsearch.search.aggregation.impl.ElasticsearchTermsAggregation;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchExistsPredicate;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchRangePredicate;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchFieldProjection;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.format.impl.ElasticsearchDefaultFieldFormatProvider;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchStandardMatchPredicate;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchTermsPredicate;
import org.hibernate.search.backend.elasticsearch.types.sort.impl.ElasticsearchStandardFieldSort;
import org.hibernate.search.engine.search.aggregation.spi.AggregationTypeKeys;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;
import org.hibernate.search.engine.search.projection.spi.ProjectionTypeKeys;
import org.hibernate.search.engine.search.sort.spi.SortTypeKeys;

abstract class AbstractElasticsearchTemporalIndexFieldTypeOptionsStep<
		S extends AbstractElasticsearchTemporalIndexFieldTypeOptionsStep<?, F>,
		F extends TemporalAccessor>
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
		builder.mapping().setFormat( defaultFieldFormatProvider.getDefaultMappingFormat( builder.valueClass() ) );

		DateTimeFormatter formatter = defaultFieldFormatProvider.getDefaultDateTimeFormatter( builder.valueClass() );

		ElasticsearchFieldCodec<F> codec = createCodec( formatter );
		builder.codec( codec );

		if ( resolvedSearchable ) {
			builder.searchable( true );
			builder.queryElementFactory( PredicateTypeKeys.MATCH,
					new ElasticsearchStandardMatchPredicate.Factory<>( codec ) );
			builder.queryElementFactory( PredicateTypeKeys.RANGE, new ElasticsearchRangePredicate.Factory<>( codec ) );
			builder.queryElementFactory( PredicateTypeKeys.EXISTS, new ElasticsearchExistsPredicate.Factory<>() );
			builder.queryElementFactory( PredicateTypeKeys.TERMS, new ElasticsearchTermsPredicate.Factory<>( codec ) );
		}

		if ( resolvedSortable ) {
			builder.sortable( true );
			builder.queryElementFactory( SortTypeKeys.FIELD,
					new ElasticsearchStandardFieldSort.TemporalFieldFactory<>( codec ) );
		}

		if ( resolvedProjectable ) {
			builder.projectable( true );
			builder.queryElementFactory( ProjectionTypeKeys.FIELD, new ElasticsearchFieldProjection.Factory<>( codec ) );
		}

		if ( resolvedAggregable ) {
			builder.aggregable( true );
			builder.queryElementFactory( AggregationTypeKeys.TERMS, new ElasticsearchTermsAggregation.Factory<>( codec ) );
			builder.queryElementFactory( AggregationTypeKeys.RANGE, new ElasticsearchRangeAggregation.Factory<>( codec ) );
		}
	}

	protected abstract ElasticsearchFieldCodec<F> createCodec(DateTimeFormatter formatter);

}
