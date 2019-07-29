/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.DataTypes;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.types.aggregation.impl.ElasticsearchBooleanFieldAggregationBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.aggregation.impl.ElasticsearchFieldAggregationBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchBooleanFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;

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
			ToDocumentFieldValueConverter<?, ? extends Boolean> dslToIndexConverter,
			ToDocumentFieldValueConverter<Boolean, ? extends Boolean> rawDslToIndexConverter,
			FromDocumentFieldValueConverter<? super Boolean, ?> indexToProjectionConverter,
			FromDocumentFieldValueConverter<? super Boolean, Boolean> rawIndexToProjectionConverter,
			ElasticsearchFieldCodec<Boolean> codec) {
		return new ElasticsearchBooleanFieldAggregationBuilderFactory(
				resolvedAggregable,
				dslToIndexConverter, rawDslToIndexConverter,
				indexToProjectionConverter, rawIndexToProjectionConverter,
				codec
		);
	}
}
