/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DataTypes;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.types.aggregation.impl.ElasticsearchBooleanFieldAggregationBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.aggregation.impl.ElasticsearchFieldAggregationBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchBooleanFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;

class ElasticsearchBooleanIndexFieldTypeOptionsStep
		extends
		AbstractElasticsearchScalarFieldTypeOptionsStep<ElasticsearchBooleanIndexFieldTypeOptionsStep, Boolean> {

	ElasticsearchBooleanIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, Boolean.class, DataTypes.BOOLEAN );
	}

	@Override
	protected ElasticsearchFieldCodec<Boolean> complete(PropertyMapping mapping) {
		return ElasticsearchBooleanFieldCodec.INSTANCE;
	}

	@Override
	protected ElasticsearchBooleanIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}

	@Override
	protected ElasticsearchFieldAggregationBuilderFactory createAggregationBuilderFactory(boolean resolvedAggregable,
			DslConverter<?, ? extends Boolean> dslToIndexConverter,
			DslConverter<Boolean, ? extends Boolean> rawDslToIndexConverter,
			ProjectionConverter<? super Boolean, ?> indexToProjectionConverter,
			ProjectionConverter<? super Boolean, Boolean> rawIndexToProjectionConverter,
			ElasticsearchFieldCodec<Boolean> codec) {
		return new ElasticsearchBooleanFieldAggregationBuilderFactory(
				resolvedAggregable,
				dslToIndexConverter, rawDslToIndexConverter,
				indexToProjectionConverter, rawIndexToProjectionConverter,
				codec
		);
	}
}
