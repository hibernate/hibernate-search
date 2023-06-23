/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.predicate.impl;

import org.hibernate.search.backend.elasticsearch.search.common.impl.AbstractElasticsearchValueFieldSearchQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexValueFieldContext;
import org.hibernate.search.engine.search.predicate.spi.SimpleQueryStringPredicateBuilder;

import com.google.gson.JsonPrimitive;

public final class ElasticsearchSimpleQueryStringPredicateBuilderFieldState
		implements SimpleQueryStringPredicateBuilder.FieldState {
	private static final String BOOST_OPERATOR = "^";

	private final ElasticsearchSearchIndexValueFieldContext<String> field;
	private Float boost;

	private ElasticsearchSimpleQueryStringPredicateBuilderFieldState(
			ElasticsearchSearchIndexValueFieldContext<String> field) {
		this.field = field;
	}

	@Override
	public void boost(float boost) {
		this.boost = boost;
	}

	public ElasticsearchSearchIndexValueFieldContext<String> field() {
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

	public static class Factory
			extends
			AbstractElasticsearchValueFieldSearchQueryElementFactory<ElasticsearchSimpleQueryStringPredicateBuilderFieldState,
					String> {
		@Override
		public ElasticsearchSimpleQueryStringPredicateBuilderFieldState create(
				ElasticsearchSearchIndexScope<?> scope, ElasticsearchSearchIndexValueFieldContext<String> field) {
			return new ElasticsearchSimpleQueryStringPredicateBuilderFieldState( field );
		}
	}
}
