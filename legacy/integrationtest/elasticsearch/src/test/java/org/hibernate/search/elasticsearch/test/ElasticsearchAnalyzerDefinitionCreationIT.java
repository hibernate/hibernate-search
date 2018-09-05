/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import static org.hibernate.search.test.util.JsonHelper.assertJsonEquals;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.apache.lucene.analysis.charfilter.HTMLStripCharFilterFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterFilterFactory;
import org.apache.lucene.analysis.standard.ClassicTokenizerFactory;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.AnalyzerDefs;
import org.hibernate.search.annotations.CharFilterDef;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Parameter;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.TokenizerDef;
import org.hibernate.search.elasticsearch.analyzer.ElasticsearchCharFilterFactory;
import org.hibernate.search.elasticsearch.analyzer.ElasticsearchTokenFilterFactory;
import org.hibernate.search.elasticsearch.analyzer.ElasticsearchTokenizerFactory;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.elasticsearch.cfg.IndexSchemaManagementStrategy;
import org.hibernate.search.elasticsearch.impl.ElasticsearchIndexManager;
import org.hibernate.search.elasticsearch.testutil.TestElasticsearchClient;
import org.hibernate.search.test.SearchInitializationTestBase;
import org.hibernate.search.test.util.ImmutableTestConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests for {@link ElasticsearchIndexManager}'s analyzer definition creation feature.
 *
 * @author Yoann Rodiere
 */
@RunWith(Parameterized.class)
public class ElasticsearchAnalyzerDefinitionCreationIT extends SearchInitializationTestBase {

	@Parameters(name = "With strategy {0}")
	public static EnumSet<IndexSchemaManagementStrategy> strategies() {
		return EnumSet.complementOf( EnumSet.of(
				// Those strategies don't create the schema, so we don't test those
				IndexSchemaManagementStrategy.NONE, IndexSchemaManagementStrategy.VALIDATE
				) );
	}

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public TestElasticsearchClient elasticSearchClient = new TestElasticsearchClient();

	private final IndexSchemaManagementStrategy strategy;

	public ElasticsearchAnalyzerDefinitionCreationIT(IndexSchemaManagementStrategy strategy) {
		super();
		this.strategy = strategy;
	}

	@Override
	protected void init(Class<?>... annotatedClasses) {
		Map<String, Object> settings = new HashMap<>();
		settings.put(
				"hibernate.search.default." + ElasticsearchEnvironment.INDEX_SCHEMA_MANAGEMENT_STRATEGY,
				strategy.getExternalName()
		);
		init( new ImmutableTestConfiguration( settings, annotatedClasses ) );
	}

	@Test
	public void success_simple() throws Exception {
		elasticSearchClient.index( SimpleAnalyzedEntity.class )
				.ensureDoesNotExist().registerForCleanup();

		init( SimpleAnalyzedEntity.class );

		assertJsonEquals(
				"{"
					+ "'analyzer': {"
							+ "'analyzerWithSimpleComponents': {"
									+ "'char_filter': ['html_strip'],"
									+ "'tokenizer': 'whitespace',"
									+ "'filter': ['lowercase']"
							+ "},"
							+ "'analyzerWithNamedSimpleComponents': {"
									+ "'char_filter': ['namedCharFilter'],"
									+ "'tokenizer': 'namedTokenizer',"
									+ "'filter': ['namedTokenFilter']"
							+ "},"
							+ "'analyzerWithComplexComponents': {"
									+ "'char_filter': ['analyzerWithComplexComponents_HTMLStripCharFilterFactory'],"
									+ "'tokenizer': 'classic',"
									+ "'filter': ['analyzerWithComplexComponents_WordDelimiterFilterFactory']"
							+ "},"
							+ "'analyzerWithNamedComplexComponents': {"
									+ "'char_filter': ['custom-html-stripper'],"
									+ "'tokenizer': 'custom-classic-tokenizer',"
									+ "'filter': ['custom-word-delimiter']"
							+ "},"
							+ "'analyzerWithElasticsearchFactories': {"
									+ "'char_filter': ['custom-pattern-replace'],"
									+ "'tokenizer': 'custom-edgeNGram',"
									+ "'filter': ['custom-keep-types']"
							+ "}"
					+ "},"
					+ "'char_filter': {"
							+ "'namedCharFilter': {"
									+ "'type': 'html_strip'"
							+ "},"
							+ "'analyzerWithComplexComponents_HTMLStripCharFilterFactory': {"
									+ "'type': 'html_strip',"
									+ "'escaped_tags': ['br', 'p']"
							+ "},"
							+ "'custom-html-stripper': {"
									+ "'type': 'html_strip',"
									+ "'escaped_tags': ['br', 'p']"
							+ "},"
							+ "'custom-pattern-replace': {"
									+ "'type': 'pattern_replace',"
									+ "'pattern': '[^0-9]',"
									+ "'replacement': '0',"
									+ "'tags': 'CASE_INSENSITIVE|COMMENTS'"
							+ "}"
					+ "},"
					+ "'tokenizer': {"
							+ "'namedTokenizer': {"
									+ "'type': 'whitespace'"
							+ "},"
							+ "'custom-classic-tokenizer': {"
									+ "'type': 'classic'"
							+ "},"
							+ "'custom-edgeNGram': {"
									+ "'type': 'edgeNGram',"
									/*
									 * Strangely enough, even if you send properly typed numbers
									 * to Elasticsearch, when you ask for the current settings it
									 * will spit back strings instead of numbers...
									 */
									+ "'min_gram': '1',"
									+ "'max_gram': '10'"
							+ "}"
					+ "},"
					+ "'filter': {"
							+ "'namedTokenFilter': {"
									+ "'type': 'lowercase'"
							+ "},"
							+ "'analyzerWithComplexComponents_WordDelimiterFilterFactory': {"
									+ "'type': 'word_delimiter',"
									+ "'generate_word_parts': 'true',"
									+ "'generate_number_parts': 'true',"
									+ "'catenate_words': 'false',"
									+ "'catenate_numbers': 'false',"
									+ "'catenate_all': 'false',"
									+ "'split_on_case_change': 'false',"
									+ "'split_on_numerics': 'false',"
									+ "'preserve_original': 'true'"
							+ "},"
							+ "'custom-word-delimiter': {"
									+ "'type': 'word_delimiter',"
									+ "'generate_word_parts': 'true',"
									+ "'generate_number_parts': 'true',"
									+ "'catenate_words': 'false',"
									+ "'catenate_numbers': 'false',"
									+ "'catenate_all': 'false',"
									+ "'split_on_case_change': 'false',"
									+ "'split_on_numerics': 'false',"
									+ "'preserve_original': 'true'"
							+ "},"
							+ "'custom-keep-types': {"
									+ "'type': 'keep_types',"
									+ "'types': ['<NUM>', '<DOUBLE>']"
							+ "}"
					+ "}"
				+ "}",
				elasticSearchClient.index( SimpleAnalyzedEntity.class ).settings( "index.analysis" ).get()
				);
	}

	@Indexed
	@Entity
	@AnalyzerDefs({
			@AnalyzerDef(
					name = "analyzerWithSimpleComponents",
					charFilters = @CharFilterDef(factory = HTMLStripCharFilterFactory.class),
					tokenizer = @TokenizerDef(factory = WhitespaceTokenizerFactory.class),
					filters = @TokenFilterDef(factory = LowerCaseFilterFactory.class)
			),
			@AnalyzerDef(
					name = "analyzerWithNamedSimpleComponents",
					charFilters = @CharFilterDef(name = "namedCharFilter", factory = HTMLStripCharFilterFactory.class),
					tokenizer = @TokenizerDef(name = "namedTokenizer", factory = WhitespaceTokenizerFactory.class),
					filters = @TokenFilterDef(name = "namedTokenFilter", factory = LowerCaseFilterFactory.class)
			),
			@AnalyzerDef(
					name = "analyzerWithComplexComponents",
					charFilters = @CharFilterDef(
							factory = HTMLStripCharFilterFactory.class,
							params = {
									@Parameter(name = "escapedTags", value = "br p")
							}
					),
					tokenizer = @TokenizerDef(
							factory = ClassicTokenizerFactory.class
					),
					filters = @TokenFilterDef(
							factory = WordDelimiterFilterFactory.class,
							params = {
									@Parameter(name = "generateWordParts", value = "1"),
									@Parameter(name = "generateNumberParts", value = "1"),
									@Parameter(name = "catenateWords", value = "0"),
									@Parameter(name = "catenateNumbers", value = "0"),
									@Parameter(name = "catenateAll", value = "0"),
									@Parameter(name = "splitOnCaseChange", value = "0"),
									@Parameter(name = "splitOnNumerics", value = "0"),
									@Parameter(name = "preserveOriginal", value = "1")
							}
					)
			),
			@AnalyzerDef(
					name = "analyzerWithNamedComplexComponents",
					charFilters = @CharFilterDef(
							name = "custom-html-stripper",
							factory = HTMLStripCharFilterFactory.class,
							params = {
									@Parameter(name = "escapedTags", value = "br p")
							}
					),
					tokenizer = @TokenizerDef(
							name = "custom-classic-tokenizer",
							factory = ClassicTokenizerFactory.class
					),
					filters = @TokenFilterDef(
							name = "custom-word-delimiter",
							factory = WordDelimiterFilterFactory.class,
							params = {
									@Parameter(name = "generateWordParts", value = "1"),
									@Parameter(name = "generateNumberParts", value = "1"),
									@Parameter(name = "catenateWords", value = "0"),
									@Parameter(name = "catenateNumbers", value = "0"),
									@Parameter(name = "catenateAll", value = "0"),
									@Parameter(name = "splitOnCaseChange", value = "0"),
									@Parameter(name = "splitOnNumerics", value = "0"),
									@Parameter(name = "preserveOriginal", value = "1")
							}
					)
			),
			@AnalyzerDef(
					name = "analyzerWithElasticsearchFactories",
					charFilters = @CharFilterDef(
							name = "custom-pattern-replace",
							factory = ElasticsearchCharFilterFactory.class,
							params = {
									@Parameter(name = "type", value = "'pattern_replace'"),
									@Parameter(name = "pattern", value = "'[^0-9]'"),
									@Parameter(name = "replacement", value = "'0'"),
									@Parameter(name = "tags", value = "'CASE_INSENSITIVE|COMMENTS'")
							}
					),
					tokenizer = @TokenizerDef(
							name = "custom-edgeNGram",
							factory = ElasticsearchTokenizerFactory.class,
							params = {
									@Parameter(name = "type", value = "'edgeNGram'"),
									@Parameter(name = "min_gram", value = "1"),
									@Parameter(name = "max_gram", value = "10")
							}
					),
					filters = @TokenFilterDef(
							name = "custom-keep-types",
							factory = ElasticsearchTokenFilterFactory.class,
							params = {
									@Parameter(name = "type", value = "'keep_types'"),
									@Parameter(name = "types", value = "['<NUM>','<DOUBLE>']")
							}
					)
			)
	})
	private static class SimpleAnalyzedEntity {
		@DocumentId
		@Id
		Long id;

		@Field(name = "myField1", analyzer = @Analyzer(definition = "analyzerWithSimpleComponents"))
		@Field(name = "myField2", analyzer = @Analyzer(definition = "analyzerWithNamedSimpleComponents"))
		@Field(name = "myField3", analyzer = @Analyzer(definition = "analyzerWithComplexComponents"))
		@Field(name = "myField4", analyzer = @Analyzer(definition = "analyzerWithNamedComplexComponents"))
		@Field(name = "myField5", analyzer = @Analyzer(definition = "analyzerWithElasticsearchFactories"))
		String myField;
	}

}
