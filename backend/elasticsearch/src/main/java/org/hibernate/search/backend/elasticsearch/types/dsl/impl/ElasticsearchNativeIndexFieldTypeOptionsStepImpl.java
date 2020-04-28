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
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexFieldType;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchStandardFieldPredicateBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.projection.impl.ElasticsearchStandardFieldProjectionBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.sort.impl.ElasticsearchStandardFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.backend.types.IndexFieldType;

import com.google.gson.Gson;
import com.google.gson.JsonElement;


class ElasticsearchNativeIndexFieldTypeOptionsStepImpl
		extends AbstractElasticsearchIndexFieldTypeOptionsStep<ElasticsearchNativeIndexFieldTypeOptionsStepImpl, JsonElement>
		implements ElasticsearchNativeIndexFieldTypeOptionsStep<ElasticsearchNativeIndexFieldTypeOptionsStepImpl> {

	private final PropertyMapping mapping;

	ElasticsearchNativeIndexFieldTypeOptionsStepImpl(ElasticsearchIndexFieldTypeBuildContext buildContext,
			PropertyMapping mapping) {
		super( buildContext, JsonElement.class );
		this.mapping = mapping;
	}

	@Override
	protected ElasticsearchNativeIndexFieldTypeOptionsStepImpl thisAsS() {
		return this;
	}

	@Override
	public IndexFieldType<JsonElement> toIndexFieldType() {
		Gson gson = getBuildContext().getUserFacingGson();

		DslConverter<?, ? extends JsonElement> dslConverter = createDslConverter();
		DslConverter<JsonElement, ? extends JsonElement> rawDslConverter = createRawDslConverter();
		ProjectionConverter<? super JsonElement, ?> projectionConverter = createProjectionConverter();
		ProjectionConverter<? super JsonElement, JsonElement> rawProjectionConverter = createRawProjectionConverter();
		ElasticsearchJsonElementFieldCodec codec = new ElasticsearchJsonElementFieldCodec( gson );

		return new ElasticsearchIndexFieldType<>(
				getFieldType(), codec,
				new ElasticsearchStandardFieldPredicateBuilderFactory<>( true, dslConverter, rawDslConverter, codec ),
				new ElasticsearchStandardFieldSortBuilderFactory<>( true, dslConverter, rawDslConverter, codec ),
				new ElasticsearchStandardFieldProjectionBuilderFactory<>(
						true, projectionConverter, rawProjectionConverter, codec
				),
				new ElasticsearchStandardFieldAggregationBuilderFactory<>(
						true,
						dslConverter, rawDslConverter,
						projectionConverter, rawProjectionConverter,
						codec
				),
				mapping
		);
	}
}
