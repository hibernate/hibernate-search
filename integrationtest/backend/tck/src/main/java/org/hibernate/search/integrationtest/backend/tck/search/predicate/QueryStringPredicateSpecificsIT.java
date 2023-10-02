/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchHitsAssert.assertThatHits;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.common.BooleanOperator;
import org.hibernate.search.engine.search.common.RewriteMethod;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.QueryStringPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.QueryStringPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.StandardFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendFeatures;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class QueryStringPredicateSpecificsIT extends AbstractBaseQueryStringPredicateSpecificsIT<QueryStringPredicateFieldStep<?>> {

	@ParameterizedTest
	@MethodSource("rewriteMethodOptions")
	void rewriteMethodOptions(RewriteMethod rewriteMethod, Integer n) {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().analyzedStringField1.relativeFieldName;

		QueryStringPredicateOptionsStep<?> optionsStep = scope.predicate().queryString()
				.field( absoluteFieldPath )
				.matching( TERM_1 );

		SearchPredicate predicate =
				( n == null ? optionsStep.rewriteMethod( rewriteMethod ) : optionsStep.rewriteMethod( rewriteMethod, n ) )
						.toPredicate();

		SearchQuery<DocumentReference> query = scope.query()
				.where( predicate )
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2 );
	}

	@ParameterizedTest
	@MethodSource("rewriteMethodOptions")
	void rewriteOptions_exceptions(RewriteMethod rewrite, Integer n) {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().analyzedStringField1.relativeFieldName;

		QueryStringPredicateOptionsStep<?> optionsStep = scope.predicate().queryString()
				.field( absoluteFieldPath )
				.matching( TERM_1 );

		assertThatThrownBy(
				() -> ( n != null ? optionsStep.rewriteMethod( rewrite ) : optionsStep.rewriteMethod( rewrite, 10 ) )
						.toPredicate() )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining(
						n == null
								? ( "Cannot use rewrite method '"
										+ rewrite
										+ "': this method does not accept parameter 'n', but it was specified."
										+ " Use another version of the rewrite(...) method that does not accept parameter 'n'." )
								: ( "Cannot use rewrite method '"
										+ rewrite
										+ "': this method requires parameter 'n', which was not specified."
										+ " Use another version of the rewrite(...) method that accepts parameter 'n'." ) );
	}

	public static List<? extends Arguments> rewriteMethodOptions() {
		return List.of(
				Arguments.of( RewriteMethod.CONSTANT_SCORE, null ),
				Arguments.of( RewriteMethod.CONSTANT_SCORE_BOOLEAN, null ),
				Arguments.of( RewriteMethod.SCORING_BOOLEAN, null ),
				Arguments.of( RewriteMethod.TOP_TERMS_BLENDED_FREQS_N, 2 ),
				Arguments.of( RewriteMethod.TOP_TERMS_BOOST_N, 2 ),
				Arguments.of( RewriteMethod.TOP_TERMS_N, 2 )
		);
	}

	@ParameterizedTest
	@MethodSource("phraseSlop")
	void phraseSlop(String phrase, int slop, String docId) {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().analyzedStringField1.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.queryString()
						.field( absoluteFieldPath )
						.matching( phrase )
						.phraseSlop( slop ) )
				.toQuery();
		if ( docId == null ) {
			assertThatQuery( query )
					.hasNoHits();
		}
		else {
			assertThatQuery( query )
					.hasDocRefHitsAnyOrder( index.typeName(), docId );
		}
	}

	public static List<? extends Arguments> phraseSlop() {
		// Here I was, feeding my panda, and the crowd had no word
		String phraseReorderedWords = "\"had crowd\"";
		String phraseWithoutTwoWordsInTheMiddle = "\"crowd word\"";

		return List.of(
				// default value means phrases should be exact as in text:
				Arguments.of( phraseReorderedWords, 0, null ),
				// Transposed terms have a slop of 2
				Arguments.of( phraseReorderedWords, 2, DOCUMENT_1 ),
				// not enough so should not match anything
				Arguments.of( phraseReorderedWords, 1, null ),
				Arguments.of( phraseWithoutTwoWordsInTheMiddle, 1, null ),
				Arguments.of( phraseWithoutTwoWordsInTheMiddle, 2, DOCUMENT_1 ),
				Arguments.of( phraseWithoutTwoWordsInTheMiddle, 5, DOCUMENT_1 )
		);
	}

	@ParameterizedTest
	@MethodSource("allowLeadingWildcard")
	void allowLeadingWildcard(String phrase, Boolean allow, String docId) {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().analyzedStringField1.relativeFieldName;

		QueryStringPredicateOptionsStep<?> step = scope.predicate().queryString()
				.field( absoluteFieldPath )
				.matching( phrase );

		Supplier<SearchPredicate> supplier =
				() -> allow != null ? step.allowLeadingWildcard( allow ).toPredicate() : step.toPredicate();

		if ( docId == null ) {
			assertThatThrownBy( () -> scope.query()
					.where( supplier.get() ).toQuery().fetchAll() )
					// Lucene will throw org.apache.lucene.queryparser.classic.ParseException
					// Elasticsearch will fail with SearchException reported by the backend: "reason": "Failed to parse query ...
					.isInstanceOf( RuntimeException.class );
			return;
		}

		SearchPredicate predicate = supplier.get();

		SearchQuery<DocumentReference> query = scope.query()
				.where( predicate )
				.toQuery();
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( index.typeName(), docId );
	}

	public static List<? extends Arguments> allowLeadingWildcard() {
		// "An elephant ran past John."
		return List.of(
				Arguments.of( "?lephant", null, DOCUMENT_4 ),
				Arguments.of( "?lephant", true, DOCUMENT_4 ),
				Arguments.of( "?lephant", false, null ),

				Arguments.of( "*hant", null, DOCUMENT_4 ),
				Arguments.of( "*hant", true, DOCUMENT_4 ),
				Arguments.of( "*hant", false, null ),

				Arguments.of( "ele?hant", null, DOCUMENT_4 ),
				Arguments.of( "ele?hant", true, DOCUMENT_4 ),
				Arguments.of( "ele?hant", false, DOCUMENT_4 ),

				Arguments.of( "el*nt", null, DOCUMENT_4 ),
				Arguments.of( "el*nt", true, DOCUMENT_4 ),
				Arguments.of( "el*nt", false, DOCUMENT_4 )
		);
	}

	@Test
	void enablePositionIncrements_disabled() {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().analyzedStringField1.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.queryString()
						.field( absoluteFieldPath )
						.matching( "\"out of the room\"" )
						.enablePositionIncrements( false ) )
				.toQuery();

		assertThatQuery( query )
				.hasNoHits();
	}

	@Test
	void enablePositionIncrements_enabled() {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().analyzedStringField1.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.queryString()
						.field( absoluteFieldPath )
						.matching( "\"out of the room\"" )
						.enablePositionIncrements( true ) )
				.toQuery();

		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_2 );
	}

	@Test
	void enablePositionIncrements_default() {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = index.binding().analyzedStringField1.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.queryString()
						.field( absoluteFieldPath )
						.matching( "\"out of the room\"" ) )
				.toQuery();

		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_2 );
	}

	@ParameterizedTest
	@MethodSource
	void queryStringSyntax(String field, String value1, String value2, String noMatch, String unParsableValue) {
		StubMappingScope scope = indexForSyntaxParsingCheck.createScope();

		assertThatHits(
				scope.query()
						.where( f -> f.queryString().field( field ).matching( value1 ) )
						.fetchAllHits()
		).hasDocRefHitsAnyOrder( indexForSyntaxParsingCheck.typeName(), syntaxDataSet.docId( 1 ) );

		assertThatHits(
				scope.query()
						.where( f -> f.queryString().field( field ).matching( value1 + "^10" ) )
						.fetchAllHits()
		).hasDocRefHitsAnyOrder( indexForSyntaxParsingCheck.typeName(), syntaxDataSet.docId( 1 ) );

		assertThatHits(
				scope.query()
						.where( f -> f.queryString().field( field ).matching( value1 + " || " + value2 )
								.defaultOperator( BooleanOperator.AND ) )
						.fetchAllHits()
		).hasDocRefHitsAnyOrder( indexForSyntaxParsingCheck.typeName(), syntaxDataSet.docId( 1 ), syntaxDataSet.docId( 2 ) );

		assertThatHits(
				scope.query()
						.where( f -> f.queryString().field( field ).matching( value1 + " OR " + value2 )
								.defaultOperator( BooleanOperator.AND ) )
						.fetchAllHits()
		).hasDocRefHitsAnyOrder( indexForSyntaxParsingCheck.typeName(), syntaxDataSet.docId( 1 ), syntaxDataSet.docId( 2 ) );

		assertThatHits(
				scope.query()
						.where( f -> f.queryString().field( field ).matching( value1 + " && " + value2 ) )
						.fetchAllHits()
		).isEmpty();

		assertThatHits(
				scope.query()
						.where( f -> f.queryString().field( field ).matching( value1 + " AND " + value2 ) )
						.fetchAllHits()
		).isEmpty();

		assertThatHits(
				scope.query()
						.where( f -> f.queryString().field( field )
								.matching( String.format( Locale.ROOT, "(%s OR %s) AND !%s", value1, value2, value1 ) ) )
						.fetchAllHits()
		).hasDocRefHitsAnyOrder( indexForSyntaxParsingCheck.typeName(), syntaxDataSet.docId( 2 ) );

		// ranges:
		//   for ZDT we have an additional "escape" by wrapping the values in quotes.
		//   See https://github.com/apache/lucene/issues/13234#issuecomment-2076083215
		String value1RangeEscaped;
		String value2RangeEscaped;
		if ( ( value1.contains( "]" ) || value2.contains( "]" ) ) ) {
			value1RangeEscaped = "\"" + value1 + "\"";
			value2RangeEscaped = "\"" + value2 + "\"";
		}
		else {
			value1RangeEscaped = value1;
			value2RangeEscaped = value2;
		}

		assertThatHits(
				scope.query()
						.where( f -> f.queryString().field( field )
								.matching(
										String.format( Locale.ROOT, "[%s TO %s]", value1RangeEscaped, value2RangeEscaped ) ) )
						.fetchAllHits()
		).hasDocRefHitsAnyOrder(
				indexForSyntaxParsingCheck.typeName(), syntaxDataSet.docId( 1 ), syntaxDataSet.docId( 2 ) );

		assertThatHits(
				scope.query()
						.where( f -> f.queryString().field( field )
								.matching(
										String.format( Locale.ROOT, "{%s TO %s}", value1RangeEscaped, value2RangeEscaped ) ) )
						.fetchAllHits()
		).isEmpty();

		assertThatHits(
				scope.query()
						.where( f -> f.queryString().field( field )
								.matching(
										String.format( Locale.ROOT, "[%s TO %s}", value1RangeEscaped, value2RangeEscaped ) ) )
						.fetchAllHits()
		).hasDocRefHitsAnyOrder( indexForSyntaxParsingCheck.typeName(), syntaxDataSet.docId( 1 ) );

		assertThatHits(
				scope.query()
						.where( f -> f.queryString().field( field )
								.matching(
										String.format( Locale.ROOT, "{%s TO %s]", value1RangeEscaped, value2RangeEscaped ) ) )
						.fetchAllHits()
		).hasDocRefHitsAnyOrder( indexForSyntaxParsingCheck.typeName(), syntaxDataSet.docId( 2 ) );

		if ( noMatch != null ) {
			assertThatHits(
					scope.query()
							.where( f -> f.queryString().field( field )
									.matching( String.format( Locale.ROOT, "[%s TO *]", value1RangeEscaped ) ) )
							.fetchAllHits()
			).hasDocRefHitsAnyOrder(
					indexForSyntaxParsingCheck.typeName(), syntaxDataSet.docId( 1 ), syntaxDataSet.docId( 2 ),
					syntaxDataSet.docId( 3 )
			);
			assertThatHits(
					scope.query()
							.where( f -> f.queryString().field( field )
									.matching( String.format( Locale.ROOT, "{%s TO *]", value1RangeEscaped ) ) )
							.fetchAllHits()
			).hasDocRefHitsAnyOrder( indexForSyntaxParsingCheck.typeName(), syntaxDataSet.docId( 2 ),
					syntaxDataSet.docId( 3 )
			);

			assertThatHits(
					scope.query()
							.where( f -> f.queryString().field( field ).matching( "-" + noMatch ) )
							.fetchAllHits()
			).hasDocRefHitsAnyOrder( indexForSyntaxParsingCheck.typeName(), syntaxDataSet.docId( 1 ),
					syntaxDataSet.docId( 2 ),
					syntaxDataSet.docId( 3 )
			);
			assertThatHits(
					scope.query()
							.where( f -> f.queryString().field( field ).matching( "!" + noMatch ) )
							.fetchAllHits()
			).hasDocRefHitsAnyOrder( indexForSyntaxParsingCheck.typeName(), syntaxDataSet.docId( 1 ),
					syntaxDataSet.docId( 2 ),
					syntaxDataSet.docId( 3 )
			);

			assertThatHits(
					scope.query()
							.where( f -> f.queryString().field( field ).matching( noMatch ) )
							.fetchAllHits()
			).isEmpty();
		}
		else {
			assertThatHits(
					scope.query()
							.where( f -> f.queryString().field( field )
									.matching( String.format( Locale.ROOT, "[%s TO *]", value1RangeEscaped ) ) )
							.fetchAllHits()
			).hasDocRefHitsAnyOrder( indexForSyntaxParsingCheck.typeName(), syntaxDataSet.docId( 1 ),
					syntaxDataSet.docId( 2 )
			);

			assertThatHits(
					scope.query()
							.where( f -> f.queryString().field( field )
									.matching( String.format( Locale.ROOT, "{%s TO *]", value1RangeEscaped ) ) )
							.fetchAllHits()
			).hasDocRefHitsAnyOrder( indexForSyntaxParsingCheck.typeName(), syntaxDataSet.docId( 2 ) );
		}

		assertThatHits(
				scope.query()
						.where( f -> f.queryString().field( field )
								.matching( String.format( Locale.ROOT, "[* TO %s]", value2RangeEscaped ) ) )
						.fetchAllHits()
		).hasDocRefHitsAnyOrder(
				indexForSyntaxParsingCheck.typeName(), syntaxDataSet.docId( 1 ), syntaxDataSet.docId( 2 ) );

		assertThatHits(
				scope.query()
						.where( f -> f.queryString().field( field )
								.matching( String.format( Locale.ROOT, "[* TO %s}", value2RangeEscaped ) ) )
						.fetchAllHits()
		).hasDocRefHitsAnyOrder( indexForSyntaxParsingCheck.typeName(), syntaxDataSet.docId( 1 ) );

		assertThatHits(
				scope.query()
						.where( f -> f.queryString().field( field ).matching( String.format( Locale.ROOT, "\"%s\"", value1 ) ) )
						.fetchAllHits()
		).hasDocRefHitsAnyOrder( indexForSyntaxParsingCheck.typeName(), syntaxDataSet.docId( 1 ) );

		assertThatHits(
				scope.query()
						.where( f -> f.queryString().fields( field, field + 2 )
								.matching( String.format( Locale.ROOT, "\"%s\"", value1 ) ) )
						.fetchAllHits()
		).hasDocRefHitsAnyOrder( indexForSyntaxParsingCheck.typeName(), syntaxDataSet.docId( 1 ) );

		assertThatThrownBy( () -> scope.query()
				.where( f -> f.queryString().field( field )
						.matching( String.format( Locale.ROOT, "\"%s %s\"", value1, value2 ) ) )
				.fetchAllHits()
		).isInstanceOf( SearchException.class );

		assertThatThrownBy( () -> scope.query()
				.where( f -> f.queryString().field( field )
						.matching( String.format( Locale.ROOT, "\"%s %s\"~10", value1, value2 ) ) )
				.fetchAllHits()
		).isInstanceOf( SearchException.class );

		assertThatThrownBy( () -> scope.query()
				.where( f -> f.queryString().field( field ).matching( String.format( Locale.ROOT, "%s~10", value1 ) ) )
				.fetchAllHits()
		).isInstanceOf( SearchException.class );

		//  "reason": "Can only use prefix queries on keyword, text and wildcard fields - not on [integer] which is of type [long]",
		assertThatThrownBy( () -> scope.query()
				.where( f -> f.queryString().field( field ).matching( String.format( Locale.ROOT, "%s*", value1 ) ) )
				.fetchAllHits()
		).isInstanceOf( SearchException.class );

		if ( TckConfiguration.get().getBackendFeatures().queryStringFailOnPatternQueries() ) {
			assertThatThrownBy( () -> scope.query()
					.where( f -> f.queryString().field( field ).matching( String.format( Locale.ROOT, "/%s/", value2 ) ) )
					.fetchAllHits()
			).isInstanceOf( SearchException.class );
		}

		assertThatThrownBy( () -> scope.query()
				.where( f -> f.queryString().field( field ).matching( String.format( Locale.ROOT, "%s?", value2 ) ) )
				.fetchAllHits()
		).isInstanceOf( SearchException.class );

		assertThatThrownBy( () -> scope.query()
				.where( f -> f.queryString().field( field ).matching(
						String.format( Locale.ROOT, "%s", unParsableValue ) ) )
				.fetchAllHits()
		).isInstanceOf( SearchException.class );

	}

	public static List<? extends Arguments> queryStringSyntax() {
		// String field, String value1, String value2, String noMatch, String unParsableValue
		TckBackendFeatures backendFeatures = TckConfiguration.get().getBackendFeatures();
		List<Arguments> parameters = new ArrayList<>();

		for ( StandardFieldTypeDescriptor<?> typeDescriptor : supported ) {
			parameters.add( arguments( backendFeatures, typeDescriptor ) );
		}

		return parameters;
	}

	@Override
	QueryStringPredicateFieldStep<?> predicate(SearchPredicateFactory f) {
		return f.queryString();
	}
}
