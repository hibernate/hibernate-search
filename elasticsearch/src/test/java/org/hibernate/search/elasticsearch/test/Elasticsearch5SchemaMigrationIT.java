/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import static org.hibernate.search.elasticsearch.testutil.JsonHelper.assertJsonEquals;
import static org.hibernate.search.test.util.impl.ExceptionMatcherBuilder.isException;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.apache.lucene.analysis.ngram.EdgeNGramTokenizerFactory;
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
import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.elasticsearch.cfg.IndexSchemaManagementStrategy;
import org.hibernate.search.elasticsearch.impl.ElasticsearchIndexManager;
import org.hibernate.search.elasticsearch.testutil.TestElasticsearchClient;
import org.hibernate.search.elasticsearch.testutil.junit.SkipOnElasticsearch2;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.test.SearchInitializationTestBase;
import org.hibernate.search.test.util.ImmutableTestConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

/**
 * Tests for {@link ElasticsearchIndexManager}'s schema validation feature.
 *
 * @author Yoann Rodiere
 */
@Category(SkipOnElasticsearch2.class)
public class Elasticsearch5SchemaMigrationIT extends SearchInitializationTestBase {

	private static final String UPDATE_FAILED_MESSAGE_ID = "HSEARCH400035";
	private static final String MAPPING_CREATION_FAILED_MESSAGE_ID = "HSEARCH400020";
	private static final String ELASTICSEARCH_REQUEST_FAILED_MESSAGE_ID = "HSEARCH400007";

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public TestElasticsearchClient elasticSearchClient = new TestElasticsearchClient();

	@Override
	protected void init(Class<?>... annotatedClasses) {
		Map<String, Object> settings = new HashMap<>();
		settings.put(
				"hibernate.search.default." + ElasticsearchEnvironment.INDEX_SCHEMA_MANAGEMENT_STRATEGY,
				IndexSchemaManagementStrategy.UPDATE.getExternalName()
		);
		init( new ImmutableTestConfiguration( settings, annotatedClasses ) );
	}

	@Test
	public void nothingToDo() throws Exception {
		elasticSearchClient.index( SimpleDateEntity.class )
				.deleteAndCreate()
				.type( SimpleDateEntity.class ).putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'keyword',"
									+ "'index': true,"
									+ "'store': true"
							+ "},"
							+ "'myField': {"
									+ "'type': 'date',"
									+ "'index': true,"
									+ "'ignore_malformed': true" // Ignored during migration
							+ "},"
							+ "'NOTmyField': {" // Ignored during migration
									+ "'type': 'date',"
									+ "'index': true"
							+ "}"
					+ "}"
				+ "}"
				);
		elasticSearchClient.index( SimpleBooleanEntity.class )
				.deleteAndCreate()
				.type( SimpleBooleanEntity.class ).putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'keyword',"
									+ "'index': true,"
									+ "'store': true"
							+ "},"
							+ "'myField': {"
									+ "'type': 'boolean',"
									+ "'index': true"
							+ "},"
							+ "'NOTmyField': {" // Ignored during migration
									+ "'type': 'boolean',"
									+ "'index': true"
							+ "}"
					+ "}"
				+ "}"
				);
		elasticSearchClient.index( AnalyzedEntity.class ).deleteAndCreate();
		elasticSearchClient.index( AnalyzedEntity.class ).settings( "index.analysis" ).put(
				"{"
					+ "'analyzer': {"
							+ "'analyzerWithElasticsearchFactories': {"
									+ "'char_filter': ['custom-pattern-replace'],"
									+ "'tokenizer': 'custom-edgeNGram',"
									+ "'filter': ['custom-keep-types']"
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
									 */
									+ "'min_gram': '1',"
									+ "'max_gram': '10'"
							+ "}"
					+ "},"
					+ "'filter': {"
							+ "'custom-keep-types': {"
									+ "'type': 'keep_types',"
									+ "'types': ['<NUM>', '<DOUBLE>']"
							+ "}"
					+ "}"
				+ "}"
				);
		elasticSearchClient.type( AnalyzedEntity.class ).putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'keyword',"
									+ "'index': 'true',"
									+ "'store': true"
							+ "},"
							+ "'myField': {"
									+ "'type': 'text',"
									+ "'analyzer': 'analyzerWithElasticsearchFactories'"
							+ "}"
					+ "}"
				+ "}"
				);

		init( SimpleDateEntity.class, SimpleBooleanEntity.class, AnalyzedEntity.class );

		assertJsonEquals(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'keyword',"
									+ "'store': true"
							+ "},"
							+ "'myField': {"
									+ "'type': 'date',"
									+ "'ignore_malformed': true" // Assert it was not removed
							+ "},"
							+ "'NOTmyField': {" // Assert it was not removed
									+ "'type': 'date'"
							+ "}"
					+ "}"
				+ "}",
				elasticSearchClient.type( SimpleDateEntity.class ).getMapping()
				);
		assertJsonEquals(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'keyword',"
									+ "'store': true"
							+ "},"
							+ "'myField': {"
									+ "'type': 'boolean'"
							+ "},"
							+ "'NOTmyField': {" // Assert it was not removed
									+ "'type': 'boolean'"
							+ "}"
					+ "}"
				+ "}",
				elasticSearchClient.type( SimpleBooleanEntity.class ).getMapping()
				);

		assertJsonEquals(
				"{"
						+ "'analyzer': {"
								+ "'analyzerWithElasticsearchFactories': {"
										+ "'char_filter': ['custom-pattern-replace'],"
										+ "'tokenizer': 'custom-edgeNGram',"
										+ "'filter': ['custom-keep-types']"
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
										 */
										+ "'min_gram': '1',"
										+ "'max_gram': '10'"
								+ "}"
						+ "},"
						+ "'filter': {"
								+ "'custom-keep-types': {"
										+ "'type': 'keep_types',"
										+ "'types': ['<NUM>', '<DOUBLE>']"
								+ "}"
						+ "}"
					+ "}",
				elasticSearchClient.index( AnalyzedEntity.class ).settings( "index.analysis" ).get()
				);
		assertJsonEquals(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'keyword',"
									+ "'store': true"
							+ "},"
							+ "'myField': {"
									+ "'type': 'text',"
									+ "'analyzer': 'analyzerWithElasticsearchFactories'"
							+ "}"
					+ "}"
				+ "}",
				elasticSearchClient.type( AnalyzedEntity.class ).getMapping()
				);
	}

	@Test
	public void mapping_missing() throws Exception {
		elasticSearchClient.index( SimpleBooleanEntity.class ).deleteAndCreate();

		init( SimpleBooleanEntity.class );

		assertJsonEquals(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'keyword',"
									+ "'store': true"
							+ "},"
							+ "'myField': {"
									+ "'type': 'boolean'"
							+ "}"
					+ "}"
				+ "}",
				elasticSearchClient.type( SimpleBooleanEntity.class ).getMapping()
				);
	}

	@Test
	public void rootMapping_attribute_missing() throws Exception {
		elasticSearchClient.index( SimpleBooleanEntity.class )
				.deleteAndCreate()
				.type(SimpleBooleanEntity.class).putMapping(
				"{"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'keyword',"
									+ "'index': true,"
									+ "'store': true"
							+ "},"
							+ "'myField': {"
									+ "'type': 'boolean',"
									+ "'index': true"
							+ "},"
							+ "'NOTmyField': {"
									+ "'type': 'boolean',"
									+ "'index': true"
							+ "}"
					+ "}"
				+ "}"
				);

		init( SimpleBooleanEntity.class );

		assertJsonEquals(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'keyword',"
									+ "'store': true"
							+ "},"
							+ "'myField': {"
									+ "'type': 'boolean'"
							+ "},"
							+ "'NOTmyField': {" // Assert it was not removed
									+ "'type': 'boolean'"
							+ "}"
					+ "}"
				+ "}",
				elasticSearchClient.type( SimpleBooleanEntity.class ).getMapping()
				);
	}

	@Test
	public void property_missing() throws Exception {
		elasticSearchClient.index( SimpleDateEntity.class )
				.deleteAndCreate()
				.type( SimpleDateEntity.class ).putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'keyword',"
									+ "'index': true,"
									+ "'store': true"
							+ "},"
							+ "'NOTmyField': {"
									+ "'type': 'date',"
									+ "'index': true"
							+ "}"
					+ "}"
				+ "}"
				);

		init( SimpleDateEntity.class );

		assertJsonEquals(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'keyword',"
									+ "'store': true"
							+ "},"
							+ "'myField': {"
									+ "'type': 'date'"
							+ "},"
							+ "'NOTmyField': {" // Assert it was not removed
									+ "'type': 'date'"
							+ "}"
					+ "}"
				+ "}",
				elasticSearchClient.type( SimpleDateEntity.class ).getMapping()
				);
	}

	@Test
	public void property_attribute_invalid() throws Exception {
		elasticSearchClient.index( SimpleDateEntity.class )
				.deleteAndCreate()
				.type( SimpleDateEntity.class ).putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'keyword',"
									+ "'index': true,"
									+ "'store': true"
							+ "},"
							+ "'myField': {"
									+ "'type': 'date',"
									+ "'index': false" // Invalid
							+ "}"
					+ "}"
				+ "}"
				);

		thrown.expect(
				isException( SearchException.class )
						.withMessage( UPDATE_FAILED_MESSAGE_ID )
				.causedBy( SearchException.class )
						.withMessage( MAPPING_CREATION_FAILED_MESSAGE_ID )
				.causedBy( SearchException.class )
						.withMessage( ELASTICSEARCH_REQUEST_FAILED_MESSAGE_ID )
						.withMessage( "index" )
				.build()
		);

		init( SimpleDateEntity.class );
	}

	@Test
	public void property_attribute_invalid_conflictingAnalyzer() throws Exception {
		elasticSearchClient.index( AnalyzedEntity.class ).deleteAndCreate();
		elasticSearchClient.type( AnalyzedEntity.class ).putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'keyword',"
									+ "'index': 'true',"
									+ "'store': true"
							+ "},"
							+ "'myField': {"
									+ "'type': 'text',"
									+ "'analyzer': 'standard'" // Invalid
							+ "}"
					+ "}"
				+ "}"
				);

		thrown.expect(
				isException( SearchException.class )
						.withMessage( UPDATE_FAILED_MESSAGE_ID )
				.causedBy( SearchException.class )
						.withMessage( MAPPING_CREATION_FAILED_MESSAGE_ID )
				.causedBy( SearchException.class )
						.withMessage( ELASTICSEARCH_REQUEST_FAILED_MESSAGE_ID )
						.withMessage( "analyzer" )
				.build()
		);

		init( AnalyzedEntity.class );
	}

	@Test
	public void analyzer_missing() throws Exception {
		elasticSearchClient.index( AnalyzedEntity.class ).deleteAndCreate();
		elasticSearchClient.index( AnalyzedEntity.class ).settings( "index.analysis" ).put(
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
							+ "}"
					+ "}"
				+ "}"
				);
		elasticSearchClient.type( AnalyzedEntity.class ).putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'keyword',"
									+ "'index': 'true',"
									+ "'store': true"
							+ "}"
							/*
							 * We cannot update analyzers in the mapping,
							 * so the only way adding an analyzer can succeed is
							 * if the fields using it do not exist yet.
							 * Thus we don't mention "myField" here.
							 */
					+ "}"
				+ "}"
				);

		init( AnalyzedEntity.class );

		assertJsonEquals(
				"{"
						+ "'analyzer': {"
								+ "'analyzerWithElasticsearchFactories': {"
										+ "'char_filter': ['custom-pattern-replace'],"
										+ "'tokenizer': 'custom-edgeNGram',"
										+ "'filter': ['custom-keep-types']"
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
										 */
										+ "'min_gram': '1',"
										+ "'max_gram': '10'"
								+ "}"
						+ "},"
						+ "'filter': {"
								+ "'custom-keep-types': {"
										+ "'type': 'keep_types',"
										+ "'types': ['<NUM>', '<DOUBLE>']"
								+ "}"
						+ "}"
					+ "}",
				elasticSearchClient.index( AnalyzedEntity.class ).settings( "index.analysis" ).get()
				);
		assertJsonEquals(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'keyword',"
									+ "'store': true"
							+ "},"
							+ "'myField': {"
									+ "'type': 'text',"
									+ "'analyzer': 'analyzerWithElasticsearchFactories'"
							+ "}"
					+ "}"
				+ "}",
				elasticSearchClient.type( AnalyzedEntity.class ).getMapping()
				);
	}

	@Test
	public void analyzer_componentDefinition_missing() throws Exception {
		elasticSearchClient.index( AnalyzedEntity.class ).deleteAndCreate();
		elasticSearchClient.index( AnalyzedEntity.class ).settings( "index.analysis" ).put(
				"{"
					/*
					 * We don't add the analyzer here: since a component is missing
					 * the analyzer can't reference it and thus it must be missing too.
					 */
					// missing: 'char_filter'
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
							+ "}"
					+ "}"
				+ "}"
				);
		elasticSearchClient.type( AnalyzedEntity.class ).putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'keyword',"
									+ "'index': 'true',"
									+ "'store': true"
							+ "}"
							/*
							 * We cannot update analyzers in the mapping,
							 * so the only way adding an analyzer can succeed is
							 * if the fields using it do not exist yet.
							 * Thus we don't mention "myField" here.
							 */
					+ "}"
				+ "}"
				);

		init( AnalyzedEntity.class );

		assertJsonEquals(
				"{"
						+ "'analyzer': {"
								+ "'analyzerWithElasticsearchFactories': {"
										+ "'char_filter': ['custom-pattern-replace'],"
										+ "'tokenizer': 'custom-edgeNGram',"
										+ "'filter': ['custom-keep-types']"
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
										 */
										+ "'min_gram': '1',"
										+ "'max_gram': '10'"
								+ "}"
						+ "},"
						+ "'filter': {"
								+ "'custom-keep-types': {"
										+ "'type': 'keep_types',"
										+ "'types': ['<NUM>', '<DOUBLE>']"
								+ "}"
						+ "}"
					+ "}",
				elasticSearchClient.index( AnalyzedEntity.class ).settings( "index.analysis" ).get()
				);
		assertJsonEquals(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'keyword',"
									+ "'store': true"
							+ "},"
							+ "'myField': {"
									+ "'type': 'text',"
									+ "'analyzer': 'analyzerWithElasticsearchFactories'"
							+ "}"
					+ "}"
				+ "}",
				elasticSearchClient.type( AnalyzedEntity.class ).getMapping()
				);
	}

	@Test
	public void analyzer_componentReference_invalid() throws Exception {
		elasticSearchClient.index( AnalyzedEntity.class ).deleteAndCreate();
		elasticSearchClient.index( AnalyzedEntity.class ).settings( "index.analysis" ).put(
				"{"
					+ "'analyzer': {"
							+ "'analyzerWithElasticsearchFactories': {"
									+ "'char_filter': ['html_strip']," // Invalid
									+ "'tokenizer': 'custom-edgeNGram',"
									+ "'filter': ['custom-keep-types']"
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
							+ "}"
					+ "}"
				+ "}"
				);
		elasticSearchClient.type( AnalyzedEntity.class ).putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'keyword',"
									+ "'index': 'true',"
									+ "'store': true"
							+ "},"
							+ "'myField': {"
									+ "'type': 'text',"
									+ "'analyzer': 'analyzerWithElasticsearchFactories'"
							+ "}"
					+ "}"
				+ "}"
				);

		init( AnalyzedEntity.class );

		assertJsonEquals(
				"{"
						+ "'analyzer': {"
								+ "'analyzerWithElasticsearchFactories': {"
										+ "'char_filter': ['custom-pattern-replace'],"
										+ "'tokenizer': 'custom-edgeNGram',"
										+ "'filter': ['custom-keep-types']"
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
										 */
										+ "'min_gram': '1',"
										+ "'max_gram': '10'"
								+ "}"
						+ "},"
						+ "'filter': {"
								+ "'custom-keep-types': {"
										+ "'type': 'keep_types',"
										+ "'types': ['<NUM>', '<DOUBLE>']"
								+ "}"
						+ "}"
					+ "}",
				elasticSearchClient.index( AnalyzedEntity.class ).settings( "index.analysis" ).get()
				);
		assertJsonEquals(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'keyword',"
									+ "'store': true"
							+ "},"
							+ "'myField': {"
									+ "'type': 'text',"
									+ "'analyzer': 'analyzerWithElasticsearchFactories'"
							+ "}"
					+ "}"
				+ "}",
				elasticSearchClient.type( AnalyzedEntity.class ).getMapping()
				);
	}

	@Test
	public void analyzer_componentDefinition_invalid() throws Exception {
		elasticSearchClient.index( AnalyzedEntity.class ).deleteAndCreate();
		elasticSearchClient.index( AnalyzedEntity.class ).settings( "index.analysis" ).put(
				"{"
					+ "'analyzer': {"
							+ "'analyzerWithElasticsearchFactories': {"
									+ "'char_filter': ['custom-pattern-replace']," // Correct, but the actual definition is not
									+ "'tokenizer': 'custom-edgeNGram',"
									+ "'filter': ['custom-keep-types']"
							+ "}"
					+ "},"
					+ "'char_filter': {"
							+ "'custom-pattern-replace': {"
									+ "'type': 'html_strip'" // Invalid
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
							+ "}"
					+ "}"
				+ "}"
				);
		elasticSearchClient.type( AnalyzedEntity.class ).putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'keyword',"
									+ "'index': 'true',"
									+ "'store': true"
							+ "},"
							+ "'myField': {"
									+ "'type': 'text',"
									+ "'analyzer': 'analyzerWithElasticsearchFactories'"
							+ "}"
					+ "}"
				+ "}"
				);

		init( AnalyzedEntity.class );

		assertJsonEquals(
				"{"
						+ "'analyzer': {"
								+ "'analyzerWithElasticsearchFactories': {"
										+ "'char_filter': ['custom-pattern-replace'],"
										+ "'tokenizer': 'custom-edgeNGram',"
										+ "'filter': ['custom-keep-types']"
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
										 */
										+ "'min_gram': '1',"
										+ "'max_gram': '10'"
								+ "}"
						+ "},"
						+ "'filter': {"
								+ "'custom-keep-types': {"
										+ "'type': 'keep_types',"
										+ "'types': ['<NUM>', '<DOUBLE>']"
								+ "}"
						+ "}"
					+ "}",
				elasticSearchClient.index( AnalyzedEntity.class ).settings( "index.analysis" ).get()
				);
		assertJsonEquals(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'keyword',"
									+ "'store': true"
							+ "},"
							+ "'myField': {"
									+ "'type': 'text',"
									+ "'analyzer': 'analyzerWithElasticsearchFactories'"
							+ "}"
					+ "}"
				+ "}",
				elasticSearchClient.type( AnalyzedEntity.class ).getMapping()
				);
	}
	@Indexed
	@Entity
	public static class SimpleBooleanEntity {
		@DocumentId
		@Id
		Long id;

		@Field
		Boolean myField;
	}

	@Indexed
	@Entity
	public static class SimpleDateEntity {
		@DocumentId
		@Id
		Long id;

		@Field
		Date myField;
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
					factory = EdgeNGramTokenizerFactory.class,
					params = {
							@Parameter(name = "minGramSize", value = "1"),
							@Parameter(name = "maxGramSize", value = "10")
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
	public static class AnalyzedEntity {
		@DocumentId
		@Id
		Long id;

		@Field(analyzer = @Analyzer(definition = "analyzerWithElasticsearchFactories"))
		String myField;
	}

}
