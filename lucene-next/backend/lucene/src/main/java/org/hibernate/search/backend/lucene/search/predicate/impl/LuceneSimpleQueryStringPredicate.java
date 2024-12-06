/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.util.Map;
import java.util.Set;

import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneCommonQueryStringPredicateBuilderFieldState;
import org.hibernate.search.engine.search.common.BooleanOperator;
import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SimpleQueryFlag;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;
import org.hibernate.search.engine.search.predicate.spi.SimpleQueryStringPredicateBuilder;
import org.hibernate.search.util.common.AssertionFailure;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.simple.SimpleQueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.Query;

public class LuceneSimpleQueryStringPredicate extends LuceneCommonQueryStringPredicate {


	private LuceneSimpleQueryStringPredicate(Builder builder) {
		super( builder );
	}

	public static class Builder extends LuceneCommonQueryStringPredicate.Builder implements SimpleQueryStringPredicateBuilder {
		private int flags = -1;

		Builder(LuceneSearchIndexScope<?> scope) {
			super( scope );
		}

		@Override
		public void flags(Set<SimpleQueryFlag> flags) {
			this.flags = toFlagsMask( flags );
		}

		@Override
		public SearchPredicate build() {
			return new LuceneSimpleQueryStringPredicate( this );
		}

		@Override
		protected Query buildQuery(PredicateRequestContext context) {
			SimpleQueryParser queryParser =
					new HibernateSearchSimpleQueryParser( buildAnalyzer(), buildWeights(), fieldStateLookup(), flags, scope );
			queryParser.setDefaultOperator( toOccur( defaultOperator ) );
			return minimumShouldMatchConstraints.apply( queryParser.parse( queryString ) );
		}

		@Override
		protected SearchQueryElementTypeKey<LuceneCommonQueryStringPredicateBuilderFieldState> typeKey() {
			return LucenePredicateTypeKeys.SIMPLE_QUERY_STRING;
		}

		private static BooleanClause.Occur toOccur(BooleanOperator operator) {
			switch ( operator ) {
				case AND:
					return BooleanClause.Occur.MUST;
				case OR:
					return BooleanClause.Occur.SHOULD;
				default:
					throw new AssertionFailure( "Unknown boolean operator: " + operator );
			}
		}

		private static int toFlagsMask(Set<SimpleQueryFlag> flags) {
			int flag = -1;
			if ( flags != null ) {
				flag = 0;
				for ( SimpleQueryFlag operation : flags ) {
					switch ( operation ) {
						case AND:
							flag |= SimpleQueryParser.AND_OPERATOR;
							break;
						case NOT:
							flag |= SimpleQueryParser.NOT_OPERATOR;
							break;
						case OR:
							flag |= SimpleQueryParser.OR_OPERATOR;
							break;
						case PREFIX:
							flag |= SimpleQueryParser.PREFIX_OPERATOR;
							break;
						case PHRASE:
							flag |= SimpleQueryParser.PHRASE_OPERATOR;
							break;
						case PRECEDENCE:
							flag |= SimpleQueryParser.PRECEDENCE_OPERATORS;
							break;
						case ESCAPE:
							flag |= SimpleQueryParser.ESCAPE_OPERATOR;
							break;
						case WHITESPACE:
							flag |= SimpleQueryParser.WHITESPACE_OPERATOR;
							break;
						case FUZZY:
							flag |= SimpleQueryParser.FUZZY_OPERATOR;
							break;
						case NEAR:
							flag |= SimpleQueryParser.NEAR_OPERATOR;
							break;
					}
				}
			}
			return flag;
		}
	}

	private static class HibernateSearchSimpleQueryParser extends SimpleQueryParser {

		private final Map<String, LuceneCommonQueryStringPredicateBuilderFieldState> fieldStates;
		private final LuceneSearchIndexScope<?> scope;

		public HibernateSearchSimpleQueryParser(Analyzer analyzer, Map<String, Float> weights,
				Map<String, LuceneCommonQueryStringPredicateBuilderFieldState> fieldStates, int flags,
				LuceneSearchIndexScope<?> scope) {
			super( analyzer, weights, flags );
			this.fieldStates = fieldStates;
			this.scope = scope;
		}

		@Override
		protected Query createFieldQuery(Analyzer analyzer, BooleanClause.Occur operator, String field,
				String queryText, boolean quoted, int phraseSlop) {
			var state = fieldStates.get( field );

			if ( !state.field().type().valueClass().isAssignableFrom( String.class ) ) {
				var builder = state.field().queryElement( PredicateTypeKeys.MATCH, scope );
				builder.value( queryText, ValueModel.STRING );
				return LuceneSearchPredicate.from( scope, builder.build() ).toQuery( contextForField( state ) );
			}
			return super.createFieldQuery( analyzer, operator, field, queryText, quoted, phraseSlop );
		}

		@Override
		protected Query newPrefixQuery(String text) {
			checkFieldsAreAcceptable( "Prefix", fieldStates );
			return super.newPrefixQuery( text );
		}

		@Override
		protected Query newFuzzyQuery(String text, int fuzziness) {
			checkFieldsAreAcceptable( "Fuzzy", fieldStates );
			return super.newFuzzyQuery( text, fuzziness );
		}
	}
}
