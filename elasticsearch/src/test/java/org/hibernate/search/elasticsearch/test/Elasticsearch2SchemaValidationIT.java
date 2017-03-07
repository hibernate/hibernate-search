/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import static org.hibernate.search.test.util.impl.ExceptionMatcherBuilder.isException;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.CharFilterDef;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Facet;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Parameter;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.TokenizerDef;
import org.hibernate.search.elasticsearch.analyzer.ElasticsearchCharFilterFactory;
import org.hibernate.search.elasticsearch.analyzer.ElasticsearchTokenFilterFactory;
import org.hibernate.search.elasticsearch.analyzer.ElasticsearchTokenizerFactory;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.elasticsearch.cfg.IndexSchemaManagementStrategy;
import org.hibernate.search.elasticsearch.impl.ElasticsearchIndexManager;
import org.hibernate.search.elasticsearch.impl.ToElasticsearch;
import org.hibernate.search.elasticsearch.schema.impl.ElasticsearchSchemaValidationException;
import org.hibernate.search.elasticsearch.testutil.TestElasticsearchClient;
import org.hibernate.search.elasticsearch.testutil.junit.SkipOnElasticsearch5;
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
@Category(SkipOnElasticsearch5.class)
public class Elasticsearch2SchemaValidationIT extends SearchInitializationTestBase {

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
				IndexSchemaManagementStrategy.VALIDATE.name()
		);
		init( new ImmutableTestConfiguration( settings, annotatedClasses ) );
	}

	@Test
	public void success_simple() throws Exception {
		elasticSearchClient.index( SimpleDateEntity.class ).deleteAndCreate();
		elasticSearchClient.type( SimpleDateEntity.class ).putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'string',"
									+ "'index': 'not_analyzed',"
									+ "'store': true"
							+ "},"
							+ "'myField': {"
									+ "'type': 'date',"
									+ "'index': 'not_analyzed',"
									+ "'ignore_malformed': true" // Ignored during validation
							+ "},"
							+ "'NOTmyField': {" // Ignored during validation
									+ "'type': 'date',"
									+ "'index': 'not_analyzed'"
							+ "}"
					+ "}"
				+ "}"
				);
		elasticSearchClient.index( SimpleBooleanEntity.class ).deleteAndCreate();
		elasticSearchClient.type( SimpleBooleanEntity.class ).putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'string',"
									+ "'index': 'not_analyzed',"
									+ "'store': true"
							+ "},"
							+ "'myField': {"
									+ "'type': 'boolean',"
									+ "'index': 'not_analyzed'"
							+ "},"
							+ "'NOTmyField': {" // Ignored during validation
									+ "'type': 'boolean',"
									+ "'index': 'not_analyzed'"
							+ "}"
					+ "}"
				+ "}"
				);
		elasticSearchClient.index( SimpleStringEntity.class ).deleteAndCreate();
		elasticSearchClient.type( SimpleStringEntity.class ).putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'string',"
									+ "'index': 'not_analyzed',"
									+ "'store': true"
							+ "},"
							+ "'myField': {"
									+ "'type': 'string',"
									+ "'index': 'analyzed',"
									+ "'analyzer': 'default'"
							+ "},"
							+ "'NOTmyField': {" // Ignored during validation
									+ "'type': 'string',"
									+ "'index': 'not_analyzed'"
							+ "}"
					+ "}"
				+ "}"
				);
		elasticSearchClient.index( FacetedDateEntity.class ).deleteAndCreate();
		elasticSearchClient.type( FacetedDateEntity.class ).putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'string',"
									+ "'index': 'not_analyzed',"
									+ "'store': true"
							+ "},"
							+ "'myField': {"
									+ "'type': 'date',"
									+ "'index': 'not_analyzed',"
									+ "'ignore_malformed': true," // Ignored during validation
									+ "'fields': {"
											+ "'myField" + ToElasticsearch.FACET_FIELD_SUFFIX + "': {"
													+ "'type': 'date',"
													+ "'index': 'not_analyzed'"
											+ "}"
									+ "}"
							+ "},"
							+ "'NOTmyField': {" // Ignored during validation
									+ "'type': 'date',"
									+ "'index': 'not_analyzed'"
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
		elasticSearchClient.type( AnalyzedEntity.class ).putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'string',"
									+ "'index': 'not_analyzed',"
									+ "'store': true"
							+ "},"
							+ "'myField': {"
									+ "'type': 'string',"
									+ "'analyzer': 'analyzerWithElasticsearchFactories'"
							+ "}"
					+ "}"
				+ "}"
				);

		init( SimpleDateEntity.class, SimpleBooleanEntity.class, SimpleStringEntity.class,
				FacetedDateEntity.class, AnalyzedEntity.class );

		// If we get here, it means validation passed (no exception was thrown)
	}

	@Test
	public void mapping_missing() throws Exception {
		elasticSearchClient.index( SimpleDateEntity.class ).deleteAndCreate();

		thrown.expect(
				isException( ElasticsearchSchemaValidationException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage( "\n\tMissing type mapping" )
				.build()
		);

		init( SimpleDateEntity.class );
	}

	@Test
	public void rootMapping_attribute_missing() throws Exception {
		elasticSearchClient.index( SimpleDateEntity.class ).deleteAndCreate();
		elasticSearchClient.type( SimpleDateEntity.class ).putMapping(
				"{"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'string',"
									+ "'index': 'not_analyzed',"
									+ "'store': true"
							+ "},"
							+ "'myField': {"
									+ "'type': 'date',"
									+ "'index': 'not_analyzed'"
							+ "}"
					+ "}"
				+ "}"
				);

		thrown.expect(
				isException( ElasticsearchSchemaValidationException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage( "\n\tInvalid value for attribute 'dynamic'. Expected 'STRICT', actual is 'null'" )
				.build()
		);

		init( SimpleDateEntity.class );
	}

	@Test
	public void rootMapping_attribute_dynamic_invalid() throws Exception {
		elasticSearchClient.index( SimpleDateEntity.class ).deleteAndCreate();
		elasticSearchClient.type( SimpleDateEntity.class ).putMapping(
				"{"
					+ "'dynamic': false,"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'string',"
									+ "'index': 'not_analyzed',"
									+ "'store': true"
							+ "},"
							+ "'myField': {"
									+ "'type': 'date',"
									+ "'index': 'not_analyzed'"
							+ "}"
					+ "}"
				+ "}"
				);

		thrown.expect(
				isException( ElasticsearchSchemaValidationException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage( "\n\tInvalid value for attribute 'dynamic'. Expected 'STRICT', actual is 'FALSE'" )
				.build()
		);

		init( SimpleDateEntity.class );
	}

	@Test
	public void properties_missing() throws Exception {
		elasticSearchClient.index( SimpleDateEntity.class ).deleteAndCreate();
		elasticSearchClient.type( SimpleDateEntity.class ).putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'string',"
									+ "'index': 'not_analyzed',"
									+ "'store': true"
							+ "}"
					+ "}"
				+ "}"
				);

		thrown.expect(
				isException( ElasticsearchSchemaValidationException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage( "property 'myField'" )
						.withMessage( "\n\tMissing property mapping" )
				.build()
		);

		init( SimpleDateEntity.class );
	}

	@Test
	public void property_missing() throws Exception {
		elasticSearchClient.index( SimpleDateEntity.class ).deleteAndCreate();
		elasticSearchClient.type( SimpleDateEntity.class ).putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'string',"
									+ "'index': 'not_analyzed',"
									+ "'store': true"
							+ "},"
							+ "'NOTmyField': {"
									+ "'type': 'date',"
									+ "'index': 'not_analyzed'"
							+ "}"
					+ "}"
				+ "}"
				);

		thrown.expect(
				isException( ElasticsearchSchemaValidationException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage( "property 'myField'" )
						.withMessage( "\n\tMissing property mapping" )
				.build()
		);

		init( SimpleDateEntity.class );
	}

	@Test
	public void property_attribute_missing() throws Exception {
		elasticSearchClient.index( SimpleDateEntity.class ).deleteAndCreate();
		elasticSearchClient.type( SimpleDateEntity.class ).putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'string',"
									+ "'index': 'not_analyzed',"
									+ "'store': true"
							+ "},"
							+ "'myField': {"
									+ "'type': 'object'"
							+ "}"
					+ "}"
				+ "}"
				);

		thrown.expect(
				isException( ElasticsearchSchemaValidationException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage( "property 'myField'" )
						.withMessage( "\n\tInvalid value for attribute 'type'. Expected 'DATE', actual is 'OBJECT'" )
				.build()
		);

		init( SimpleDateEntity.class );
	}

	@Test
	public void property_attribute_invalid() throws Exception {
		elasticSearchClient.index( SimpleDateEntity.class ).deleteAndCreate();
		elasticSearchClient.type( SimpleDateEntity.class ).putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'string',"
									+ "'index': 'not_analyzed',"
									+ "'store': true"
							+ "},"
							+ "'myField': {"
									+ "'type': 'date',"
									+ "'index': 'analyzed'"
							+ "}"
					+ "}"
				+ "}"
				);

		thrown.expect(
				isException( ElasticsearchSchemaValidationException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage( "property 'myField'" )
						.withMessage( "\n\tInvalid value for attribute 'index'. Expected 'NOT_ANALYZED', actual is 'ANALYZED'" )
				.build()
		);

		init( SimpleDateEntity.class );
	}

	@Test
	public void property_analyzer_invalid() throws Exception {
		elasticSearchClient.index( SimpleStringEntity.class ).deleteAndCreate();
		elasticSearchClient.type( SimpleStringEntity.class ).putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'string',"
									+ "'index': not_analyzed,"
									+ "'store': true"
							+ "},"
							+ "'myField': {"
									+ "'type': 'string',"
									+ "'index': analyzed,"
									+ "'analyzer': 'keyword'"
							+ "}"
					+ "}"
				+ "}"
				);

		thrown.expect(
				isException( ElasticsearchSchemaValidationException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage( "property 'myField'" )
						.withMessage( "\n\tInvalid value for attribute 'analyzer'. Expected 'default', actual is 'keyword'" )
				.build()
		);

		init( SimpleStringEntity.class );
	}

	@Test
	public void property_format_invalidOutputFormat() throws Exception {
		elasticSearchClient.index( SimpleDateEntity.class ).deleteAndCreate();
		elasticSearchClient.type( SimpleDateEntity.class ).putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'string',"
									+ "'index': 'not_analyzed',"
									+ "'store': true"
							+ "},"
							+ "'myField': {"
									+ "'type': 'date',"
									+ "'format': 'epoch_millis||yyyy'"
							+ "}"
					+ "}"
				+ "}"
				);

		thrown.expect(
				isException( ElasticsearchSchemaValidationException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage( "property 'myField'" )
						.withMessage( "\n\tThe output format (the first format in the 'format' attribute) is invalid."
								+ " Expected 'strict_date_optional_time', actual is 'epoch_millis'" )
				.build()
		);

		init( SimpleDateEntity.class );
	}

	@Test
	public void property_format_missingInputFormat() throws Exception {
		elasticSearchClient.index( SimpleDateEntity.class ).deleteAndCreate();
		elasticSearchClient.type( SimpleDateEntity.class ).putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'string',"
									+ "'index': 'not_analyzed',"
									+ "'store': true"
							+ "},"
							+ "'myField': {"
									+ "'type': 'date',"
									+ "'format': 'strict_date_optional_time'"
							+ "}"
					+ "}"
				+ "}"
				);

		thrown.expect(
				isException( ElasticsearchSchemaValidationException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage( "property 'myField'" )
						.withMessage( "\n\tInvalid formats for attribute 'format'" )
						.withMessage( "missing elements are '[epoch_millis]'" )
				.build()
		);

		init( SimpleDateEntity.class );
	}

	@Test
	public void property_format_unexpectedInputFormat() throws Exception {
		elasticSearchClient.index( SimpleDateEntity.class ).deleteAndCreate();
		elasticSearchClient.type( SimpleDateEntity.class ).putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'string',"
									+ "'index': 'not_analyzed',"
									+ "'store': true"
							+ "},"
							+ "'myField': {"
									+ "'type': 'date',"
									+ "'format': 'strict_date_optional_time||epoch_millis||yyyy'"
							+ "}"
					+ "}"
				+ "}"
				);

		thrown.expect(
				isException( ElasticsearchSchemaValidationException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage( "property 'myField'" )
						.withMessage( "\n\tInvalid formats for attribute 'format'" )
						.withMessage( "unexpected elements are '[yyyy]'" )
				.build()
		);

		init( SimpleDateEntity.class );
	}

	/**
	 * Tests that mappings that are more powerful than requested will pass validation.
	 */
	@Test
	public void property_attribute_leniency() throws Exception {
		elasticSearchClient.index( SimpleLenientEntity.class ).deleteAndCreate();
		elasticSearchClient.type( SimpleLenientEntity.class ).putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'string',"
									+ "'index': 'not_analyzed',"
									+ "'store': true"
							+ "},"
							+ "'myField': {"
									+ "'type': 'long',"
									+ "'index': 'analyzed',"
									+ "'store': 'yes'"
							+ "}"
					+ "}"
				+ "}"
				);

		init( SimpleLenientEntity.class );
	}

	@Test
	public void multipleErrors_singleIndexManagers() throws Exception {
		elasticSearchClient.index( SimpleDateEntity.class ).deleteAndCreate();
		elasticSearchClient.type( SimpleDateEntity.class ).putMapping(
				"{"
					+ "'dynamic': false,"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'string',"
									+ "'index': 'not_analyzed',"
									+ "'store': true"
							+ "},"
							+ "'myField': {"
									+ "'type': 'string'"
							+ "}"
					+ "}"
				+ "}"
				);

		thrown.expect(
				isException( ElasticsearchSchemaValidationException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage(
								"\nindex 'org.hibernate.search.elasticsearch.test.elasticsearch2schemavalidationit$simpledateentity',"
								+ " mapping 'org.hibernate.search.elasticsearch.test.Elasticsearch2SchemaValidationIT$SimpleDateEntity':"
								+ "\n\tInvalid value for attribute 'dynamic'. Expected 'STRICT', actual is 'FALSE'"
								+ "\nindex 'org.hibernate.search.elasticsearch.test.elasticsearch2schemavalidationit$simpledateentity',"
								+ " mapping 'org.hibernate.search.elasticsearch.test.Elasticsearch2SchemaValidationIT$SimpleDateEntity',"
								+ " property 'myField':"
								+ "\n\tInvalid value for attribute 'type'. Expected 'DATE', actual is 'STRING'"
						)
				.build()
		);

		init( SimpleDateEntity.class );
	}

	@Test
	public void multipleErrors_multipleIndexManagers() throws Exception {
		elasticSearchClient.index( SimpleDateEntity.class ).deleteAndCreate();
		elasticSearchClient.type( SimpleDateEntity.class ).putMapping(
				"{"
					+ "'dynamic': false,"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'string',"
									+ "'index': 'not_analyzed',"
									+ "'store': true"
							+ "},"
							+ "'myField': {"
									+ "'type': 'string'"
							+ "}"
					+ "}"
				+ "}"
				);
		elasticSearchClient.index( SimpleBooleanEntity.class ).deleteAndCreate();
		elasticSearchClient.type( SimpleBooleanEntity.class ).putMapping(
				"{"
					+ "'dynamic': false,"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'string',"
									+ "'index': 'not_analyzed',"
									+ "'store': true"
							+ "},"
							+ "'myField': {"
									+ "'type': 'boolean'"
							+ "}"
					+ "}"
				+ "}"
				);

		thrown.expect(
				isException( ElasticsearchSchemaValidationException.class )
				.withMainOrSuppressed(
						isException( ElasticsearchSchemaValidationException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage(
								"\nindex 'org.hibernate.search.elasticsearch.test.elasticsearch2schemavalidationit$simplebooleanentity',"
								+ " mapping 'org.hibernate.search.elasticsearch.test.Elasticsearch2SchemaValidationIT$SimpleBooleanEntity':"
								+ "\n\tInvalid value for attribute 'dynamic'. Expected 'STRICT', actual is 'FALSE'"
						)
						.build()
				)
				.withMainOrSuppressed(
						isException( ElasticsearchSchemaValidationException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage(
								"\nindex 'org.hibernate.search.elasticsearch.test.elasticsearch2schemavalidationit$simpledateentity',"
								+ " mapping 'org.hibernate.search.elasticsearch.test.Elasticsearch2SchemaValidationIT$SimpleDateEntity':"
								+ "\n\tInvalid value for attribute 'dynamic'. Expected 'STRICT', actual is 'FALSE'"
								+ "\nindex 'org.hibernate.search.elasticsearch.test.elasticsearch2schemavalidationit$simpledateentity',"
								+ " mapping 'org.hibernate.search.elasticsearch.test.Elasticsearch2SchemaValidationIT$SimpleDateEntity',"
								+ " property 'myField':"
								+ "\n\tInvalid value for attribute 'type'. Expected 'DATE', actual is 'STRING'"
						)
						.build()
				)
				.build()
		);

		init( SimpleBooleanEntity.class, SimpleDateEntity.class );
	}

	@Test
	public void property_fields_missing() throws Exception {
		elasticSearchClient.index( FacetedDateEntity.class ).deleteAndCreate();
		elasticSearchClient.type( FacetedDateEntity.class ).putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'string',"
									+ "'index': 'not_analyzed',"
									+ "'store': true"
							+ "},"
							+ "'myField': {"
									+ "'type': 'date',"
									+ "'index': 'not_analyzed'"
							+ "}"
					+ "}"
				+ "}"
				);

		thrown.expect(
				isException( ElasticsearchSchemaValidationException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage( "property 'myField', field 'myField" + ToElasticsearch.FACET_FIELD_SUFFIX + "'" )
						.withMessage( "\n\tMissing field mapping" )
				.build()
		);

		init( FacetedDateEntity.class );
	}

	@Test
	public void property_field_missing() throws Exception {
		elasticSearchClient.index( FacetedDateEntity.class ).deleteAndCreate();
		elasticSearchClient.type( FacetedDateEntity.class ).putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'string',"
									+ "'index': 'not_analyzed',"
									+ "'store': true"
							+ "},"
							+ "'myField': {"
									+ "'type': 'date',"
									+ "'index': 'not_analyzed',"
									+ "'fields': {"
											+ "'NOTmyField" + ToElasticsearch.FACET_FIELD_SUFFIX + "': {"
													+ "'type': 'date',"
													+ "'index': 'not_analyzed'"
											+ "}"
									+ "}"
							+ "}"
					+ "}"
				+ "}"
				);

		thrown.expect(
				isException( ElasticsearchSchemaValidationException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage( "property 'myField', field 'myField" + ToElasticsearch.FACET_FIELD_SUFFIX + "'" )
						.withMessage( "\n\tMissing field mapping" )
				.build()
		);

		init( FacetedDateEntity.class );
	}

	@Test
	public void property_field_attribute_invalid() throws Exception {
		elasticSearchClient.index( FacetedDateEntity.class ).deleteAndCreate();
		elasticSearchClient.type( FacetedDateEntity.class ).putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'string',"
									+ "'index': 'not_analyzed',"
									+ "'store': true"
							+ "},"
							+ "'myField': {"
									+ "'type': 'date',"
									+ "'index': 'not_analyzed',"
									+ "'fields': {"
											+ "'myField" + ToElasticsearch.FACET_FIELD_SUFFIX + "': {"
													+ "'type': 'date',"
													+ "'index': 'analyzed'"
											+ "}"
									+ "}"
							+ "}"
					+ "}"
				+ "}"
				);

		thrown.expect(
				isException( ElasticsearchSchemaValidationException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage( "property 'myField', field 'myField" + ToElasticsearch.FACET_FIELD_SUFFIX + "'" )
						.withMessage( "\n\tInvalid value for attribute 'index'. Expected 'NOT_ANALYZED', actual is 'ANALYZED'" )
				.build()
		);

		init( FacetedDateEntity.class );
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
		elasticSearchClient.index( AnalyzedEntity.class ).deleteAndCreate();
		elasticSearchClient.index( AnalyzedEntity.class ).settings( "index.analysis" ).put(
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
		elasticSearchClient.index( AnalyzedEntity.class ).deleteAndCreate();
		elasticSearchClient.index( AnalyzedEntity.class ).settings( "index.analysis" ).put(
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
		elasticSearchClient.index( AnalyzedEntity.class ).deleteAndCreate();
		elasticSearchClient.index( AnalyzedEntity.class ).settings( "index.analysis" ).put(
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
		elasticSearchClient.index( AnalyzedEntity.class ).deleteAndCreate();
		elasticSearchClient.index( AnalyzedEntity.class ).settings( "index.analysis" ).put(
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
		elasticSearchClient.index( AnalyzedEntity.class ).deleteAndCreate();
		elasticSearchClient.index( AnalyzedEntity.class ).settings( "index.analysis" ).put(
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
		elasticSearchClient.index( AnalyzedEntity.class ).deleteAndCreate();
		elasticSearchClient.index( AnalyzedEntity.class ).settings( "index.analysis" ).put(
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
		elasticSearchClient.index( AnalyzedEntity.class ).deleteAndCreate();
		elasticSearchClient.index( AnalyzedEntity.class ).settings( "index.analysis" ).put(
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
		elasticSearchClient.index( AnalyzedEntity.class ).deleteAndCreate();
		elasticSearchClient.index( AnalyzedEntity.class ).settings( "index.analysis" ).put(
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
	public static class SimpleStringEntity {
		@DocumentId
		@Id
		Long id;

		@Field
		String myField;
	}

	@Indexed
	@Entity
	public static class SimpleLenientEntity {
		@DocumentId
		@Id
		Long id;

		@Field(index = Index.NO, store = Store.NO)
		Long myField;
	}

	@Indexed
	@Entity
	public static class FacetedDateEntity {
		@DocumentId
		@Id
		Long id;

		@Field(analyze = Analyze.NO)
		@Facet
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
