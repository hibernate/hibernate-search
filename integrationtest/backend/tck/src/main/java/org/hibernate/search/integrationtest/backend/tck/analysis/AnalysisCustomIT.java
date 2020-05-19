/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.analysis;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFinalStep;
import org.hibernate.search.engine.backend.types.dsl.StringIndexFieldTypeOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendHelper;
import org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.engine.backend.common.DocumentReference;
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

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper( TckBackendHelper::createAnalysisCustomBackendSetupStrategy );

	private SimpleMappedIndex<IndexBinding> index;

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
				.hasDocRefHitsAnyOrder( index.typeName(), "1" );
		assertMatchQuery( "WORD" )
				.hasDocRefHitsAnyOrder( index.typeName(), "2" );
		assertMatchQuery( "wôrd" )
				.hasDocRefHitsAnyOrder( index.typeName(), "3" );
		assertMatchQuery( "word word" )
				.hasDocRefHitsAnyOrder( index.typeName(), "4" );
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
				.hasDocRefHitsAnyOrder( index.typeName(), "1", "2" );
		assertMatchQuery( "WORD" )
				.hasDocRefHitsAnyOrder( index.typeName(), "1", "2" );
		assertMatchQuery( "Word" )
				.hasDocRefHitsAnyOrder( index.typeName(), "1", "2" );
		assertMatchQuery( "WÔRD" )
				.hasDocRefHitsAnyOrder( index.typeName(), "3" );
		assertMatchQuery( "WorD Word" )
				.hasDocRefHitsAnyOrder( index.typeName(), "4" );
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
				.hasDocRefHitsAnyOrder( index.typeName(), "1" );
		assertMatchQuery( "WORD" )
				.hasDocRefHitsAnyOrder( index.typeName(), "2" );
		assertMatchQuery( "wôrd" )
				.hasDocRefHitsAnyOrder( index.typeName(), "3" );
		assertMatchQuery( "word word" )
				.hasDocRefHitsAnyOrder( index.typeName(), "4", "5" );
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
				.hasDocRefHitsAnyOrder( index.typeName(), "1" );
		assertMatchQuery( "WORD" )
				.hasDocRefHitsAnyOrder( index.typeName(), "2" );
		assertMatchQuery( "wôrd" )
				.hasDocRefHitsAnyOrder( index.typeName(), "3" );
		assertMatchQuery( "word word" )
				.hasDocRefHitsAnyOrder( index.typeName(), "4" );
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
				.hasDocRefHitsAnyOrder( index.typeName(), "1", "2", "4", "5" );
		assertMatchQuery( "WORD" )
				.hasDocRefHitsAnyOrder( index.typeName(), "1", "2", "4", "5" );
		assertMatchQuery( "Word" )
				.hasDocRefHitsAnyOrder( index.typeName(), "1", "2", "4", "5" );
		assertMatchQuery( "WÔRD" )
				.hasDocRefHitsAnyOrder( index.typeName(), "3" );
		assertMatchQuery( "WorD Word" )
				.hasDocRefHitsAnyOrder( index.typeName(), "1", "2", "4", "5" );
		assertMatchQuery( "word wôrd" )
				.hasDocRefHitsAnyOrder( index.typeName(), "1", "2", "3", "4", "5" );
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
				.hasDocRefHitsAnyOrder( index.typeName(), "1", "2", "4" );
		assertMatchQuery( "word2" )
				.hasDocRefHitsAnyOrder( index.typeName(), "1", "3", "5" );
		assertMatchQuery( "word3" )
				.hasDocRefHitsAnyOrder( index.typeName(), "1", "3" );
		assertMatchQuery( "stopword" )
				.hasNoHits();
		assertMatchQuery( "word1 stopword" )
				.hasDocRefHitsAnyOrder( index.typeName(), "1", "2", "4" );
		assertMatchQuery( "word1,word2" )
				.hasDocRefHitsAnyOrder( index.typeName(), "1", "2", "3", "4", "5" );
	}

	private SearchResultAssert<DocumentReference> assertMatchQuery(String valueToMatch) {
		StubMappingScope scope = index.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.match().field( index.binding().field.relativeFieldName ).matching( valueToMatch ) )
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
			Function<StringIndexFieldTypeOptionsStep<?>, IndexFieldTypeFinalStep<String>> typeContributor) {
		index = SimpleMappedIndex.of( ctx -> new IndexBinding( ctx, fieldName, typeContributor ) );
		setupHelper.start().withIndex( index ).setup();
	}

	private void initData(Consumer<AnalysisITDocumentBuilder> valueContributor) {
		BulkIndexer indexer = index.bulkIndexer();
		List<String> documentIds = new ArrayList<>();
		valueContributor.accept(
				(String documentId, String ... fieldValues) -> {
					documentIds.add( documentId );
					indexer.add( documentId, document -> {
						for ( String fieldValue : fieldValues ) {
							index.binding().field.write( document, fieldValue );
						}
					} );
				}
		);
		indexer.join();
	}

	interface AnalysisITDocumentBuilder {
		void document(String documentId, String ... fieldValues);
	}

	private static class IndexBinding {
		final MainFieldModel field;

		IndexBinding(IndexSchemaElement root, String fieldName,
				Function<StringIndexFieldTypeOptionsStep<?>, IndexFieldTypeFinalStep<String>> typeContributor) {
			IndexFieldReference<String> reference = root.field(
					fieldName,
					f -> typeContributor.apply( f.asString() )
			)
					.toReference();
			this.field = new MainFieldModel( reference, fieldName );
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
