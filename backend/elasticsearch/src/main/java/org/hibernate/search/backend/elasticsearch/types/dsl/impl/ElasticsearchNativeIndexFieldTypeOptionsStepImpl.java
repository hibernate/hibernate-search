/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.search.aggregation.impl.AggregationTypeKeys;
import org.hibernate.search.backend.elasticsearch.search.aggregation.impl.ElasticsearchRangeAggregation;
import org.hibernate.search.backend.elasticsearch.search.aggregation.impl.ElasticsearchTermsAggregation;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchExistsPredicate;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchRangePredicate;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.PredicateTypeKeys;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchFieldProjection;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ProjectionTypeKeys;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.SortTypeKeys;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchJsonElementFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.dsl.ElasticsearchNativeIndexFieldTypeOptionsStep;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchStandardMatchPredicate;
import org.hibernate.search.backend.elasticsearch.types.sort.impl.ElasticsearchStandardFieldSort;
import org.hibernate.search.engine.backend.types.IndexFieldType;

import com.google.gson.Gson;
import com.google.gson.JsonElement;


class ElasticsearchNativeIndexFieldTypeOptionsStepImpl
		extends AbstractElasticsearchIndexFieldTypeOptionsStep<ElasticsearchNativeIndexFieldTypeOptionsStepImpl, JsonElement>
		implements ElasticsearchNativeIndexFieldTypeOptionsStep<ElasticsearchNativeIndexFieldTypeOptionsStepImpl> {

	ElasticsearchNativeIndexFieldTypeOptionsStepImpl(ElasticsearchIndexFieldTypeBuildContext buildContext,
			PropertyMapping mapping) {
		super( buildContext, JsonElement.class, mapping );
	}

	@Override
	protected ElasticsearchNativeIndexFieldTypeOptionsStepImpl thisAsS() {
		return this;
	}

	@Override
	public IndexFieldType<JsonElement> toIndexFieldType() {
		Gson gson = buildContext.getUserFacingGson();

		ElasticsearchJsonElementFieldCodec codec = new ElasticsearchJsonElementFieldCodec( gson );
		builder.codec( codec );

		builder.searchable( true );
		builder.queryElementFactory( PredicateTypeKeys.MATCH,
				new ElasticsearchStandardMatchPredicate.Factory<>( codec ) );
		builder.queryElementFactory( PredicateTypeKeys.RANGE, new ElasticsearchRangePredicate.Factory<>( codec ) );
		builder.queryElementFactory( PredicateTypeKeys.EXISTS, new ElasticsearchExistsPredicate.Factory<>( codec ) );

		builder.sortable( true );
		builder.queryElementFactory( SortTypeKeys.FIELD, new ElasticsearchStandardFieldSort.Factory<>( codec ) );

		builder.projectable( true );
		builder.queryElementFactory( ProjectionTypeKeys.FIELD, new ElasticsearchFieldProjection.Factory<>( codec ) );

		builder.aggregable( true );
		builder.queryElementFactory( AggregationTypeKeys.TERMS, new ElasticsearchTermsAggregation.Factory<>( codec ) );
		builder.queryElementFactory( AggregationTypeKeys.RANGE, new ElasticsearchRangeAggregation.Factory<>( codec ) );

		return builder.build();
	}
}
