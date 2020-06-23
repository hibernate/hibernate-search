/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.util.List;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.AnalyzedStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.NormalizedStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Test;

public abstract class AbstractPredicateAnalysisIT {

	private final SimpleMappedIndex<IndexBinding> index;
	private final SimpleMappedIndex<IndexBinding> compatibleSearchAnalyzerIndex;
	private final SimpleMappedIndex<IncompatibleIndexBinding> incompatibleSearchAnalyzerIndex;
	private final DataSet dataSet;

	protected AbstractPredicateAnalysisIT(SimpleMappedIndex<IndexBinding> index,
			SimpleMappedIndex<IndexBinding> compatibleIndex,
			SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex,
			DataSet dataSet) {
		this.index = index;
		this.compatibleSearchAnalyzerIndex = compatibleIndex;
		this.incompatibleSearchAnalyzerIndex = incompatibleIndex;
		this.dataSet = dataSet;
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.dsl.SimpleQueryStringDSLTest.testEmptyQueryString")
	public void emptyStringBeforeAnalysis() {
		assertThatQuery( index.query()
				.where( f -> predicate( f, index.binding().analyzedStringField.relativeFieldName, "" ) ) )
				.hasNoHits();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2700")
	@PortedFromSearch5(original = "org.hibernate.search.test.dsl.SimpleQueryStringDSLTest.testBlankQueryString")
	public void blankStringBeforeAnalysis() {
		assertThatQuery( index.query()
				.where( f -> predicate( f, index.binding().analyzedStringField.relativeFieldName, "   " ) ) )
				.hasNoHits();
	}

	@Test
	public void noTokenAfterAnalysis() {
		assertThatQuery( index.query()
				// Use a stopword, which should be removed by the analysis
				.where( f -> predicate( f, index.binding().analyzedStringField.relativeFieldName, "a" ) ) )
				.hasNoHits();
	}

	@Test
	public void analyzerOverride() {
		String whitespaceAnalyzedField = index.binding().whitespaceAnalyzedField.relativeFieldName;
		String whitespaceLowercaseAnalyzedField = index.binding().whitespaceLowercaseAnalyzedField.relativeFieldName;
		String whitespaceLowercaseSearchAnalyzedField = index.binding().whitespaceLowercaseSearchAnalyzedField.relativeFieldName;

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
	@TestForIssue( jiraKey = { "HSEARCH-2534", "HSEARCH-3042" } )
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
	public void multiIndex_incompatibleAnalyzer() {
		StubMappingScope scope = index.createScope( incompatibleSearchAnalyzerIndex );
		String absoluteFieldPath = index.binding().analyzedStringField.relativeFieldName;

		assertThatThrownBy( () -> scope.query()
				.where( f -> predicate( f, absoluteFieldPath, "fox" ) )
				.fetchAll() )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting types" )
				.hasMessageContaining( "'analyzedString'" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( index.name(), incompatibleSearchAnalyzerIndex.name() )
				) );
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
	public void multiIndex_incompatibleAnalyzer_searchAnalyzer() {
		StubMappingScope scope = index.createScope( compatibleSearchAnalyzerIndex );
		String absoluteFieldPath = index.binding().analyzedStringField.relativeFieldName;

		assertThatQuery( scope.query()
				.where( f -> predicate( f, absoluteFieldPath, "fox" ) ) )
				.hasDocRefHitsAnyOrder( b -> {
					b.doc( index.typeName(), dataSet.docId( 0 ) );
					b.doc( compatibleSearchAnalyzerIndex.typeName(), dataSet.docId( 0 ) );
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

	protected abstract PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, String matchingParam);

	protected abstract PredicateFinalStep predicateWithAnalyzerOverride(SearchPredicateFactory f, String fieldPath,
			String matchingParam, String analyzerName);

	protected abstract PredicateFinalStep predicateWithSkipAnalysis(SearchPredicateFactory f, String fieldPath,
			String matchingParam);

	public static final class IndexBinding {
		private final SimpleFieldModel<String> analyzedStringField;
		private final SimpleFieldModel<String> normalizedStringField;
		private final SimpleFieldModel<String> whitespaceAnalyzedField;
		private final SimpleFieldModel<String> whitespaceLowercaseAnalyzedField;
		private final SimpleFieldModel<String> whitespaceLowercaseSearchAnalyzedField;
		private final SimpleFieldModel<String> ngramSearchAnalyzedField;

		public IndexBinding(IndexSchemaElement root) {
			analyzedStringField = SimpleFieldModel.mapperWithOverride(
					AnalyzedStringFieldTypeDescriptor.INSTANCE,
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
			)
					.map( root, "analyzedString" );
			normalizedStringField = SimpleFieldModel.mapperWithOverride(
					NormalizedStringFieldTypeDescriptor.INSTANCE,
					c -> c.asString().normalizer( DefaultAnalysisDefinitions.NORMALIZER_LOWERCASE.name )
			)
					.map( root, "normalizedString" );
			whitespaceAnalyzedField = SimpleFieldModel.mapperWithOverride(
					AnalyzedStringFieldTypeDescriptor.INSTANCE,
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_WHITESPACE.name )
			)
					.map( root, "whitespaceAnalyzed" );
			whitespaceLowercaseAnalyzedField = SimpleFieldModel.mapperWithOverride(
					AnalyzedStringFieldTypeDescriptor.INSTANCE,
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_WHITESPACE_LOWERCASE.name )
			)
					.map( root, "whitespaceLowercaseAnalyzed" );
			whitespaceLowercaseSearchAnalyzedField = SimpleFieldModel.mapperWithOverride(
					AnalyzedStringFieldTypeDescriptor.INSTANCE,
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_WHITESPACE.name )
							.searchAnalyzer( DefaultAnalysisDefinitions.ANALYZER_WHITESPACE_LOWERCASE.name )
			)
					.map( root, "whitespaceLowercaseSearchAnalyzed" );
			ngramSearchAnalyzedField = SimpleFieldModel.mapperWithOverride(
					AnalyzedStringFieldTypeDescriptor.INSTANCE,
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_WHITESPACE_LOWERCASE.name )
							.searchAnalyzer( DefaultAnalysisDefinitions.ANALYZER_NGRAM.name )
			)
					.map( root, "ngramSearchAnalyzed" );
		}
	}

	public static final class IncompatibleIndexBinding {
		private final SimpleFieldModel<String> analyzedStringField;

		public IncompatibleIndexBinding(IndexSchemaElement root) {
			// Just use a different analyzer
			analyzedStringField = SimpleFieldModel.mapperWithOverride(
					AnalyzedStringFieldTypeDescriptor.INSTANCE,
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_WHITESPACE.name )
			)
					.map( root, "analyzedString" );
		}
	}

	public static final class DataSet extends AbstractPredicateDataSet {
		private final List<String> analyzedFieldValues = CollectionHelper.asImmutableList(
				"quick brown fox", "another word", "a"
		);
		private final List<String> normalizedStringFieldValues = CollectionHelper.asImmutableList(
				"Irving", "Auster", "Coe"
		);
		private final List<String> whitespaceAnalyzedFieldValues = CollectionHelper.asImmutableList(
				"brave new world", "BRAVE NEW WORLD", "BRave NeW WoRlD"
		);
		private final List<String> whitespaceLowercaseAnalyzedFieldValues = CollectionHelper.asImmutableList(
				"brave new world", "BRAVE NEW WORLD", "BRave NeW WoRlD"
		);
		private final List<String> whitespaceLowercaseSearchAnalyzedFieldValues = CollectionHelper.asImmutableList(
				"brave new world", "BRAVE NEW WORLD", "BRave NeW WoRlD"
		);
		private final List<String> ngramSearchAnalyzedFieldValues = CollectionHelper.asImmutableList(
				"brave new world", "BRAVE NEW WORLD", "BRave NeW WoRlD"
		);

		public DataSet() {
			super( null );
		}

		public void contribute(SimpleMappedIndex<IndexBinding> index, BulkIndexer indexer,
				SimpleMappedIndex<IndexBinding> compatibleIndex, BulkIndexer compatibleIndexer,
				SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex, BulkIndexer incompatibleIndexer) {
			indexer.add( docId( 0 ), routingKey,
					document -> initCompatibleDocument( index, document, 0 ) );
			indexer.add( docId( 1 ), routingKey,
					document -> initCompatibleDocument( index, document, 1 ) );
			indexer.add( docId( 2 ), routingKey,
					document -> initCompatibleDocument( index, document, 2 ) );
			compatibleIndexer.add( docId( 0 ), routingKey,
					document -> initCompatibleDocument( compatibleIndex, document, 0 ) );
			incompatibleIndexer.add( docId( 0 ), routingKey,
					document -> initIncompatibleDocument( incompatibleIndex, document, 0 ) );
		}

		private void initCompatibleDocument(SimpleMappedIndex<IndexBinding> index, DocumentElement document,
				int docOrdinal) {
			IndexBinding binding = index.binding();
			document.addValue( binding.analyzedStringField.reference,
					analyzedFieldValues.get( docOrdinal ) );
			document.addValue( binding.normalizedStringField.reference,
					normalizedStringFieldValues.get( docOrdinal ) );
			document.addValue( binding.whitespaceAnalyzedField.reference,
					whitespaceAnalyzedFieldValues.get( docOrdinal ) );
			document.addValue( binding.whitespaceLowercaseAnalyzedField.reference,
					whitespaceLowercaseAnalyzedFieldValues.get( docOrdinal ) );
			document.addValue( binding.whitespaceLowercaseSearchAnalyzedField.reference,
					whitespaceLowercaseSearchAnalyzedFieldValues.get( docOrdinal ) );
			document.addValue( binding.ngramSearchAnalyzedField.reference,
					ngramSearchAnalyzedFieldValues.get( docOrdinal ) );
		}

		private void initIncompatibleDocument(SimpleMappedIndex<IncompatibleIndexBinding> index, DocumentElement document,
				int docOrdinal) {
			IncompatibleIndexBinding binding = index.binding();
			document.addValue( binding.analyzedStringField.reference,
					analyzedFieldValues.get( docOrdinal ) );
		}
	}
}
