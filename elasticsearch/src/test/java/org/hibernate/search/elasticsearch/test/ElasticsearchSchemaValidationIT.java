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

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.elasticsearch.cfg.IndexSchemaManagementStrategy;
import org.hibernate.search.elasticsearch.impl.ElasticsearchIndexManager;
import org.hibernate.search.elasticsearch.schema.impl.ElasticsearchSchemaValidationException;
import org.hibernate.search.elasticsearch.testutil.TestElasticsearchClient;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.test.SearchInitializationTestBase;
import org.hibernate.search.test.util.ImmutableTestConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests for {@link ElasticsearchIndexManager}'s schema validation feature.
 *
 * @author Yoann Rodiere
 */
public class ElasticsearchSchemaValidationIT extends SearchInitializationTestBase {

	private static final String VALIDATION_FAILED_MESSAGE_ID = "HSEARCH400033";
	private static final String MISSING_MAPPINGS_MESSAGE_ID = "HSEARCH400035";
	private static final String MISSING_MAPPING_MESSAGE_ID = "HSEARCH400036";
	private static final String INVALID_MAPPING_MESSAGE_ID = "HSEARCH400037";
	private static final String MISSING_PROPERTY_MESSAGE_ID = "HSEARCH400038";
	private static final String INVALID_PROPERTY_MESSAGE_ID = "HSEARCH400039";
	private static final String INVALID_ATTRIBUTE_MESSAGE_ID = "HSEARCH400040";
	private static final String INVALID_OUTPUT_FORMAT_MESSAGE_ID = "HSEARCH400041";
	private static final String INVALID_INPUT_FORMAT_MESSAGE_ID = "HSEARCH400042";

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
		elasticSearchClient.deleteAndCreateIndex( SimpleDateEntity.class );
		elasticSearchClient.putMapping(
				SimpleDateEntity.class,
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
		elasticSearchClient.deleteAndCreateIndex( SimpleBooleanEntity.class );
		elasticSearchClient.putMapping(
				SimpleBooleanEntity.class,
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

		init( SimpleDateEntity.class, SimpleBooleanEntity.class );

		// If we get here, it means validation passed (no exception was thrown)
	}

	@Test
	public void mappings_missing() throws Exception {
		thrown.expect(
				isException( SearchException.class )
					.withMessage( MISSING_MAPPINGS_MESSAGE_ID )
				.build()

		);

		init( SimpleDateEntity.class );
	}

	@Test
	public void mapping_missing() throws Exception {
		elasticSearchClient.deleteAndCreateIndex( SimpleDateEntity.class );

		thrown.expect(
				isException( SearchException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
				.causedBy( ElasticsearchSchemaValidationException.class )
						.withMessage( MISSING_MAPPING_MESSAGE_ID )
				.build()

		);

		init( SimpleDateEntity.class );
	}

	@Test
	public void rootMapping_attribute_missing() throws Exception {
		elasticSearchClient.deleteAndCreateIndex( SimpleDateEntity.class );
		elasticSearchClient.putMapping(
				SimpleDateEntity.class,
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
				isException( SearchException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
				.causedBy( ElasticsearchSchemaValidationException.class )
						.withMessage( INVALID_MAPPING_MESSAGE_ID )
				.causedBy( ElasticsearchSchemaValidationException.class )
						.withMessage( INVALID_ATTRIBUTE_MESSAGE_ID )
						.withMessage( "dynamic" )
				.build()
		);

		init( SimpleDateEntity.class );
	}

	@Test
	public void rootMapping_attribute_dynamic_invalid() throws Exception {
		elasticSearchClient.deleteAndCreateIndex( SimpleDateEntity.class );
		elasticSearchClient.putMapping(
				SimpleDateEntity.class,
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
				isException( SearchException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
				.causedBy( ElasticsearchSchemaValidationException.class )
						.withMessage( INVALID_MAPPING_MESSAGE_ID )
				.causedBy( ElasticsearchSchemaValidationException.class )
						.withMessage( INVALID_ATTRIBUTE_MESSAGE_ID )
						.withMessage( "dynamic" )
				.build()
		);

		init( SimpleDateEntity.class );
	}

	@Test
	public void properties_missing() throws Exception {
		elasticSearchClient.deleteAndCreateIndex( SimpleDateEntity.class );
		elasticSearchClient.putMapping(
				SimpleDateEntity.class,
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
				isException( SearchException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
				.causedBy( ElasticsearchSchemaValidationException.class )
						.withMessage( INVALID_MAPPING_MESSAGE_ID )
				.causedBy( ElasticsearchSchemaValidationException.class )
						.withMessage( MISSING_PROPERTY_MESSAGE_ID )
				.build()
		);

		init( SimpleDateEntity.class );
	}

	@Test
	public void property_missing() throws Exception {
		elasticSearchClient.deleteAndCreateIndex( SimpleDateEntity.class );
		elasticSearchClient.putMapping(
				SimpleDateEntity.class,
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
				isException( SearchException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
				.causedBy( ElasticsearchSchemaValidationException.class )
						.withMessage( INVALID_MAPPING_MESSAGE_ID )
				.causedBy( ElasticsearchSchemaValidationException.class )
						.withMessage( MISSING_PROPERTY_MESSAGE_ID )
				.build()
		);

		init( SimpleDateEntity.class );
	}

	@Test
	public void property_attribute_missing() throws Exception {
		elasticSearchClient.deleteAndCreateIndex( SimpleDateEntity.class );
		elasticSearchClient.putMapping(
				SimpleDateEntity.class,
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
				isException( SearchException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
				.causedBy( ElasticsearchSchemaValidationException.class )
						.withMessage( INVALID_MAPPING_MESSAGE_ID )
				.causedBy( ElasticsearchSchemaValidationException.class )
						.withMessage( INVALID_PROPERTY_MESSAGE_ID )
						.withMessage( "myField" )
				.causedBy( ElasticsearchSchemaValidationException.class )
						.withMessage( INVALID_ATTRIBUTE_MESSAGE_ID )
						.withMessage( "type" )
				.build()
		);

		init( SimpleDateEntity.class );
	}

	@Test
	public void property_attribute_invalid() throws Exception {
		elasticSearchClient.deleteAndCreateIndex( SimpleDateEntity.class );
		elasticSearchClient.putMapping(
				SimpleDateEntity.class,
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
				isException( SearchException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
				.causedBy( ElasticsearchSchemaValidationException.class )
						.withMessage( INVALID_MAPPING_MESSAGE_ID )
				.causedBy( ElasticsearchSchemaValidationException.class )
						.withMessage( INVALID_PROPERTY_MESSAGE_ID )
						.withMessage( "myField" )
				.causedBy( ElasticsearchSchemaValidationException.class )
						.withMessage( INVALID_ATTRIBUTE_MESSAGE_ID )
						.withMessage( "index" )
				.build()
		);

		init( SimpleDateEntity.class );
	}

	@Test
	public void property_format_invalidOutputFormat() throws Exception {
		elasticSearchClient.deleteAndCreateIndex( SimpleDateEntity.class );
		elasticSearchClient.putMapping(
				SimpleDateEntity.class,
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
				isException( SearchException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
				.causedBy( ElasticsearchSchemaValidationException.class )
						.withMessage( INVALID_MAPPING_MESSAGE_ID )
				.causedBy( ElasticsearchSchemaValidationException.class )
						.withMessage( INVALID_PROPERTY_MESSAGE_ID )
						.withMessage( "myField" )
				.causedBy( ElasticsearchSchemaValidationException.class )
						.withMessage( INVALID_OUTPUT_FORMAT_MESSAGE_ID )
						.withMessage( "format" )
				.build()
		);

		init( SimpleDateEntity.class );
	}

	@Test
	public void property_format_missingInputFormat() throws Exception {
		elasticSearchClient.deleteAndCreateIndex( SimpleDateEntity.class );
		elasticSearchClient.putMapping(
				SimpleDateEntity.class,
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
				isException( SearchException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
				.causedBy( ElasticsearchSchemaValidationException.class )
						.withMessage( INVALID_MAPPING_MESSAGE_ID )
				.causedBy( ElasticsearchSchemaValidationException.class )
						.withMessage( INVALID_PROPERTY_MESSAGE_ID )
						.withMessage( "myField" )
				.causedBy( ElasticsearchSchemaValidationException.class )
						.withMessage( INVALID_INPUT_FORMAT_MESSAGE_ID )
						.withMessage( "format" )
				.build()
		);

		init( SimpleDateEntity.class );
	}

	@Test
	public void property_format_unexpectedInputFormat() throws Exception {
		elasticSearchClient.deleteAndCreateIndex( SimpleDateEntity.class );
		elasticSearchClient.putMapping(
				SimpleDateEntity.class,
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
				isException( SearchException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
				.causedBy( ElasticsearchSchemaValidationException.class )
						.withMessage( INVALID_MAPPING_MESSAGE_ID )
				.causedBy( ElasticsearchSchemaValidationException.class )
						.withMessage( INVALID_PROPERTY_MESSAGE_ID )
						.withMessage( "myField" )
				.causedBy( ElasticsearchSchemaValidationException.class )
						.withMessage( INVALID_INPUT_FORMAT_MESSAGE_ID )
						.withMessage( "format" )
				.build()
		);

		init( SimpleDateEntity.class );
	}

	/**
	 * Tests that mappings that are more powerful than requested will pass validation.
	 */
	@Test
	public void property_attribute_leniency() throws Exception {
		elasticSearchClient.deleteAndCreateIndex( SimpleLenientEntity.class );
		elasticSearchClient.putMapping(
				SimpleLenientEntity.class,
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
	public static class SimpleLenientEntity {
		@DocumentId
		@Id
		Long id;

		@Field(index = Index.NO, store = Store.NO)
		Long myField;
	}

}
