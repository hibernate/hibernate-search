/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.analysis;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeTerminalContext;
import org.hibernate.search.engine.backend.types.dsl.StringIndexFieldTypeContext;
import org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingSearchScope;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;

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
		 * Normalizer with a pattern-replacing char filter replacing "\s+" with ",".
		 */
		NORMALIZER_PATTERN_REPLACING("normalizer_pattern_replacing"),
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
	private StubMappingIndexManager indexManager;

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
				.hasDocRefHitsAnyOrder( INDEX_NAME, "1" );
		assertMatchQuery( "WORD" )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "2" );
		assertMatchQuery( "wôrd" )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "3" );
		assertMatchQuery( "word word" )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "4" );
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
				.hasDocRefHitsAnyOrder( INDEX_NAME, "1", "2" );
		assertMatchQuery( "WORD" )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "1", "2" );
		assertMatchQuery( "Word" )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "1", "2" );
		assertMatchQuery( "WÔRD" )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "3" );
		assertMatchQuery( "WorD Word" )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "4" );
	}

	@Test
	public void normalizer_pattern_replacing() {
		setupWithNormalizer( AnalysisDefinitions.NORMALIZER_PATTERN_REPLACING );
		initData( b -> {
			b.document( "empty" );
			b.document( "1", "word" );
			b.document( "2", "WORD" );
			b.document( "3", "wôrd" );
			b.document( "4", "word word" );
			b.document( "5", "word,word" );
		} );

		// We expect any ',' will be treated as a space character
		assertMatchQuery( "word" )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "1" );
		assertMatchQuery( "WORD" )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "2" );
		assertMatchQuery( "wôrd" )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "3" );
		assertMatchQuery( "word word" )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "4", "5" );
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
				.hasDocRefHitsAnyOrder( INDEX_NAME, "1" );
		assertMatchQuery( "WORD" )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "2" );
		assertMatchQuery( "wôrd" )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "3" );
		assertMatchQuery( "word word" )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "4" );
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
				.hasDocRefHitsAnyOrder( INDEX_NAME, "1", "2", "4", "5" );
		assertMatchQuery( "WORD" )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "1", "2", "4", "5" );
		assertMatchQuery( "Word" )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "1", "2", "4", "5" );
		assertMatchQuery( "WÔRD" )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "3" );
		assertMatchQuery( "WorD Word" )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "1", "2", "4", "5" );
		assertMatchQuery( "word wôrd" )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "1", "2", "3", "4", "5" );
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
				.hasDocRefHitsAnyOrder( INDEX_NAME, "1", "2", "4" );
		assertMatchQuery( "word2" )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "1", "3", "5" );
		assertMatchQuery( "word3" )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "1", "3" );
		assertMatchQuery( "stopword" )
				.hasNoHits();
		assertMatchQuery( "word1 stopword" )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "1", "2", "4" );
		assertMatchQuery( "word1,word2" )
				.hasDocRefHitsAnyOrder( INDEX_NAME, "1", "2", "3", "4", "5" );
	}

	private SearchResultAssert<DocumentReference> assertMatchQuery(String valueToMatch) {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		SearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.match().onField( indexMapping.field.relativeFieldName ).matching( valueToMatch ) )
				.toQuery();

		return assertThat( query );
	}

	private void setupWithAnalyzer(AnalysisDefinitions analysisDefinition) {
		setup( "fieldWithAnalyzer", c -> c.analyzer( analysisDefinition.name ) );
	}

	private void setupWithNormalizer(AnalysisDefinitions analysisDefinition) {
		setup( "fieldWithNormalizer", c -> c.normalizer( analysisDefinition.name ) );
	}

	private void setup(String fieldName,
			Function<StringIndexFieldTypeContext<?>, IndexFieldTypeTerminalContext<String>> typeContributor) {
		setupHelper.withConfiguration( CONFIGURATION_ID )
				.withIndex(
						INDEX_NAME,
						ctx -> {
							IndexFieldReference<String> reference = ctx.getSchemaElement().field(
									fieldName,
									f -> typeContributor.apply( f.asString() )
							)
									.toReference();
							MainFieldModel fieldModel = new MainFieldModel( reference, fieldName );
							this.indexMapping = new IndexMapping( fieldModel );
						},
						indexManager -> this.indexManager = indexManager
				)
				.setup();
	}

	private void initData(Consumer<AnalysisITDocumentBuilder> valueContributor) {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
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
		StubMappingSearchScope scope = indexManager.createSearchScope();
		SearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( c -> {
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
		private final IndexFieldReference<String> reference;
		final String relativeFieldName;

		private MainFieldModel(IndexFieldReference<String> reference, String relativeFieldName) {
			this.reference = reference;
			this.relativeFieldName = relativeFieldName;
		}

		public void write(DocumentElement target, String value) {
			target.addValue( reference, value );
		}
	}
}
