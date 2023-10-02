/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchCommonQueryStringPredicateBuilderFieldState;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SimpleQueryFlag;
import org.hibernate.search.engine.search.predicate.spi.SimpleQueryStringPredicateBuilder;
import org.hibernate.search.util.common.AssertionFailure;

import com.google.gson.JsonObject;

public class ElasticsearchSimpleQueryStringPredicate extends ElasticsearchCommonQueryStringPredicate {

	private static final JsonObjectAccessor SIMPLE_QUERY_STRING_ACCESSOR =
			JsonAccessor.root().property( "simple_query_string" ).asObject();
	private static final JsonAccessor<String> FLAGS_ACCESSOR = JsonAccessor.root().property( "flags" ).asString();

	private final Set<SimpleQueryFlag> flags;

	ElasticsearchSimpleQueryStringPredicate(Builder builder) {
		super( builder );
		flags = builder.flags;
	}

	@Override
	protected void addSpecificProperties(PredicateRequestContext context, JsonObject outerObject, JsonObject innerObject) {
		if ( flags != null ) {
			FLAGS_ACCESSOR.set( innerObject, toFlagsMask( flags ) );
		}
	}

	@Override
	protected JsonObjectAccessor queryNameAccessor() {
		return SIMPLE_QUERY_STRING_ACCESSOR;
	}

	private static String toFlagsMask(Set<SimpleQueryFlag> flags) {
		if ( flags.isEmpty() ) {
			return "NONE";
		}
		StringBuilder flagsMask = new StringBuilder();
		for ( SimpleQueryFlag flag : flags ) {
			if ( flagsMask.length() > 0 ) {
				flagsMask.append( "|" );
			}
			flagsMask.append( getFlagName( flag ) );
		}
		return flagsMask.toString();
	}

	/**
	 * @param flag The flag as defined in Hibernate Search.
	 * @return The name of this flag in Elasticsearch (might be different from flag.name()).
	 */
	private static String getFlagName(SimpleQueryFlag flag) {
		switch ( flag ) {
			case AND:
				return "AND";
			case NOT:
				return "NOT";
			case OR:
				return "OR";
			case PREFIX:
				return "PREFIX";
			case PHRASE:
				return "PHRASE";
			case PRECEDENCE:
				return "PRECEDENCE";
			case ESCAPE:
				return "ESCAPE";
			case WHITESPACE:
				return "WHITESPACE";
			case FUZZY:
				return "FUZZY";
			case NEAR:
				return "SLOP";
			default:
				throw new AssertionFailure( "Unexpected flag: " + flag );
		}
	}

	public static class Builder extends ElasticsearchCommonQueryStringPredicate.Builder
			implements SimpleQueryStringPredicateBuilder {
		private Set<SimpleQueryFlag> flags;

		Builder(ElasticsearchSearchIndexScope<?> scope) {
			super( scope );
		}

		@Override
		public void flags(Set<SimpleQueryFlag> flags) {
			this.flags = flags.isEmpty() ? Collections.emptySet() : EnumSet.copyOf( flags );
		}

		@Override
		protected SearchPredicate doBuild(ElasticsearchCommonQueryStringPredicate.Builder builder) {
			return new ElasticsearchSimpleQueryStringPredicate( this );
		}

		@Override
		protected SearchQueryElementTypeKey<ElasticsearchCommonQueryStringPredicateBuilderFieldState> typeKey() {
			return ElasticsearchPredicateTypeKeys.SIMPLE_QUERY_STRING;
		}
	}
}
