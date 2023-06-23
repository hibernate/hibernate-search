/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.analysis;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.util.Arrays;
import java.util.List;

import org.hibernate.search.engine.backend.analysis.AnalyzerNames;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.KeywordStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendHelper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SingleFieldDocumentBuilder;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Test indexing and searching with built-in analyzer definitions.
 * See {@link AnalyzerNames}.
 */
@RunWith(Parameterized.class)
public class AnalysisBuiltinIT {

	@Parameterized.Parameters
	public static List<SearchSetupHelper> params() {
		return Arrays.asList(
				// Test with no analysis configurer whatsoever
				new SearchSetupHelper( TckBackendHelper::createAnalysisNotConfiguredBackendSetupStrategy ),
				// Test with an analysis configurer that does not override the defaults but defines other analyzers
				new SearchSetupHelper( TckBackendHelper::createAnalysisCustomBackendSetupStrategy )
		);
	}

	@Rule
	public final SearchSetupHelper setupHelper;

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	public AnalysisBuiltinIT(SearchSetupHelper setupHelper) {
		this.setupHelper = setupHelper;
	}

	@Before
	public void setup() {
		setupHelper.start().withIndex( index ).setup();
	}

	@Test
	public void analyzer_default() {
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
	public void analyzer_standard() {
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
	public void analyzer_simple() {
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
	public void analyzer_whitespace() {
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
	public void analyzer_stop() {
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
	public void analyzer_keyword() {
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
