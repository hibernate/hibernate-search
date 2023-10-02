/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.backend.analysis.AnalysisToken;
import org.hibernate.search.engine.backend.analysis.AnalyzerDescriptor;
import org.hibernate.search.engine.backend.analysis.AnalyzerNames;
import org.hibernate.search.engine.backend.analysis.NormalizerDescriptor;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.KeywordStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendHelper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendSetupStrategy;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SingleFieldDocumentBuilder;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.extension.parameterized.ParameterizedPerMethod;
import org.hibernate.search.util.impl.test.extension.parameterized.ParameterizedSetup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test indexing and searching with built-in analyzer definitions.
 * See {@link AnalyzerNames}.
 */
@ParameterizedPerMethod
class AnalysisBuiltinIT {

	public static List<? extends Arguments> params() {
		return Arrays.asList(
				// Test with no analysis configurer whatsoever
				Arguments.of( (Function<TckBackendHelper,
						TckBackendSetupStrategy<?>>) TckBackendHelper::createAnalysisNotConfiguredBackendSetupStrategy, false ),
				// Test with an analysis configurer that does not override the defaults but defines other analyzers
				Arguments.of( (Function<TckBackendHelper,
						TckBackendSetupStrategy<?>>) TckBackendHelper::createAnalysisCustomBackendSetupStrategy, true )
		);
	}

	@RegisterExtension
	public static SearchSetupHelper setupHelper = SearchSetupHelper.create();
	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	private boolean hasCustomConfiguration;

	@ParameterizedSetup
	@MethodSource("params")
	public void init(Function<TckBackendHelper, TckBackendSetupStrategy<?>> setupStrategyFunction,
			boolean hasCustomConfiguration) {
		setupHelper.start( setupStrategyFunction ).withIndex( index ).setup();
		this.hasCustomConfiguration = hasCustomConfiguration;
	}

	@Test
	void analyzer_default() {
		SimpleFieldModel<String> field = index.binding().defaultAnalyzer;
		initData( field );

		// Tokenize on space, hyphen
		assertMatchQuery( field, "words" )
				.hasDocRefHitsAnyOrder( index.typeName(), "3", "4", "5" );

		// Case-insensitive
		assertMatchQuery( field, "WORDS" )
				.hasDocRefHitsAnyOrder( index.typeName(), "3", "4", "5" );
		assertMatchQuery( field, "Words" )
				.hasDocRefHitsAnyOrder( index.typeName(), "3", "4", "5" );

		// No stopword removal
		assertMatchQuery( field, "a" )
				.hasDocRefHitsAnyOrder( index.typeName(), "7" );
		assertMatchQuery( field, "the" )
				.hasDocRefHitsAnyOrder( index.typeName(), "7" );
	}

	@Test
	void analyzer_standard() {
		SimpleFieldModel<String> field = index.binding().standardAnalyzer;
		initData( field );

		// Tokenize on space, hyphen
		assertMatchQuery( field, "words" )
				.hasDocRefHitsAnyOrder( index.typeName(), "3", "4", "5" );

		// Case-insensitive
		assertMatchQuery( field, "WORDS" )
				.hasDocRefHitsAnyOrder( index.typeName(), "3", "4", "5" );
		assertMatchQuery( field, "Words" )
				.hasDocRefHitsAnyOrder( index.typeName(), "3", "4", "5" );

		// No stopword removal
		assertMatchQuery( field, "a" )
				.hasDocRefHitsAnyOrder( index.typeName(), "7" );
		assertMatchQuery( field, "the" )
				.hasDocRefHitsAnyOrder( index.typeName(), "7" );
	}

	@Test
	void analyzer_simple() {
		SimpleFieldModel<String> field = index.binding().simpleAnalyzer;
		initData( field );

		// Tokenize on space, hyphen, dot (basically, any character which is not a letter)
		assertMatchQuery( field, "words" )
				.hasDocRefHitsAnyOrder( index.typeName(), "2", "3", "4", "5" );

		// Case-insensitive
		assertMatchQuery( field, "WORDS" )
				.hasDocRefHitsAnyOrder( index.typeName(), "2", "3", "4", "5" );
		assertMatchQuery( field, "Words" )
				.hasDocRefHitsAnyOrder( index.typeName(), "2", "3", "4", "5" );

		// No stopword removal
		assertMatchQuery( field, "a" )
				.hasDocRefHitsAnyOrder( index.typeName(), "7" );
		assertMatchQuery( field, "the" )
				.hasDocRefHitsAnyOrder( index.typeName(), "7" );
	}

	@Test
	void analyzer_whitespace() {
		SimpleFieldModel<String> field = index.binding().whitespaceAnalyzer;
		initData( field );

		// Tokenize on space only && case-sensitive
		assertMatchQuery( field, "words" )
				.hasDocRefHitsAnyOrder( index.typeName(), "4" );

		// Case-sensitive
		assertMatchQuery( field, "WORDS" )
				.hasDocRefHitsAnyOrder( index.typeName(), "5" );
		assertMatchQuery( field, "Words" )
				.hasNoHits();

		// No stopword removal
		assertMatchQuery( field, "a" )
				.hasDocRefHitsAnyOrder( index.typeName(), "7" );
		assertMatchQuery( field, "the" )
				.hasDocRefHitsAnyOrder( index.typeName(), "7" );
	}

	@Test
	void analyzer_stop() {
		SimpleFieldModel<String> field = index.binding().stopAnalyzer;
		initData( field );

		// Tokenize on space, hyphen, dot (basically, any character which is not a letter)
		assertMatchQuery( field, "words" )
				.hasDocRefHitsAnyOrder( index.typeName(), "2", "3", "4", "5" );

		// Case-insensitive
		assertMatchQuery( field, "WORDS" )
				.hasDocRefHitsAnyOrder( index.typeName(), "2", "3", "4", "5" );
		assertMatchQuery( field, "Words" )
				.hasDocRefHitsAnyOrder( index.typeName(), "2", "3", "4", "5" );

		// Stopword removal
		assertMatchQuery( field, "a" )
				.hasNoHits();
		assertMatchQuery( field, "the" )
				.hasNoHits();
	}

	@Test
	void analyzer_keyword() {
		SimpleFieldModel<String> field = index.binding().keywordAnalyzer;
		initData( field );

		// no match for any partial text
		assertMatchQuery( field, "words" ).hasNoHits();
		assertMatchQuery( field, "WORDS" ).hasNoHits();
		assertMatchQuery( field, "Words" ).hasNoHits();
		assertMatchQuery( field, "a" ).hasNoHits();
		assertMatchQuery( field, "the" ).hasNoHits();

		// as a keyword field, it will match only the whole text
		assertMatchQuery( field, "two wôrds" ).hasDocRefHitsAnyOrder( index.typeName(), "6" );
	}

	@Test
	void indexAnalyzerDescriptor() {
		Collection<? extends AnalyzerDescriptor> analyzerDescriptors = index.toApi().descriptor().analyzers();
		if ( TckConfiguration.get().getBackendFeatures().hasBuiltInAnalyzerDescriptorsAvailable() ) {
			assertThat( analyzerDescriptors )
					.isNotEmpty()
					.extracting( AnalyzerDescriptor::name )
					.contains(
							AnalyzerNames.DEFAULT,
							AnalyzerNames.STOP,
							AnalyzerNames.KEYWORD,
							AnalyzerNames.WHITESPACE,
							AnalyzerNames.SIMPLE,
							AnalyzerNames.STANDARD
					);
		}
		else if ( !hasCustomConfiguration ) {
			assertThat( analyzerDescriptors ).isEmpty();
		}
		// means we are testing with a second setup strategy that adds non-builtin analyzers:
		if ( hasCustomConfiguration ) {
			assertThat( analyzerDescriptors )
					.extracting( AnalyzerDescriptor::name )
					.contains(
							AnalysisCustomIT.AnalysisDefinitions.ANALYZER_NOOP.name,
							AnalysisCustomIT.AnalysisDefinitions.ANALYZER_WHITESPACE_LOWERCASE.name,
							AnalysisCustomIT.AnalysisDefinitions.ANALYZER_PATTERNS_STOPWORD.name
					);
		}

		for ( AnalyzerDescriptor descriptor : analyzerDescriptors ) {
			assertThat( index.toApi().descriptor().analyzer( descriptor.name() ) )
					.isPresent()
					.get()
					.isEqualTo( descriptor );
		}
	}

	@Test
	void indexNormalizerDescriptor() {
		Collection<? extends NormalizerDescriptor> normalizerDescriptors = index.toApi().descriptor().normalizers();

		// means we are testing with a second setup strategy that adds some normalizers:
		if ( hasCustomConfiguration ) {
			assertThat( normalizerDescriptors )
					.extracting( NormalizerDescriptor::name )
					.contains(
							AnalysisCustomIT.AnalysisDefinitions.NORMALIZER_LOWERCASE.name,
							AnalysisCustomIT.AnalysisDefinitions.NORMALIZER_NOOP.name,
							AnalysisCustomIT.AnalysisDefinitions.NORMALIZER_PATTERN_REPLACING.name
					);
		}
		else {
			assertThat( normalizerDescriptors ).isEmpty();
		}

		for ( NormalizerDescriptor descriptor : normalizerDescriptors ) {
			assertThat( index.toApi().descriptor().normalizer( descriptor.name() ) )
					.isPresent()
					.get()
					.isEqualTo( descriptor );
		}
	}

	@Test
	void applyAnalyzer_noop() {
		assumeTrue( hasCustomConfiguration,
				"Test only make sense if we know for sure that there are configured analyzers" );
		IndexManager indexManager = index.toApi();

		List<? extends AnalysisToken> tokens = indexManager.analyze(
				AnalysisCustomIT.AnalysisDefinitions.ANALYZER_NOOP.name,
				"The quick brown fox jumps over the lazy dog."
		);

		assertThat( tokens )
				.hasSize( 1 )
				.element( 0 )
				.satisfies( analysisToken -> {
					assertThat( analysisToken.term() ).isEqualTo( "The quick brown fox jumps over the lazy dog." );
					assertThat( analysisToken.type() ).containsIgnoringCase( "word" );
					assertThat( analysisToken.startOffset() ).isEqualTo( 0 );
					assertThat( analysisToken.endOffset() ).isEqualTo( 44 );
				} );

		tokens = indexManager.analyze(
				AnalysisCustomIT.AnalysisDefinitions.ANALYZER_WHITESPACE_LOWERCASE.name,
				"The quick brown fox jumps over the lazy dog."
		);

		assertThat( tokens ).hasSize( 9 );
		assertThat( tokens ).extracting( AnalysisToken::term )
				.containsExactly( "the", "quick", "brown", "fox", "jumps", "over", "the", "lazy", "dog." );
		assertThat( tokens ).extracting( AnalysisToken::startOffset )
				.containsExactly( 0, 4, 10, 16, 20, 26, 31, 35, 40 );
		assertThat( tokens ).extracting( AnalysisToken::endOffset )
				.containsExactly( 3, 9, 15, 19, 25, 30, 34, 39, 44 );
		assertThat( tokens ).extracting( AnalysisToken::type )
				.allSatisfy( token -> assertThat( token ).containsIgnoringCase( "word" ) );
	}

	@Test
	void applyAnalyzer_lowercase() {
		assumeTrue( hasCustomConfiguration,
				"Test only make sense if we know for sure that there are configured analyzers" );
		IndexManager indexManager = index.toApi();

		List<? extends AnalysisToken> tokens = indexManager.analyze(
				AnalysisCustomIT.AnalysisDefinitions.ANALYZER_WHITESPACE_LOWERCASE.name,
				"The quick brown fox jumps over the lazy dog."
		);

		assertThat( tokens ).hasSize( 9 );
		assertThat( tokens ).extracting( AnalysisToken::term )
				.containsExactly( "the", "quick", "brown", "fox", "jumps", "over", "the", "lazy", "dog." );
		assertThat( tokens ).extracting( AnalysisToken::startOffset )
				.containsExactly( 0, 4, 10, 16, 20, 26, 31, 35, 40 );
		assertThat( tokens ).extracting( AnalysisToken::endOffset )
				.containsExactly( 3, 9, 15, 19, 25, 30, 34, 39, 44 );
		assertThat( tokens ).extracting( AnalysisToken::type )
				.allSatisfy( token -> assertThat( token ).containsIgnoringCase( "word" ) );
	}

	@Test
	void applyAnalyzer_noSuchAnalyzer() {
		IndexManager indexManager = index.toApi();

		assertThatThrownBy( () -> indexManager.analyze( "there-is-no-such-analyzer",
				"The quick brown fox jumps over the lazy dog." ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "there-is-no-such-analyzer" );
	}

	@Test
	void applyNormalizer_noop() {
		assumeTrue( hasCustomConfiguration,
				"Test only make sense if we know for sure that there are configured analyzers" );
		IndexManager indexManager = index.toApi();

		AnalysisToken token = indexManager.normalize(
				AnalysisCustomIT.AnalysisDefinitions.NORMALIZER_NOOP.name,
				"The quick brown FOX jumps over The Lazy DOG."
		);

		assertThat( token.term() ).isEqualTo( "The quick brown FOX jumps over The Lazy DOG." );
	}

	@Test
	void applyNormalizer_lowercase() {
		assumeTrue( hasCustomConfiguration,
				"Test only make sense if we know for sure that there are configured analyzers" );
		IndexManager indexManager = index.toApi();

		AnalysisToken token = indexManager.normalize(
				AnalysisCustomIT.AnalysisDefinitions.NORMALIZER_LOWERCASE.name,
				"The quick brown FOX jumps over The Lazy DOG."
		);

		assertThat( token.term() ).isEqualTo( "the quick brown fox jumps over the lazy dog." );
	}

	@Test
	void applyNormalizer_noSuchAnalyzer() {
		IndexManager indexManager = index.toApi();

		assertThatThrownBy( () -> indexManager.normalize( "there-is-no-such-normalizer",
				"The quick brown fox jumps over the lazy dog." ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "there-is-no-such-normalizer" );
	}


	private SearchResultAssert<DocumentReference> assertMatchQuery(SimpleFieldModel<String> fieldModel, String valueToMatch) {
		StubMappingScope scope = index.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.match().field( fieldModel.relativeFieldName ).matching( valueToMatch ) )
				.toQuery();

		return assertThatQuery( query );
	}

	private void initData(SimpleFieldModel<String> fieldModel) {
		index.bulkIndexer()
				.add( fieldModel.reference, AnalysisBuiltinIT::buildDocuments )
				.join();
	}

	private static void buildDocuments(SingleFieldDocumentBuilder<String> builder) {
		builder.emptyDocument( "empty" );
		builder.document( "1", "twowords" );
		builder.document( "2", "two.words" );
		builder.document( "3", "two-words" );
		builder.document( "4", "two words" );
		builder.document( "5", "TWO WORDS" );
		builder.document( "6", "two wôrds" );
		builder.document( "7", "a stopword the stopword" );
	}

	private static class IndexBinding {
		final SimpleFieldModel<String> defaultAnalyzer;
		final SimpleFieldModel<String> standardAnalyzer;
		final SimpleFieldModel<String> simpleAnalyzer;
		final SimpleFieldModel<String> whitespaceAnalyzer;
		final SimpleFieldModel<String> stopAnalyzer;
		final SimpleFieldModel<String> keywordAnalyzer;

		IndexBinding(IndexSchemaElement root) {
			this.defaultAnalyzer = SimpleFieldModel.mapperWithOverride( KeywordStringFieldTypeDescriptor.INSTANCE,
					f -> f.asString().analyzer( AnalyzerNames.DEFAULT ) )
					.map( root, "default" );
			this.standardAnalyzer = SimpleFieldModel.mapperWithOverride( KeywordStringFieldTypeDescriptor.INSTANCE,
					f -> f.asString().analyzer( AnalyzerNames.STANDARD ) )
					.map( root, "standard" );
			this.simpleAnalyzer = SimpleFieldModel.mapperWithOverride( KeywordStringFieldTypeDescriptor.INSTANCE,
					f -> f.asString().analyzer( AnalyzerNames.SIMPLE ) )
					.map( root, "simple" );
			this.whitespaceAnalyzer = SimpleFieldModel.mapperWithOverride( KeywordStringFieldTypeDescriptor.INSTANCE,
					f -> f.asString().analyzer( AnalyzerNames.WHITESPACE ) )
					.map( root, "whitespace" );
			this.stopAnalyzer = SimpleFieldModel.mapperWithOverride( KeywordStringFieldTypeDescriptor.INSTANCE,
					f -> f.asString().analyzer( AnalyzerNames.STOP ) )
					.map( root, "stop" );
			this.keywordAnalyzer = SimpleFieldModel.mapperWithOverride( KeywordStringFieldTypeDescriptor.INSTANCE,
					f -> f.asString().analyzer( AnalyzerNames.KEYWORD ) )
					.map( root, "keyword" );
		}
	}
}
