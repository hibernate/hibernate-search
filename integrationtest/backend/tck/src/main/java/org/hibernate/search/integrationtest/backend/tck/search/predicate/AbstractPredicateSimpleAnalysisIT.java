/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.Test;

/**
 * Integration tests for simple analysis support in predicates,
 * i.e. analysis that cannot be configured by overriding the analyzer.
 */
public abstract class AbstractPredicateSimpleAnalysisIT {

	protected final SimpleMappedIndex<IndexBinding> index;
	protected final SimpleMappedIndex<IndexBinding> compatibleSearchAnalyzerIndex;
	protected final SimpleMappedIndex<IncompatibleIndexBinding> incompatibleSearchAnalyzerIndex;
	protected final DataSet dataSet;

	protected AbstractPredicateSimpleAnalysisIT(SimpleMappedIndex<IndexBinding> index,
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
	void emptyStringBeforeAnalysis() {
		assertThatQuery( index.query()
				.where( f -> predicate( f, index.binding().analyzedStringField.relativeFieldName, "" ) ) )
				.hasNoHits();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2700")
	@PortedFromSearch5(original = "org.hibernate.search.test.dsl.SimpleQueryStringDSLTest.testBlankQueryString")
	void blankStringBeforeAnalysis() {
		assertThatQuery( index.query()
				.where( f -> predicate( f, index.binding().analyzedStringField.relativeFieldName, "   " ) ) )
				.hasNoHits();
	}

	@Test
	void noTokenAfterAnalysis() {
		assertThatQuery( index.query()
				// Use a stopword, which should be removed by the analysis
				.where( f -> predicate( f, index.binding().analyzedStringField.relativeFieldName, "a" ) ) )
				.hasNoHits();
	}

	@Test
	void multiIndex_incompatibleAnalyzer() {
		StubMappingScope scope = index.createScope( incompatibleSearchAnalyzerIndex );
		String absoluteFieldPath = index.binding().analyzedStringField.relativeFieldName;

		assertThatThrownBy( () -> scope.query()
				.where( f -> predicate( f, absoluteFieldPath, "fox" ) )
				.fetchAll() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Inconsistent configuration for field '" + absoluteFieldPath
								+ "' in a search query across multiple indexes",
						"Attribute 'searchAnalyzer", "' differs:", " vs. "
				)
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( index.name(), incompatibleSearchAnalyzerIndex.name() )
				) );
	}

	@Test
	void multiIndex_incompatibleAnalyzer_searchAnalyzer() {
		StubMappingScope scope = index.createScope( compatibleSearchAnalyzerIndex );
		String absoluteFieldPath = index.binding().analyzedStringField.relativeFieldName;

		assertThatQuery( scope.query()
				.where( f -> predicate( f, absoluteFieldPath, "fox" ) ) )
				.hasDocRefHitsAnyOrder( b -> {
					b.doc( index.typeName(), dataSet.docId( 0 ) );
					b.doc( compatibleSearchAnalyzerIndex.typeName(), dataSet.docId( 0 ) );
				} );
	}

	protected abstract PredicateFinalStep predicate(SearchPredicateFactory f, String fieldPath, String matchingParam);

	public static final class IndexBinding {
		final SimpleFieldModel<String> analyzedStringField;
		final SimpleFieldModel<String> normalizedStringField;
		final SimpleFieldModel<String> whitespaceAnalyzedField;
		final SimpleFieldModel<String> whitespaceLowercaseAnalyzedField;
		final SimpleFieldModel<String> whitespaceLowercaseSearchAnalyzedField;
		final SimpleFieldModel<String> ngramSearchAnalyzedField;

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
