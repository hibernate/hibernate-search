/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.analysis;

import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.StringIndexSchemaFieldTypedContext;
import org.hibernate.search.engine.backend.index.spi.IndexSearchTarget;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.assertion.DocumentReferencesSearchResultAssert;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubSessionContext;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test indexing and searching with custom analyzer definitions.
 * <p>
 * Backend testing modules are expected to add the definitions
 * listed in {@link AnalysisDefinitions}.
 */
public class AnalysisCustomIT {

	public static final String CONFIGURATION_ID = "analysis-custom";

	/**
	 * The analysis definitions this test expects will be available when defining the schema.
	 * <p>
	 * Access {@link #name} to get the expected name .
	 * See the javadoc for a description of what is expected in each definition.
	 */
	public enum AnalysisDefinitions {
		/**
		 * No-op normalizer.
		 */
		NORMALIZER_NOOP("normalizer_noop"),
		/**
		 * Normalizer with a lowercase token filter.
		 */
		NORMALIZER_LOWERCASE("normalizer_lowercase"),
		/**
		 * No-op analyzer.
		 */
		ANALYZER_NOOP("analyzer_noop"),
		/**
		 * Analyzer with a tokenizer on whitespaces and a lowercase token filter.
		 */
		ANALYZER_WHITESPACE_LOWERCASE("analyzer_lowercase"),
		/**
		 * Analyzer with:
		 * <ul>
		 *     <li>A pattern-replacing char filter replacing "\s+" with ","</li>
		 *     <li>A pattern tokenizer on pattern ","</li>
		 *     <li>A stopword token filter removing the stopword "stopword"</li>
		 * </ul>
		 */
		ANALYZER_PATTERNS_STOPWORD("analyzer_patterns_stopword")
		;

		public final String name;

		AnalysisDefinitions(String name) {
			this.name = "AnalysisCustomIT_" + name;
		}
	}

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private IndexMapping indexMapping;
	private MappedIndexManager<?> indexManager;
	private SessionContext sessionContext = new StubSessionContext();

	@Test
	public void normalizer_keyword() {
		setupWithNormalizer( AnalysisDefinitions.NORMALIZER_NOOP );
		initData( b -> {
			b.document( "empty" );
			b.document( "1", "word" );
			b.document( "2", "WORD" );
			b.document( "3", "wôrd" );
			b.document( "4", "word word" );
			b.document( "5", "word\tword" );
		} );

		// We only expect exact matches
		assertMatchQuery( "word" )
				.hasReferencesHitsAnyOrder( INDEX_NAME, "1" );
		assertMatchQuery( "WORD" )
				.hasReferencesHitsAnyOrder( INDEX_NAME, "2" );
		assertMatchQuery( "wôrd" )
				.hasReferencesHitsAnyOrder( INDEX_NAME, "3" );
		assertMatchQuery( "word word" )
				.hasReferencesHitsAnyOrder( INDEX_NAME, "4" );
	}

	@Test
	public void normalizer_lowercase() {
		setupWithNormalizer( AnalysisDefinitions.NORMALIZER_LOWERCASE );
		initData( b -> {
			b.document( "empty" );
			b.document( "1", "word" );
			b.document( "2", "WORD" );
			b.document( "3", "wôrd" );
			b.document( "4", "word word" );
			b.document( "5", "word\tword" );
		} );

		// We expect case-insensitive matches
		assertMatchQuery( "word" )
				.hasReferencesHitsAnyOrder( INDEX_NAME, "1", "2" );
		assertMatchQuery( "WORD" )
				.hasReferencesHitsAnyOrder( INDEX_NAME, "1", "2" );
		assertMatchQuery( "Word" )
				.hasReferencesHitsAnyOrder( INDEX_NAME, "1", "2" );
		assertMatchQuery( "WÔRD" )
				.hasReferencesHitsAnyOrder( INDEX_NAME, "3" );
		assertMatchQuery( "WorD Word" )
				.hasReferencesHitsAnyOrder( INDEX_NAME, "4" );
	}

	@Test
	public void analyzer_keyword() {
		setupWithAnalyzer( AnalysisDefinitions.ANALYZER_NOOP );
		initData( b -> {
			b.document( "empty" );
			b.document( "1", "word" );
			b.document( "2", "WORD" );
			b.document( "3", "wôrd" );
			b.document( "4", "word word" );
			b.document( "5", "word\tword" );
		} );

		// We only expect exact matches
		assertMatchQuery( "word" )
				.hasReferencesHitsAnyOrder( INDEX_NAME, "1" );
		assertMatchQuery( "WORD" )
				.hasReferencesHitsAnyOrder( INDEX_NAME, "2" );
		assertMatchQuery( "wôrd" )
				.hasReferencesHitsAnyOrder( INDEX_NAME, "3" );
		assertMatchQuery( "word word" )
				.hasReferencesHitsAnyOrder( INDEX_NAME, "4" );
	}

	@Test
	public void analyzer_whitespace_lowercase() {
		setupWithAnalyzer( AnalysisDefinitions.ANALYZER_WHITESPACE_LOWERCASE );
		initData( b -> {
			b.document( "empty" );
			b.document( "1", "word" );
			b.document( "2", "WORD" );
			b.document( "3", "wôrd" );
			b.document( "4", "word word" );
			b.document( "5", "word\tword" );
		} );

		// We expect case-insensitive, word-sensitive matches
		assertMatchQuery( "word" )
				.hasReferencesHitsAnyOrder( INDEX_NAME, "1", "2", "4", "5" );
		assertMatchQuery( "WORD" )
				.hasReferencesHitsAnyOrder( INDEX_NAME, "1", "2", "4", "5" );
		assertMatchQuery( "Word" )
				.hasReferencesHitsAnyOrder( INDEX_NAME, "1", "2", "4", "5" );
		assertMatchQuery( "WÔRD" )
				.hasReferencesHitsAnyOrder( INDEX_NAME, "3" );
		assertMatchQuery( "WorD Word" )
				.hasReferencesHitsAnyOrder( INDEX_NAME, "1", "2", "4", "5" );
		assertMatchQuery( "word wôrd" )
				.hasReferencesHitsAnyOrder( INDEX_NAME, "1", "2", "3", "4", "5" );
	}

	@Test
	public void analyzer_patterns_stopword() {
		setupWithAnalyzer( AnalysisDefinitions.ANALYZER_PATTERNS_STOPWORD );
		initData( b -> {
			b.document( "empty" );
			b.document( "1", "word1,word2 word3" );
			b.document( "2", "word1 stopword" );
			b.document( "3", "word2,word3" );
			b.document( "4", "word1\tstopword" );
			b.document( "5", "word2,stopword" );
		} );

		assertMatchQuery( "word1" )
				.hasReferencesHitsAnyOrder( INDEX_NAME, "1", "2", "4" );
		assertMatchQuery( "word2" )
				.hasReferencesHitsAnyOrder( INDEX_NAME, "1", "3", "5" );
		assertMatchQuery( "word3" )
				.hasReferencesHitsAnyOrder( INDEX_NAME, "1", "3" );
		assertMatchQuery( "stopword" )
				.hasNoHits();
		assertMatchQuery( "word1 stopword" )
				.hasReferencesHitsAnyOrder( INDEX_NAME, "1", "2", "4" );
		assertMatchQuery( "word1,word2" )
				.hasReferencesHitsAnyOrder( INDEX_NAME, "1", "2", "3", "4", "5" );
	}

	private DocumentReferencesSearchResultAssert<DocumentReference> assertMatchQuery(String valueToMatch) {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( root -> root.match().onField( indexMapping.field.relativeFieldName ).matching( valueToMatch ) )
				.build();

		return DocumentReferencesSearchResultAssert.assertThat( query );
	}

	private void setupWithAnalyzer(AnalysisDefinitions analysisDefinition) {
		setup( "fieldWithAnalyzer", c -> c.analyzer( analysisDefinition.name ).createAccessor() );
	}

	private void setupWithNormalizer(AnalysisDefinitions analysisDefinition) {
		setup( "fieldWithNormalizer", c -> c.normalizer( analysisDefinition.name ).createAccessor() );
	}

	private void setup(String fieldName, Function<StringIndexSchemaFieldTypedContext<?>, IndexFieldAccessor<String>> fieldMapping) {
		setupHelper.withConfiguration( CONFIGURATION_ID )
				.withIndex(
						"MappedType", INDEX_NAME,
						ctx -> {
							IndexFieldAccessor<String> accessor =
									fieldMapping.apply( ctx.getSchemaElement().field( fieldName ).asString() );
							MainFieldModel fieldModel = new MainFieldModel( accessor, fieldName );
							this.indexMapping = new IndexMapping( fieldModel );
						},
						indexManager -> this.indexManager = indexManager
				)
				.setup();
	}

	private void initData(Consumer<AnalysisITDocumentBuilder> valueContributor) {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan( sessionContext );
		List<String> documentIds = new ArrayList<>();
		valueContributor.accept(
				(String documentId, String ... fieldValues) -> {
					documentIds.add( documentId );
					workPlan.add( referenceProvider( documentId ), document -> {
						for ( String fieldValue : fieldValues ) {
							indexMapping.field.write( document, fieldValue );
						}
					} );
				}
		);
		workPlan.execute().join();

		// Check that all documents are searchable
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();
		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( root -> root.matchAll() )
				.build();
		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( c -> {
					for ( String documentId : documentIds ) {
						c.doc( INDEX_NAME, documentId );
					}
				} );
	}

	interface AnalysisITDocumentBuilder {
		void document(String documentId, String ... fieldValues);
	}

	private static class IndexMapping {
		final MainFieldModel field;

		IndexMapping(MainFieldModel field) {
			this.field = field;
		}
	}

	private static class MainFieldModel {
		private final IndexFieldAccessor<String> accessor;
		final String relativeFieldName;

		private MainFieldModel(IndexFieldAccessor<String> accessor, String relativeFieldName) {
			this.accessor = accessor;
			this.relativeFieldName = relativeFieldName;
		}

		public void write(DocumentElement target, String value) {
			accessor.write( target, value );
		}
	}
}
