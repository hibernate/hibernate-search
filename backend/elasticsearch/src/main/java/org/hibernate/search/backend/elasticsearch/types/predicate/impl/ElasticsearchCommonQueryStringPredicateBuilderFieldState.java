/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.predicate.impl;

import org.hibernate.search.backend.elasticsearch.search.common.impl.AbstractElasticsearchCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexValueFieldContext;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.search.predicate.spi.CommonQueryStringPredicateBuilder;

import com.google.gson.JsonPrimitive;

public final class ElasticsearchCommonQueryStringPredicateBuilderFieldState
		implements CommonQueryStringPredicateBuilder.FieldState {
	private static final String BOOST_OPERATOR = "^";

	private final ElasticsearchSearchIndexValueFieldContext<?> field;
	private Float boost;

	private ElasticsearchCommonQueryStringPredicateBuilderFieldState(
			ElasticsearchSearchIndexValueFieldContext<?> field) {
		this.field = field;
	}

	@Override
	public void boost(float boost) {
		this.boost = boost;
	}

	public ElasticsearchSearchIndexValueFieldContext<?> field() {
		return field;
	}

	public void checkAnalyzerOrNormalizerCompatibleAcrossIndexes() {
		field.type().searchAnalyzerName();
		field.type().normalizerName();
	}

	public JsonPrimitive build() {
		StringBuilder sb = new StringBuilder( field.absolutePath() );
		if ( boost != null ) {
			sb.append( BOOST_OPERATOR ).append( boost );
		}
		return new JsonPrimitive( sb.toString() );
	}

	public static class Factory<T>
			extends
			AbstractElasticsearchCodecAwareSearchQueryElementFactory<ElasticsearchCommonQueryStringPredicateBuilderFieldState,
					T> {
		public Factory(ElasticsearchFieldCodec<T> codec) {
			super( codec );
		}

		@Override
		public ElasticsearchCommonQueryStringPredicateBuilderFieldState create(
				ElasticsearchSearchIndexScope<?> scope, ElasticsearchSearchIndexValueFieldContext<T> field) {
			return new ElasticsearchCommonQueryStringPredicateBuilderFieldState( field );
		}
	}
}
