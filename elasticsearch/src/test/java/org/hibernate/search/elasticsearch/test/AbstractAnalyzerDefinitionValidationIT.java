/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import static org.hibernate.search.test.util.impl.ExceptionMatcherBuilder.isException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.AnalyzerDef;
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
import org.hibernate.search.elasticsearch.schema.impl.ElasticsearchSchemaValidationException;
import org.hibernate.search.elasticsearch.testutil.TestElasticsearchClient;
import org.hibernate.search.test.SearchInitializationTestBase;
import org.hibernate.search.test.util.ImmutableTestConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests for {@link ElasticsearchIndexManager}'s analyzer definition validation feature.
 *
 * @author Yoann Rodiere
 */
public abstract class AbstractAnalyzerDefinitionValidationIT extends SearchInitializationTestBase {

	private static final String VALIDATION_FAILED_MESSAGE_ID = "HSEARCH400033";

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public TestElasticsearchClient elasticSearchClient = new TestElasticsearchClient();

	@Override
	protected void init(Class<?>... annotatedClasses) {
		Map<String, Object> settings = new HashMap<>();
		settings.put(
				"hibernate.search.default." + ElasticsearchEnvironment.INDEX_SCHEMA_MANAGEMENT_STRATEGY,
				IndexSchemaManagementStrategy.VALIDATE.getExternalName()
		);
		init( new ImmutableTestConfiguration( settings, annotatedClasses ) );
	}

	@Test
	public void success_simple() throws Exception {
		elasticSearchClient.index( AnalyzedEntity.class ).deleteAndCreate(
				"index.analysis",
				"{"
					+ "'analyzer': {"
							+ "'analyzerWithElasticsearchFactories': {"
									+ "'char_filter': ['custom-pattern-replace'],"
									+ "'tokenizer': 'custom-edgeNGram',"
									+ "'filter': ['custom-keep-types', 'custom-word-delimiter']"
							+ "}"
					+ "},"
					+ "'char_filter': {"
							+ "'custom-pattern-replace': {"
									+ "'type': 'pattern_replace',"
									+ "'pattern': '[^0-9]',"
									+ "'replacement': '0',"
									+ "'tags': 'CASE_INSENSITIVE|COMMENTS'"
							+ "}"
					+ "},"
					+ "'tokenizer': {"
							+ "'custom-edgeNGram': {"
									+ "'type': 'edgeNGram',"
									/*
									 * Strangely enough, even if you send properly typed numbers
									 * to Elasticsearch, when you ask for the current settings it
									 * will spit back strings instead of numbers...
									 * Testing both a properly typed number and a number as string here.
									 */
									+ "'min_gram': 1,"
									+ "'max_gram': '10'"
							+ "}"
					+ "},"
					+ "'filter': {"
							+ "'custom-keep-types': {"
									+ "'type': 'keep_types',"
									/*
									 * The order doesn't matter with this array.
									 * Here we test that validation ignores order.
									 */
									+ "'types': ['<DOUBLE>', '<NUM>']"
							+ "},"
							+ "'custom-word-delimiter': {"
									+ "'type': 'word_delimiter',"
									+ "'generate_word_parts': false"
							+ "}"
					+ "}"
				+ "}"
				);

		putMapping();

		init( AnalyzedEntity.class );

		// If we get here, it means validation passed (no exception was thrown)
	}

	protected abstract void putMapping() throws IOException;

	@Test
	public void analyzer_missing() throws Exception {
		elasticSearchClient.index( AnalyzedEntity.class ).deleteAndCreate(
				"index.analysis",
				"{"
					+ "'char_filter': {"
							+ "'custom-pattern-replace': {"
									+ "'type': 'pattern_replace',"
									+ "'pattern': '[^0-9]',"
									+ "'replacement': '0',"
									+ "'tags': 'CASE_INSENSITIVE|COMMENTS'"
							+ "}"
					+ "},"
					+ "'tokenizer': {"
							+ "'custom-edgeNGram': {"
									+ "'type': 'edgeNGram',"
									+ "'min_gram': 1,"
									+ "'max_gram': 10"
							+ "}"
					+ "},"
					+ "'filter': {"
							+ "'custom-keep-types': {"
									+ "'type': 'keep_types',"
									+ "'types': ['<NUM>', '<DOUBLE>']"
							+ "},"
							+ "'custom-word-delimiter': {"
									+ "'type': 'word_delimiter',"
									+ "'generate_word_parts': false"
							+ "}"
					+ "}"
				+ "}"
				);

		thrown.expect(
				isException( ElasticsearchSchemaValidationException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage( "analyzer 'analyzerWithElasticsearchFactories':\n\tMissing analyzer" )
				.build()
		);

		init( AnalyzedEntity.class );
	}

	@Test
	public void analyzer_charFilters_invalid() throws Exception {
		elasticSearchClient.index( AnalyzedEntity.class ).deleteAndCreate(
				"index.analysis",
				"{"
					+ "'analyzer': {"
							+ "'analyzerWithElasticsearchFactories': {"
									+ "'char_filter': ['html_strip'],"
									+ "'tokenizer': 'custom-edgeNGram',"
									+ "'filter': ['custom-keep-types', 'custom-word-delimiter']"
							+ "}"
					+ "},"
					+ "'char_filter': {"
							+ "'custom-pattern-replace': {"
									+ "'type': 'pattern_replace',"
									+ "'pattern': '[^0-9]',"
									+ "'replacement': '0',"
									+ "'tags': 'CASE_INSENSITIVE|COMMENTS'"
							+ "}"
					+ "},"
					+ "'tokenizer': {"
							+ "'custom-edgeNGram': {"
									+ "'type': 'edgeNGram',"
									+ "'min_gram': 1,"
									+ "'max_gram': 10"
							+ "}"
					+ "},"
					+ "'filter': {"
							+ "'custom-keep-types': {"
									+ "'type': 'keep_types',"
									+ "'types': ['<NUM>', '<DOUBLE>']"
							+ "},"
							+ "'custom-word-delimiter': {"
									+ "'type': 'word_delimiter',"
									+ "'generate_word_parts': false"
							+ "}"
					+ "}"
				+ "}"
				);

		thrown.expect(
				isException( ElasticsearchSchemaValidationException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage( "analyzer 'analyzerWithElasticsearchFactories':\n"
								+ "\tInvalid char filters. Expected '[custom-pattern-replace]',"
								+ " actual is '[html_strip]'" )
				.build()
		);

		init( AnalyzedEntity.class );
	}

	@Test
	public void analyzer_tokenizer_invalid() throws Exception {
		elasticSearchClient.index( AnalyzedEntity.class ).deleteAndCreate(
				"index.analysis",
				"{"
					+ "'analyzer': {"
							+ "'analyzerWithElasticsearchFactories': {"
									+ "'char_filter': ['custom-pattern-replace'],"
									+ "'tokenizer': 'whitespace',"
									+ "'filter': ['custom-keep-types', 'custom-word-delimiter']"
							+ "}"
					+ "},"
					+ "'char_filter': {"
							+ "'custom-pattern-replace': {"
									+ "'type': 'pattern_replace',"
									+ "'pattern': '[^0-9]',"
									+ "'replacement': '0',"
									+ "'tags': 'CASE_INSENSITIVE|COMMENTS'"
							+ "}"
					+ "},"
					+ "'tokenizer': {"
							+ "'custom-edgeNGram': {"
									+ "'type': 'edgeNGram',"
									+ "'min_gram': 1,"
									+ "'max_gram': 10"
							+ "}"
					+ "},"
					+ "'filter': {"
							+ "'custom-keep-types': {"
									+ "'type': 'keep_types',"
									+ "'types': ['<NUM>', '<DOUBLE>']"
							+ "},"
							+ "'custom-word-delimiter': {"
									+ "'type': 'word_delimiter',"
									+ "'generate_word_parts': false"
							+ "}"
					+ "}"
				+ "}"
				);

		thrown.expect(
				isException( ElasticsearchSchemaValidationException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage( "analyzer 'analyzerWithElasticsearchFactories':\n"
								+ "\tInvalid tokenizer. Expected 'custom-edgeNGram',"
								+ " actual is 'whitespace'" )
				.build()
		);

		init( AnalyzedEntity.class );
	}

	@Test
	public void analyzer_tokenFilters_invalid() throws Exception {
		elasticSearchClient.index( AnalyzedEntity.class ).deleteAndCreate(
				"index.analysis",
				"{"
					+ "'analyzer': {"
							+ "'analyzerWithElasticsearchFactories': {"
									+ "'char_filter': ['custom-pattern-replace'],"
									+ "'tokenizer': 'custom-edgeNGram',"
									+ "'filter': ['standard', 'custom-word-delimiter']"
							+ "}"
					+ "},"
					+ "'char_filter': {"
							+ "'custom-pattern-replace': {"
									+ "'type': 'pattern_replace',"
									+ "'pattern': '[^0-9]',"
									+ "'replacement': '0',"
									+ "'tags': 'CASE_INSENSITIVE|COMMENTS'"
							+ "}"
					+ "},"
					+ "'tokenizer': {"
							+ "'custom-edgeNGram': {"
									+ "'type': 'edgeNGram',"
									+ "'min_gram': 1,"
									+ "'max_gram': 10"
							+ "}"
					+ "},"
					+ "'filter': {"
							+ "'custom-keep-types': {"
									+ "'type': 'keep_types',"
									+ "'types': ['<NUM>', '<DOUBLE>']"
							+ "},"
							+ "'custom-word-delimiter': {"
									+ "'type': 'word_delimiter',"
									+ "'generate_word_parts': false"
							+ "}"
					+ "}"
				+ "}"
				);

		thrown.expect(
				isException( ElasticsearchSchemaValidationException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage( "analyzer 'analyzerWithElasticsearchFactories':\n"
								+ "\tInvalid token filters. Expected '[custom-keep-types, custom-word-delimiter]',"
								+ " actual is '[standard, custom-word-delimiter]'" )
				.build()
		);

		init( AnalyzedEntity.class );
	}

	@Test
	public void charFilter_missing() throws Exception {
		elasticSearchClient.index( AnalyzedEntity.class ).deleteAndCreate(
				"index.analysis",
				"{"
					+ "'tokenizer': {"
							+ "'custom-edgeNGram': {"
									+ "'type': 'edgeNGram',"
									+ "'min_gram': 1,"
									+ "'max_gram': 10"
							+ "}"
					+ "},"
					+ "'filter': {"
							+ "'custom-keep-types': {"
									+ "'type': 'keep_types',"
									+ "'types': ['<NUM>', '<DOUBLE>']"
							+ "},"
							+ "'custom-word-delimiter': {"
									+ "'type': 'word_delimiter',"
									+ "'generate_word_parts': false"
							+ "}"
					+ "}"
				+ "}"
				);

		thrown.expect(
				isException( ElasticsearchSchemaValidationException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage( "char filter 'custom-pattern-replace':\n\tMissing char filter" )
				.build()
		);

		init( AnalyzedEntity.class );
	}

	@Test
	public void tokenizer_missing() throws Exception {
		elasticSearchClient.index( AnalyzedEntity.class ).deleteAndCreate(
				"index.analysis",
				"{"
					+ "'char_filter': {"
							+ "'custom-pattern-replace': {"
									+ "'type': 'pattern_replace',"
									+ "'pattern': '[^0-9]',"
									+ "'replacement': '0',"
									+ "'tags': 'CASE_INSENSITIVE|COMMENTS'"
							+ "}"
					+ "},"
					+ "'filter': {"
							+ "'custom-keep-types': {"
									+ "'type': 'keep_types',"
									+ "'types': ['<NUM>', '<DOUBLE>']"
							+ "},"
							+ "'custom-word-delimiter': {"
									+ "'type': 'word_delimiter',"
									+ "'generate_word_parts': false"
							+ "}"
					+ "}"
				+ "}"
				);

		thrown.expect(
				isException( ElasticsearchSchemaValidationException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage( "tokenizer 'custom-edgeNGram':\n\tMissing tokenizer" )
				.build()
		);

		init( AnalyzedEntity.class );
	}

	@Test
	public void tokenFilter_missing() throws Exception {
		elasticSearchClient.index( AnalyzedEntity.class ).deleteAndCreate(
				"index.analysis",
				"{"
					+ "'char_filter': {"
							+ "'custom-pattern-replace': {"
									+ "'type': 'pattern_replace',"
									+ "'pattern': '[^0-9]',"
									+ "'replacement': '0',"
									+ "'tags': 'CASE_INSENSITIVE|COMMENTS'"
							+ "}"
					+ "},"
					+ "'tokenizer': {"
							+ "'custom-edgeNGram': {"
									+ "'type': 'edgeNGram',"
									+ "'min_gram': 1,"
									+ "'max_gram': 10"
							+ "}"
					+ "}"
				+ "}"
				);

		thrown.expect(
				isException( ElasticsearchSchemaValidationException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage( "token filter 'custom-keep-types':\n\tMissing token filter" )
				.build()
		);

		init( AnalyzedEntity.class );
	}

	@Test
	public void charFilter_type_invalid() throws Exception {
		elasticSearchClient.index( AnalyzedEntity.class ).deleteAndCreate(
				"index.analysis",
				"{"
					+ "'analyzer': {"
							+ "'analyzerWithElasticsearchFactories': {"
									+ "'char_filter': ['custom-pattern-replace'],"
									+ "'tokenizer': 'custom-edgeNGram',"
									+ "'filter': ['custom-keep-types', 'custom-word-delimiter']"
							+ "}"
					+ "},"
					+ "'char_filter': {"
							+ "'custom-pattern-replace': {"
									+ "'type': 'html_strip',"
									+ "'pattern': '[^0-9]',"
									+ "'replacement': '0',"
									+ "'tags': 'CASE_INSENSITIVE|COMMENTS'"
							+ "}"
					+ "},"
					+ "'tokenizer': {"
							+ "'custom-edgeNGram': {"
									+ "'type': 'edgeNGram',"
									+ "'min_gram': 1,"
									+ "'max_gram': 10"
							+ "}"
					+ "},"
					+ "'filter': {"
							+ "'custom-keep-types': {"
									+ "'type': 'keep_types',"
									+ "'types': ['<NUM>', '<DOUBLE>']"
							+ "},"
							+ "'custom-word-delimiter': {"
									+ "'type': 'word_delimiter',"
									+ "'generate_word_parts': false"
							+ "}"
					+ "}"
				+ "}"
				);

		thrown.expect(
				isException( ElasticsearchSchemaValidationException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage( "char filter 'custom-pattern-replace':\n\tInvalid type."
								+ " Expected 'pattern_replace', actual is 'html_strip'" )
				.build()
		);

		init( AnalyzedEntity.class );
	}

	@Test
	public void charFilter_parameter_invalid() throws Exception {
		elasticSearchClient.index( AnalyzedEntity.class ).deleteAndCreate(
				"index.analysis",
				"{"
					+ "'analyzer': {"
							+ "'analyzerWithElasticsearchFactories': {"
									+ "'char_filter': ['custom-pattern-replace'],"
									+ "'tokenizer': 'custom-edgeNGram',"
									+ "'filter': ['custom-keep-types', 'custom-word-delimiter']"
							+ "}"
					+ "},"
					+ "'char_filter': {"
							+ "'custom-pattern-replace': {"
									+ "'type': 'pattern_replace',"
									+ "'pattern': '[^a-z]',"
									+ "'replacement': '0',"
									+ "'tags': 'CASE_INSENSITIVE|COMMENTS'"
							+ "}"
					+ "},"
					+ "'tokenizer': {"
							+ "'custom-edgeNGram': {"
									+ "'type': 'edgeNGram',"
									+ "'min_gram': 1,"
									+ "'max_gram': 10"
							+ "}"
					+ "},"
					+ "'filter': {"
							+ "'custom-keep-types': {"
									+ "'type': 'keep_types',"
									+ "'types': ['<NUM>', '<DOUBLE>']"
							+ "},"
							+ "'custom-word-delimiter': {"
									+ "'type': 'word_delimiter',"
									+ "'generate_word_parts': false"
							+ "}"
					+ "}"
				+ "}"
				);

		thrown.expect(
				isException( ElasticsearchSchemaValidationException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage( "char filter 'custom-pattern-replace':\n"
								+ "\tInvalid value for parameter 'pattern'. Expected '\"[^0-9]\"', actual is '\"[^a-z]\"'" )
				.build()
		);

		init( AnalyzedEntity.class );
	}

	@Test
	public void charFilter_parameter_missing() throws Exception {
		elasticSearchClient.index( AnalyzedEntity.class ).deleteAndCreate(
				"index.analysis",
				"{"
					+ "'analyzer': {"
							+ "'analyzerWithElasticsearchFactories': {"
									+ "'char_filter': ['custom-pattern-replace'],"
									+ "'tokenizer': 'custom-edgeNGram',"
									+ "'filter': ['custom-keep-types', 'custom-word-delimiter']"
							+ "}"
					+ "},"
					+ "'char_filter': {"
							+ "'custom-pattern-replace': {"
									+ "'type': 'pattern_replace',"
									+ "'pattern': '[^0-9]',"
									+ "'replacement': '0'"
									// Missing "tags"
							+ "}"
					+ "},"
					+ "'tokenizer': {"
							+ "'custom-edgeNGram': {"
									+ "'type': 'edgeNGram',"
									+ "'min_gram': 1,"
									+ "'max_gram': 10"
							+ "}"
					+ "},"
					+ "'filter': {"
							+ "'custom-keep-types': {"
									+ "'type': 'keep_types',"
									+ "'types': ['<NUM>', '<DOUBLE>']"
							+ "},"
							+ "'custom-word-delimiter': {"
									+ "'type': 'word_delimiter',"
									+ "'generate_word_parts': false"
							+ "}"
					+ "}"
				+ "}"
				);

		thrown.expect(
				isException( ElasticsearchSchemaValidationException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage( "char filter 'custom-pattern-replace':\n"
								+ "\tInvalid value for parameter 'tags'."
								+ " Expected '\"CASE_INSENSITIVE|COMMENTS\"', actual is 'null'" )
				.build()
		);

		init( AnalyzedEntity.class );
	}

	@Test
	public void tokenFilter_parameter_unexpected() throws Exception {
		elasticSearchClient.index( AnalyzedEntity.class ).deleteAndCreate(
				"index.analysis",
				"{"
					+ "'analyzer': {"
							+ "'analyzerWithElasticsearchFactories': {"
									+ "'char_filter': ['custom-pattern-replace'],"
									+ "'tokenizer': 'custom-edgeNGram',"
									+ "'filter': ['custom-keep-types', 'custom-word-delimiter']"
							+ "}"
					+ "},"
					+ "'char_filter': {"
							+ "'custom-pattern-replace': {"
									+ "'type': 'pattern_replace',"
									+ "'pattern': '[^0-9]',"
									+ "'replacement': '0',"
									+ "'tags': 'CASE_INSENSITIVE|COMMENTS'"
							+ "}"
					+ "},"
					+ "'tokenizer': {"
							+ "'custom-edgeNGram': {"
									+ "'type': 'edgeNGram',"
									+ "'min_gram': 1,"
									+ "'max_gram': 10"
							+ "}"
					+ "},"
					+ "'filter': {"
							+ "'custom-keep-types': {"
									+ "'type': 'keep_types',"
									+ "'types': ['<NUM>', '<DOUBLE>']"
							+ "},"
							+ "'custom-word-delimiter': {"
									+ "'type': 'word_delimiter',"
									+ "'generate_word_parts': false,"
									+ "'generate_number_parts': false"
							+ "}"
					+ "}"
				+ "}"
				);

		thrown.expect(
				isException( ElasticsearchSchemaValidationException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage( "filter 'custom-word-delimiter':\n"
								+ "\tInvalid value for parameter 'generate_number_parts'."
								+ " Expected 'null', actual is '\"false\"'" )
				.build()
		);

		init( AnalyzedEntity.class );
	}

	@Indexed
	@Entity
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
							@Parameter(name = "type", value = "edgeNGram"),
							@Parameter(name = "min_gram", value = "1"),
							/*
							 * Use a string instead of an integer here on purpose.
							 * We must test that, because we tend to use strings
							 * instead of integers/floats/booleans/etc. when translating
							 * from definitions referring to Lucene factories.
							 */
							@Parameter(name = "max_gram", value = "'10'")
					}
			),
			filters = {
					@TokenFilterDef(
							name = "custom-keep-types",
							factory = ElasticsearchTokenFilterFactory.class,
							params = {
									@Parameter(name = "type", value = "'keep_types'"),
									@Parameter(name = "types", value = "['<NUM>','<DOUBLE>']")
							}
					),
					@TokenFilterDef(
							name = "custom-word-delimiter",
							factory = ElasticsearchTokenFilterFactory.class,
							params = {
									@Parameter(name = "type", value = "'word_delimiter'"),
									@Parameter(name = "generate_word_parts", value = "false")
							}
					)
			}
	)
	public static class AnalyzedEntity {
		@DocumentId
		@Id
		Long id;

		@Field(analyzer = @Analyzer(definition = "analyzerWithElasticsearchFactories"))
		String myField;
	}
}
