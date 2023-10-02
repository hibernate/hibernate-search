/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import java.util.Locale;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchCommonQueryStringPredicateBuilderFieldState;
import org.hibernate.search.engine.search.common.RewriteMethod;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.QueryStringPredicateBuilder;

import com.google.gson.JsonObject;

public class ElasticsearchQueryStringPredicate extends ElasticsearchCommonQueryStringPredicate {

	private static final JsonObjectAccessor QUERY_STRING_ACCESSOR = JsonAccessor.root().property( "query_string" ).asObject();

	private static final JsonAccessor<Boolean> ALLOW_LEADING_WILDCARD_ACCESSOR =
			JsonAccessor.root().property( "allow_leading_wildcard" ).asBoolean();
	private static final JsonAccessor<Boolean> ENABLE_POSITION_INCREMENTS_ACCESSOR =
			JsonAccessor.root().property( "enable_position_increments" ).asBoolean();
	private static final JsonAccessor<Integer> PHRASE_SLOP_ACCESSOR = JsonAccessor.root().property( "phrase_slop" ).asInteger();
	private static final JsonAccessor<String> REWRITE_ACCESSOR = JsonAccessor.root().property( "rewrite" ).asString();

	private final Boolean allowLeadingWildcard;
	private final Boolean enablePositionIncrements;
	private final Integer phraseSlop;
	private final RewriteMethod rewriteMethod;
	private final Integer rewriteN;

	ElasticsearchQueryStringPredicate(Builder builder) {
		super( builder );

		this.allowLeadingWildcard = builder.allowLeadingWildcard;
		this.enablePositionIncrements = builder.enablePositionIncrements;
		this.phraseSlop = builder.phraseSlop;
		this.rewriteMethod = builder.rewriteMethod;
		this.rewriteN = builder.rewriteN;
	}

	@Override
	protected void addSpecificProperties(PredicateRequestContext context, JsonObject outerObject,
			JsonObject innerObject) {
		if ( this.allowLeadingWildcard != null ) {
			ALLOW_LEADING_WILDCARD_ACCESSOR.set( innerObject, this.allowLeadingWildcard );
		}
		if ( this.enablePositionIncrements != null ) {
			ENABLE_POSITION_INCREMENTS_ACCESSOR.set( innerObject, this.enablePositionIncrements );
		}
		if ( this.phraseSlop != null ) {
			PHRASE_SLOP_ACCESSOR.set( innerObject, this.phraseSlop );
		}
		if ( this.rewriteMethod != null ) {
			REWRITE_ACCESSOR.set( innerObject, rewriteMethodAsString( this.rewriteMethod, this.rewriteN ) );
		}
	}

	private static String rewriteMethodAsString(RewriteMethod rewriteMethod, Integer rewriteN) {
		String string = rewriteMethod.name().toLowerCase( Locale.ROOT );
		if ( rewriteN == null ) {
			return string;
		}
		else {
			return string.substring( 0, string.length() - 1 ) + rewriteN;
		}
	}

	@Override
	protected JsonObjectAccessor queryNameAccessor() {
		return QUERY_STRING_ACCESSOR;
	}


	public static class Builder extends ElasticsearchCommonQueryStringPredicate.Builder implements QueryStringPredicateBuilder {

		private Boolean allowLeadingWildcard;
		private Boolean enablePositionIncrements;
		private Integer phraseSlop;
		private RewriteMethod rewriteMethod;
		private Integer rewriteN;

		Builder(ElasticsearchSearchIndexScope<?> scope) {
			super( scope );
		}

		@Override
		public void allowLeadingWildcard(boolean allowLeadingWildcard) {
			this.allowLeadingWildcard = allowLeadingWildcard;
		}

		@Override
		public void enablePositionIncrements(boolean enablePositionIncrements) {
			this.enablePositionIncrements = enablePositionIncrements;
		}

		@Override
		public void phraseSlop(Integer phraseSlop) {
			this.phraseSlop = phraseSlop;
		}

		@Override
		public void rewriteMethod(RewriteMethod rewriteMethod, Integer n) {
			this.rewriteMethod = rewriteMethod;
			this.rewriteN = n;
		}

		@Override
		protected SearchPredicate doBuild(ElasticsearchCommonQueryStringPredicate.Builder builder) {
			return new ElasticsearchQueryStringPredicate( this );
		}

		@Override
		protected SearchQueryElementTypeKey<ElasticsearchCommonQueryStringPredicateBuilderFieldState> typeKey() {
			return ElasticsearchPredicateTypeKeys.QUERY_STRING;
		}
	}
}
