/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Test;

public abstract class AbstractPredicateConfigurableAnalysisIT extends AbstractPredicateSimpleAnalysisIT {

	protected AbstractPredicateConfigurableAnalysisIT(SimpleMappedIndex<IndexBinding> index,
			SimpleMappedIndex<IndexBinding> compatibleIndex,
			SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex,
			DataSet dataSet) {
		super( index, compatibleIndex, incompatibleIndex, dataSet );
	}

	@Test
	public void analyzerOverride() {
		String whitespaceAnalyzedField = index.binding().whitespaceAnalyzedField.relativeFieldName;
		String whitespaceLowercaseAnalyzedField = index.binding().whitespaceLowercaseAnalyzedField.relativeFieldName;
		String whitespaceLowercaseSearchAnalyzedField =
				index.binding().whitespaceLowercaseSearchAnalyzedField.relativeFieldName;

		// Terms are never lower-cased, neither at write nor at query time.
		assertThatQuery( index.query()
				.where( f -> predicate( f, whitespaceAnalyzedField, "NEW WORLD" ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 1 ) );

		// Terms are always lower-cased, both at write and at query time.
		assertThatQuery( index.query()
				.where( f -> predicate( f, whitespaceLowercaseAnalyzedField, "NEW WORLD" ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ),
						dataSet.docId( 1 ), dataSet.docId( 2 ) );

		// Terms are lower-cased only at query time. Because we are overriding the analyzer in the predicate.
		assertThatQuery( index.query()
				.where( f -> predicateWithAnalyzerOverride( f, whitespaceAnalyzedField, "NEW WORLD",
						DefaultAnalysisDefinitions.ANALYZER_WHITESPACE_LOWERCASE.name ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );

		// Same here. Terms are lower-cased only at query time. Because we've defined a search analyzer.
		assertThatQuery( index.query()
				.where( f -> predicate( f, whitespaceLowercaseSearchAnalyzedField, "NEW WORLD" ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );

		// As for the first query, terms are never lower-cased, neither at write nor at query time.
		// Because even if we've defined a search analyzer, we are overriding it with an analyzer in the predicate,
		// since the overriding takes precedence over the search analyzer.
		assertThatQuery( index.query()
				.where( f -> predicateWithAnalyzerOverride( f, whitespaceLowercaseSearchAnalyzedField, "NEW WORLD",
						DefaultAnalysisDefinitions.ANALYZER_WHITESPACE.name ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 1 ) );
	}

	@Test
	public void analyzerOverride_withNormalizer() {
		String whitespaceAnalyzedField = index.binding().whitespaceAnalyzedField.relativeFieldName;

		assertThatThrownBy( () -> index.query()
				.where( f -> predicateWithAnalyzerOverride( f, whitespaceAnalyzedField, "WORLD",
						// we have a normalizer with that name, but not an analyzer
						DefaultAnalysisDefinitions.NORMALIZER_LOWERCASE.name ) )
				.fetchAll() )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "DefaultAnalysisDefinitions_lowercase" );
	}

	@Test
	public void analyzerOverride_notExistingName() {
		String whitespaceAnalyzedField = index.binding().whitespaceAnalyzedField.relativeFieldName;

		assertThatThrownBy( () -> index.query()
				.where( f -> predicateWithAnalyzerOverride( f, whitespaceAnalyzedField, "WORLD",
						// we don't have any analyzer with that name
						"this_name_does_actually_not_exist" ) )
				.fetchAll() )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "this_name_does_actually_not_exist" );
	}

	@Test
	public void skipAnalysis() {
		String absoluteFieldPath = index.binding().whitespaceLowercaseAnalyzedField.relativeFieldName;

		assertThatQuery( index.query()
				.where( f -> predicate( f, absoluteFieldPath, "BRAVE" ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ),
						dataSet.docId( 1 ), dataSet.docId( 2 ) );

		// ignoring the analyzer means that the parameter of match predicate will not be lowercased
		// so it will not match any token
		assertThatQuery( index.query()
				.where( f -> predicateWithSkipAnalysis( f, absoluteFieldPath, "BRAVE" ) ) )
				.hasNoHits();

		// to have a match with the skipAnalysis option enabled, we have to use the exact case that was indexed
		assertThatQuery( index.query()
				.where( f -> predicateWithSkipAnalysis( f, absoluteFieldPath, "brave" ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ),
						dataSet.docId( 1 ), dataSet.docId( 2 ) );
	}

	@Test
	public void analyzerOverride_normalizedStringField() {
		String absoluteFieldPath = index.binding().normalizedStringField.relativeFieldName;

		assertThatQuery( index.query()
				// the matching parameter will be tokenized even if the field has a normalizer
				.where( f -> predicateWithAnalyzerOverride( f, absoluteFieldPath, "Auster Coe",
						DefaultAnalysisDefinitions.ANALYZER_WHITESPACE_LOWERCASE.name ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 1 ),
						dataSet.docId( 2 ) );
	}

	@Test
	@TestForIssue(jiraKey = { "HSEARCH-2534", "HSEARCH-3042" })
	public void analyzerOverride_queryOnlyAnalyzer() {
		String absoluteFieldPath = index.binding().whitespaceLowercaseAnalyzedField.relativeFieldName;
		String ngramFieldPath = index.binding().ngramSearchAnalyzedField.relativeFieldName;

		// Using the white space lower case analyzer, we don't have any matching.
		assertThatQuery( index.query()
				.where( f -> predicate( f, absoluteFieldPath, "worldofwordcraft" ) ) )
				.hasNoHits();

		// Overriding the analyzer with a n-gram analyzer for the specific query,
		// the same query matches all values.
		assertThatQuery( index.query()
				.where( f -> predicateWithAnalyzerOverride( f, absoluteFieldPath, "worldofwordcraft",
						DefaultAnalysisDefinitions.ANALYZER_NGRAM.name ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ),
						dataSet.docId( 1 ), dataSet.docId( 2 ) );

		// Defining a ngram search analyzer to override the analyzer,
		// we expect the same result.
		assertThatQuery( index.query()
				.where( f -> predicate( f, ngramFieldPath, "worldofwordcraft" ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ),
						dataSet.docId( 1 ), dataSet.docId( 2 ) );
	}

	@Test
	public void multiIndex_incompatibleAnalyzer_overrideAnalyzer() {
		StubMappingScope scope = index.createScope( incompatibleSearchAnalyzerIndex );
		String absoluteFieldPath = index.binding().analyzedStringField.relativeFieldName;

		assertThatQuery( scope.query()
				.where( f -> predicateWithAnalyzerOverride( f, absoluteFieldPath, "fox",
						DefaultAnalysisDefinitions.ANALYZER_WHITESPACE_LOWERCASE.name ) ) )
				.hasDocRefHitsAnyOrder( b -> {
					b.doc( index.typeName(), dataSet.docId( 0 ) );
					b.doc( incompatibleSearchAnalyzerIndex.typeName(), dataSet.docId( 0 ) );
				} );
	}

	@Test
	public void multiIndex_incompatibleAnalyzer_skipAnalysis() {
		StubMappingScope scope = index.createScope( incompatibleSearchAnalyzerIndex );
		String absoluteFieldPath = index.binding().analyzedStringField.relativeFieldName;

		assertThatQuery( scope.query()
				.where( f -> predicateWithSkipAnalysis( f, absoluteFieldPath, "fox" ) ) )
				.hasDocRefHitsAnyOrder( b -> {
					b.doc( index.typeName(), dataSet.docId( 0 ) );
					b.doc( incompatibleSearchAnalyzerIndex.typeName(), dataSet.docId( 0 ) );
				} );
	}

	protected abstract PredicateFinalStep predicateWithAnalyzerOverride(SearchPredicateFactory f, String fieldPath,
			String matchingParam, String analyzerName);

	protected abstract PredicateFinalStep predicateWithSkipAnalysis(SearchPredicateFactory f, String fieldPath,
			String matchingParam);
}
