/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import static org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchCommonMinimumShouldMatchConstraint.formatMinimumShouldMatchConstraints;

import java.lang.invoke.MethodHandles;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchCommonQueryStringPredicateBuilderFieldState;
import org.hibernate.search.engine.search.common.RewriteMethod;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.QueryStringPredicateBuilder;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonObject;

public class ElasticsearchQueryStringPredicate extends ElasticsearchCommonQueryStringPredicate {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final JsonObjectAccessor QUERY_STRING_ACCESSOR = JsonAccessor.root().property( "query_string" ).asObject();

	private static final JsonAccessor<Boolean> ALLOW_LEADING_WILDCARD_ACCESSOR =
			JsonAccessor.root().property( "allow_leading_wildcard" ).asBoolean();
	private static final JsonAccessor<Boolean> ENABLE_POSITION_INCREMENTS_ACCESSOR =
			JsonAccessor.root().property( "enable_position_increments" ).asBoolean();
	private static final JsonAccessor<Integer> PHRASE_SLOP_ACCESSOR = JsonAccessor.root().property( "phrase_slop" ).asInteger();
	private static final JsonAccessor<String> REWRITE_ACCESSOR = JsonAccessor.root().property( "rewrite" ).asString();
	private static final JsonAccessor<String> MINIMUM_SHOULD_MATCH_ACCESSOR =
			JsonAccessor.root().property( "minimum_should_match" ).asString();

	private final Boolean allowLeadingWildcard;
	private final Boolean enablePositionIncrements;
	private final Integer phraseSlop;
	private final RewriteMethod rewriteMethod;
	private final Integer rewriteN;
	private final Map<Integer, ElasticsearchCommonMinimumShouldMatchConstraint> minimumShouldMatchConstraints;


	ElasticsearchQueryStringPredicate(Builder builder) {
		super( builder );

		this.allowLeadingWildcard = builder.allowLeadingWildcard;
		this.enablePositionIncrements = builder.enablePositionIncrements;
		this.phraseSlop = builder.phraseSlop;
		this.rewriteMethod = builder.rewriteMethod;
		this.rewriteN = builder.rewriteN;
		this.minimumShouldMatchConstraints = builder.minimumShouldMatchConstraints;

		builder.minimumShouldMatchConstraints = null;
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

		if ( minimumShouldMatchConstraints != null ) {
			MINIMUM_SHOULD_MATCH_ACCESSOR.set(
					innerObject,
					formatMinimumShouldMatchConstraints( minimumShouldMatchConstraints )
			);
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
		private Map<Integer, ElasticsearchCommonMinimumShouldMatchConstraint> minimumShouldMatchConstraints;


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
		public void minimumShouldMatchNumber(int ignoreConstraintCeiling, int matchingClausesNumber) {
			addMinimumShouldMatchConstraint(
					ignoreConstraintCeiling,
					new ElasticsearchCommonMinimumShouldMatchConstraint( matchingClausesNumber, null )
			);
		}

		@Override
		public void minimumShouldMatchPercent(int ignoreConstraintCeiling, int matchingClausesPercent) {
			addMinimumShouldMatchConstraint(
					ignoreConstraintCeiling,
					new ElasticsearchCommonMinimumShouldMatchConstraint( null, matchingClausesPercent )
			);
		}

		private void addMinimumShouldMatchConstraint(int ignoreConstraintCeiling,
				ElasticsearchCommonMinimumShouldMatchConstraint constraint) {
			if ( minimumShouldMatchConstraints == null ) {
				// We'll need to go through the data in ascending order, so use a TreeMap
				minimumShouldMatchConstraints = new TreeMap<>();
			}
			Object previous = minimumShouldMatchConstraints.put( ignoreConstraintCeiling, constraint );
			if ( previous != null ) {
				throw log.minimumShouldMatchConflictingConstraints( ignoreConstraintCeiling );
			}
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
