/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchHitsAssert.assertThatHits;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.common.BooleanOperator;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.SimpleQueryFlag;
import org.hibernate.search.engine.search.predicate.dsl.SimpleQueryStringPredicateFieldStep;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.StandardFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendFeatures;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SimpleQueryStringPredicateSpecificsIT
		extends AbstractBaseQueryStringPredicateSpecificsIT<SimpleQueryStringPredicateFieldStep<?>> {

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2678")
	@PortedFromSearch5(original = "org.hibernate.search.test.dsl.SimpleQueryStringDSLTest.testSimpleQueryString")
	void moreBooleanOperators() {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().analyzedStringField1.relativeFieldName;
		Function<String, SearchQuery<DocumentReference>> createQuery = queryString -> scope.query()
				.where( f -> predicate( f ).field( absoluteFieldPath ).matching( queryString ) )
				.toQuery();

		assertThatQuery( createQuery.apply( TERM_1 + " + " + TERM_2 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3844") // Used to throw NPE
	void moreNonAnalyzedField() {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().nonAnalyzedField.relativeFieldName;
		Function<String, SearchQuery<DocumentReference>> createQuery = queryString -> scope.query()
				.where( f -> predicate( f ).field( absoluteFieldPath ).matching( queryString ) )
				.toQuery();

		assertThatQuery( createQuery.apply( TERM_1 + " " + TERM_2 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_2, DOCUMENT_3 );

		assertThatQuery( createQuery.apply( TERM_1 + " | " + TERM_2 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_2, DOCUMENT_3 );

		assertThatQuery( createQuery.apply( TERM_1 + " + " + TERM_2 ) )
				.hasNoHits();

		assertThatQuery( createQuery.apply( "-" + TERM_1 + " + " + TERM_2 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_3 );

		assertThatQuery( createQuery.apply( TERM_1 + " + -" + TERM_2 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_2 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3847")
	void booleanOperators_flags() {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().analyzedStringField1.relativeFieldName;

		// Don't use a whitespace here: there's a bug in ES6.2 that leads the "|",
		// when interpreted as an (empty) term, to be turned into a match-no-docs query.
		String orQueryString = TERM_1 + "|" + TERM_2;
		assertThatQuery( scope.query().where( f -> f.simpleQueryString().field( absoluteFieldPath )
				.matching( orQueryString )
				.defaultOperator( BooleanOperator.AND )
				.flags( SimpleQueryFlag.OR ) )
				.toQuery() )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
		assertThatQuery( scope.query().where( f -> f.simpleQueryString().field( absoluteFieldPath )
				.matching( orQueryString )
				.defaultOperator( BooleanOperator.AND )
				.flags( EnumSet.complementOf( EnumSet.of( SimpleQueryFlag.OR ) ) ) )
				.toQuery() )
				// "OR" disabled: "+" is dropped during analysis and we end up with "term1 + term2", since AND is the default operator
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
		assertThatQuery( scope.query().where( f -> f.simpleQueryString().field( absoluteFieldPath )
				.matching( orQueryString )
				.defaultOperator( BooleanOperator.AND )
				.flags( Collections.emptySet() ) ) )
				// All flags disabled: operators are dropped during analysis (empty tokens)
				// and we end up with "term1 + term2", since AND is the default operator.
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );

		String andQueryString = TERM_1 + " + " + TERM_2;
		assertThatQuery( scope.query().where( f -> f.simpleQueryString().field( absoluteFieldPath )
				.matching( andQueryString )
				.defaultOperator( BooleanOperator.OR )
				.flags( SimpleQueryFlag.AND ) )
				.toQuery() )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
		assertThatQuery( scope.query().where( f -> f.simpleQueryString().field( absoluteFieldPath )
				.matching( andQueryString )
				.defaultOperator( BooleanOperator.OR )
				.flags( EnumSet.complementOf( EnumSet.of( SimpleQueryFlag.AND ) ) ) )
				.toQuery() )
				// "AND" disabled: "+" is dropped during analysis and we end up with "term1 | term2", since OR is the default operator
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
		assertThatQuery( scope.query().where( f -> f.simpleQueryString().field( absoluteFieldPath )
				.matching( andQueryString )
				.defaultOperator( BooleanOperator.OR )
				.flags( Collections.emptySet() ) ) )
				// All flags disabled: operators are dropped during analysis (empty tokens)
				// and we end up with "term1 | term2", since OR is the default operator.
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

		String notQueryString = "-" + TERM_1 + " + " + TERM_2;
		assertThatQuery( scope.query().where( f -> f.simpleQueryString().field( absoluteFieldPath )
				.matching( notQueryString )
				.flags( SimpleQueryFlag.AND, SimpleQueryFlag.NOT ) )
				.toQuery() )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_3 );
		assertThatQuery( scope.query().where( f -> f.simpleQueryString().field( absoluteFieldPath )
				.matching( notQueryString )
				.flags( EnumSet.complementOf( EnumSet.of( SimpleQueryFlag.NOT ) ) ) )
				.toQuery() )
				// "NOT" disabled: "-" is dropped during analysis and we end up with "term1 + term2"
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
		assertThatQuery( scope.query().where( f -> f.simpleQueryString().field( absoluteFieldPath )
				.matching( notQueryString )
				.flags( Collections.emptySet() ) ) )
				// All flags disabled: operators are dropped during analysis (empty tokens)
				// and we end up with "term1 | term2", since OR is the default operator.
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

		// Don't use a whitespace here: there's a bug in ES6.2 that leads the "("/")",
		// when interpreted as an (empty) term, to be turned into a match-no-docs query.
		String precedenceQueryString = TERM_2 + "+(" + TERM_1 + "|" + TERM_3 + ")";
		assertThatQuery( scope.query().where( f -> f.simpleQueryString().field( absoluteFieldPath )
				.matching( precedenceQueryString )
				.flags( SimpleQueryFlag.AND, SimpleQueryFlag.OR, SimpleQueryFlag.PRECEDENCE ) )
				.toQuery() )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
		assertThatQuery( scope.query().where( f -> f.simpleQueryString().field( absoluteFieldPath )
				.matching( precedenceQueryString )
				.flags( EnumSet.complementOf( EnumSet.of( SimpleQueryFlag.PRECEDENCE ) ) ) )
				.toQuery() )
				// "PRECENDENCE" disabled: parentheses are dropped during analysis and we end up with "(term2 + term1) | term3"
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2 );
		assertThatQuery( scope.query().where( f -> f.simpleQueryString().field( absoluteFieldPath )
				.matching( precedenceQueryString )
				.flags( Collections.emptySet() ) ) )
				// All flags disabled: operators are dropped during analysis (empty tokens)
				// and we end up with "term2 | term1 | term3", since OR is the default operator.
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3847")
	void phrase_flag() {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().analyzedStringField1.relativeFieldName;

		assertThatQuery( scope.query()
				.where( f -> f.simpleQueryString().field( absoluteFieldPath )
						.matching( "\"" + PHRASE_WITH_TERM_2 + "\"" )
						.flags( SimpleQueryFlag.PHRASE ) )
				.toQuery() )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_3 );

		assertThatQuery( scope.query()
				.where( f -> f.simpleQueryString().field( absoluteFieldPath )
						.matching( "\"" + PHRASE_WITH_TERM_2 + "\"" )
						.flags( EnumSet.complementOf( EnumSet.of( SimpleQueryFlag.PHRASE ) ) ) )
				.toQuery() )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_3 );

		assertThatQuery( scope.query()
				.where( f -> f.simpleQueryString().field( absoluteFieldPath )
						.matching( "\"" + PHRASE_WITH_TERM_2 + "\"" )
						.flags( Collections.emptySet() ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_3 );

		// Slop
		assertThatQuery( scope.query()
				.where( f -> f.simpleQueryString().field( absoluteFieldPath )
						.matching( "\"" + PHRASE_WITH_TERM_4 + "\"~2" )
						.flags( SimpleQueryFlag.PHRASE, SimpleQueryFlag.NEAR ) )
				.toQuery() )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_4 );

		assertThatQuery( scope.query()
				.where( f -> f.simpleQueryString().field( absoluteFieldPath )
						.matching( "\"" + PHRASE_WITH_TERM_4 + "\"~2" )
						.flags( EnumSet.complementOf( EnumSet.of( SimpleQueryFlag.NEAR ) ) ) )
				.toQuery() )
				.hasNoHits();

		assertThatQuery( scope.query()
				.where( f -> f.simpleQueryString().field( absoluteFieldPath )
						.matching( "\"" + PHRASE_WITH_TERM_4 + "\"~2" )
						.flags( Collections.emptySet() ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_4 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3847")
	void fuzzy_flag() {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().analyzedStringField1.relativeFieldName;

		assertThatQuery( scope.query()
				.where( f -> f.simpleQueryString().field( absoluteFieldPath )
						.matching( TERM_1 + "~1" )
						.flags( SimpleQueryFlag.FUZZY ) )
				.toQuery() )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_5 );

		assertThatQuery( scope.query()
				.where( f -> f.simpleQueryString().field( absoluteFieldPath )
						.matching( TERM_1 + "~1" )
						.flags( EnumSet.complementOf( EnumSet.of( SimpleQueryFlag.FUZZY ) ) ) )
				.toQuery() )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2 );

		assertThatQuery( scope.query()
				.where( f -> f.simpleQueryString().field( absoluteFieldPath )
						.matching( TERM_1 + "~1" )
						.flags( Collections.emptySet() ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3847")
	void prefix_flag() {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().analyzedStringField1.relativeFieldName;

		assertThatQuery( scope.query()
				.where( f -> f.simpleQueryString().field( absoluteFieldPath )
						.matching( PREFIX_FOR_TERM_1_AND_TERM_6 + "*" )
						.flags( SimpleQueryFlag.PHRASE, SimpleQueryFlag.PREFIX ) )
				.toQuery() )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_5 );

		assertThatQuery( scope.query()
				.where( f -> f.simpleQueryString().field( absoluteFieldPath )
						.matching( PREFIX_FOR_TERM_1_AND_TERM_6 + "*" )
						.flags( EnumSet.complementOf( EnumSet.of( SimpleQueryFlag.PREFIX ) ) ) )
				.toQuery() )
				.hasNoHits();

		assertThatQuery( scope.query()
				.where( f -> f.simpleQueryString().field( absoluteFieldPath )
						.matching( PREFIX_FOR_TERM_1_AND_TERM_6 + "*" )
						.flags( Collections.emptySet() ) ) )
				.hasNoHits();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3847")
	void whitespace() {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().nonAnalyzedField.relativeFieldName;

		String whitespaceQueryString = TERM_1 + " " + TERM_2;
		assertThatQuery( scope.query().where( f -> f.simpleQueryString().field( absoluteFieldPath )
				.matching( whitespaceQueryString )
				.flags( SimpleQueryFlag.WHITESPACE ) )
				.toQuery() )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_2, DOCUMENT_3 );
		assertThatQuery( scope.query().where( f -> f.simpleQueryString().field( absoluteFieldPath )
				.matching( whitespaceQueryString )
				.flags( EnumSet.complementOf( EnumSet.of( SimpleQueryFlag.WHITESPACE ) ) ) )
				.toQuery() )
				// "WHITESPACE" disabled: "term1 term2" is interpreted as a single term and cannot be found
				.hasNoHits();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3847")
	void escape() {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().analyzedStringField1.relativeFieldName;

		String escapedPrefixQueryString = TERM_1 + "\\*";
		assertThatQuery( scope.query().where( f -> f.simpleQueryString().field( absoluteFieldPath )
				.matching( escapedPrefixQueryString )
				.flags( SimpleQueryFlag.AND, SimpleQueryFlag.NOT, SimpleQueryFlag.ESCAPE ) )
				.toQuery() )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2 );
		assertThatQuery( scope.query().where( f -> f.simpleQueryString().field( absoluteFieldPath )
				.matching( escapedPrefixQueryString )
				.flags( EnumSet.complementOf( EnumSet.of( SimpleQueryFlag.ESCAPE ) ) ) )
				.toQuery() )
				// "ESCAPE" disabled: "\" is interpreted as a literal and the prefix cannot be found
				.hasNoHits();
	}

	@ParameterizedTest
	@MethodSource
	void simpleQueryStringSyntax(String field, String value1, String value2, String noMatch, String unParsableValue) {
		StubMappingScope scope = indexForSyntaxParsingCheck.createScope();

		assertThatHits(
				scope.query()
						.where( f -> f.simpleQueryString().field( field ).matching( value1 ) )
						.fetchAllHits()
		).hasDocRefHitsAnyOrder( indexForSyntaxParsingCheck.typeName(), syntaxDataSet.docId( 1 ) );

		assertThatHits(
				scope.query()
						.where( f -> f.simpleQueryString().field( field ).matching( value1 + " | " + value2 )
								.defaultOperator( BooleanOperator.AND ) )
						.fetchAllHits()
		).hasDocRefHitsAnyOrder( indexForSyntaxParsingCheck.typeName(), syntaxDataSet.docId( 1 ), syntaxDataSet.docId( 2 ) );

		assertThatHits(
				scope.query()
						.where( f -> f.simpleQueryString().field( field ).matching(
								String.format( Locale.ROOT, "(%s | %s) + -%s", value1, value2, value1 ) )
								.defaultOperator( BooleanOperator.AND ) )
						.fetchAllHits()
		).hasDocRefHitsAnyOrder( indexForSyntaxParsingCheck.typeName(), syntaxDataSet.docId( 2 ) );

		if ( noMatch != null ) {

			assertThatHits(
					scope.query()
							.where( f -> f.simpleQueryString().field( field ).matching( "-" + noMatch ) )
							.fetchAllHits()
			).hasDocRefHitsAnyOrder( indexForSyntaxParsingCheck.typeName(), syntaxDataSet.docId( 1 ), syntaxDataSet.docId( 2 ),
					syntaxDataSet.docId( 3 ) );

			assertThatHits(
					scope.query()
							.where( f -> f.simpleQueryString().field( field ).matching( noMatch ) )
							.fetchAllHits()
			).isEmpty();
		}

		assertThatHits(
				scope.query()
						.where( f -> f.simpleQueryString().field( field ).matching(
								String.format( Locale.ROOT, "\"%s\"", value1 ) ) )
						.fetchAllHits()
		).hasDocRefHitsAnyOrder( indexForSyntaxParsingCheck.typeName(), syntaxDataSet.docId( 1 ) );

		assertThatHits(
				scope.query()
						.where( f -> f.simpleQueryString().fields( field, field + "2" ).matching(
								String.format( Locale.ROOT, "\"%s\"", value1 ) ) )
						.fetchAllHits()
		).hasDocRefHitsAnyOrder( indexForSyntaxParsingCheck.typeName(), syntaxDataSet.docId( 1 ) );

		assertThatThrownBy( () -> scope.query()
				.where( f -> f.simpleQueryString().field( field ).matching(
						String.format( Locale.ROOT, "\"%s %s\"", value1, value2 ) ) )
				.fetchAllHits()
		).isInstanceOf( SearchException.class );

		assertThatThrownBy( () -> scope.query()
				.where( f -> f.simpleQueryString().field( field ).matching(
						String.format( Locale.ROOT, "\"%s %s\"~10", value1, value2 ) ) )
				.fetchAllHits()
		).isInstanceOf( SearchException.class );

		assertThatThrownBy( () -> scope.query()
				.where( f -> f.simpleQueryString().field( field ).matching(
						String.format( Locale.ROOT, "%s~10", value1 ) ) )
				.fetchAllHits()
		).isInstanceOf( SearchException.class );

		//  "reason": "Can only use prefix queries on keyword, text and wildcard fields - not on [integer] which is of type [long]",
		assertThatThrownBy( () -> scope.query()
				.where( f -> f.simpleQueryString().field( field ).matching(
						String.format( Locale.ROOT, "%s*", value1 ) ) )
				.fetchAllHits()
		).isInstanceOf( SearchException.class );

		assertThatThrownBy( () -> scope.query()
				.where( f -> f.simpleQueryString().field( field ).matching(
						String.format( Locale.ROOT, "%s", unParsableValue ) ) )
				.fetchAllHits()
		).isInstanceOf( SearchException.class );

	}

	public static List<? extends Arguments> simpleQueryStringSyntax() {
		// String field, String value1, String value2, String noMatch, String unParsableValue
		TckBackendFeatures backendFeatures = TckConfiguration.get().getBackendFeatures();
		List<Arguments> parameters = new ArrayList<>();

		for ( StandardFieldTypeDescriptor<?> typeDescriptor : supported ) {
			parameters.add( arguments( backendFeatures, typeDescriptor ) );
		}

		return parameters;
	}

	@Override
	SimpleQueryStringPredicateFieldStep<?> predicate(SearchPredicateFactory f) {
		return f.simpleQueryString();
	}
}
