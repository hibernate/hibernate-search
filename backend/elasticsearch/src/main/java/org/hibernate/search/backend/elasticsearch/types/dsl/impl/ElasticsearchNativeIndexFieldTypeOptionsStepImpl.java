/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.types.aggregation.impl.ElasticsearchStandardFieldAggregationBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchJsonElementFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.dsl.ElasticsearchNativeIndexFieldTypeOptionsStep;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchStandardFieldPredicateBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.projection.impl.ElasticsearchStandardFieldProjectionBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.sort.impl.ElasticsearchStandardFieldSortBuilderFactory;
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

		builder.predicateBuilderFactory(
				new ElasticsearchStandardFieldPredicateBuilderFactory<>( true, codec ) );
		builder.sortBuilderFactory(
				new ElasticsearchStandardFieldSortBuilderFactory<>( true, codec ) );
		builder.projectionBuilderFactory(
				new ElasticsearchStandardFieldProjectionBuilderFactory<>( true, codec ) );
		builder.aggregationBuilderFactory(
				new ElasticsearchStandardFieldAggregationBuilderFactory<>( true, codec ) );

		return builder.build();
	}
}
