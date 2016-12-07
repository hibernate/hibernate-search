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
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Facet;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.elasticsearch.cfg.IndexSchemaManagementStrategy;
import org.hibernate.search.elasticsearch.impl.ElasticsearchIndexManager;
import org.hibernate.search.elasticsearch.impl.ToElasticsearch;
import org.hibernate.search.elasticsearch.schema.impl.ElasticsearchSchemaValidationException;
import org.hibernate.search.elasticsearch.testutil.TestElasticsearchClient;
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
		elasticSearchClient.deleteAndCreateIndex( FacetedDateEntity.class );
		elasticSearchClient.putMapping(
				FacetedDateEntity.class,
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

		init( SimpleDateEntity.class, SimpleBooleanEntity.class, FacetedDateEntity.class );

		// If we get here, it means validation passed (no exception was thrown)
	}

	@Test
	public void mapping_missing() throws Exception {
		elasticSearchClient.deleteAndCreateIndex( SimpleDateEntity.class );

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
				isException( ElasticsearchSchemaValidationException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage( "\n\tInvalid value for attribute 'dynamic'. Expected 'STRICT', actual is 'null'" )
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
				isException( ElasticsearchSchemaValidationException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage( "\n\tInvalid value for attribute 'dynamic'. Expected 'STRICT', actual is 'FALSE'" )
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
				isException( ElasticsearchSchemaValidationException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage( "property 'myField'" )
						.withMessage( "\n\tInvalid value for attribute 'index'. Expected 'NOT_ANALYZED', actual is 'ANALYZED'" )
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
				isException( ElasticsearchSchemaValidationException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage( "property 'myField'" )
						.withMessage( "\n\tThe output format (the first format in the 'format' attribute) is invalid. Expected 'strict_date_optional_time', actual is 'epoch_millis'" )
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

	@Test
	public void multipleErrors_singleIndexManagers() throws Exception {
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
									+ "'type': 'string'"
							+ "}"
					+ "}"
				+ "}"
				);

		thrown.expect(
				isException( ElasticsearchSchemaValidationException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage(
								"\nIndex 'org.hibernate.search.elasticsearch.test.elasticsearchschemavalidationit$simpledateentity', mapping 'org.hibernate.search.elasticsearch.test.ElasticsearchSchemaValidationIT$SimpleDateEntity':"
								+ "\n\tInvalid value for attribute 'dynamic'. Expected 'STRICT', actual is 'FALSE'"
								+ "\nIndex 'org.hibernate.search.elasticsearch.test.elasticsearchschemavalidationit$simpledateentity', mapping 'org.hibernate.search.elasticsearch.test.ElasticsearchSchemaValidationIT$SimpleDateEntity', property 'myField':"
								+ "\n\tInvalid value for attribute 'type'. Expected 'DATE', actual is 'STRING'"
						)
				.build()
		);

		init( SimpleDateEntity.class );
	}

	@Test
	public void multipleErrors_multipleIndexManagers() throws Exception {
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
									+ "'type': 'string'"
							+ "}"
					+ "}"
				+ "}"
				);
		elasticSearchClient.deleteAndCreateIndex( SimpleBooleanEntity.class );
		elasticSearchClient.putMapping(
				SimpleBooleanEntity.class,
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
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage(
								"\nIndex 'org.hibernate.search.elasticsearch.test.elasticsearchschemavalidationit$simplebooleanentity', mapping 'org.hibernate.search.elasticsearch.test.ElasticsearchSchemaValidationIT$SimpleBooleanEntity':"
								+ "\n\tInvalid value for attribute 'dynamic'. Expected 'STRICT', actual is 'FALSE'"
						)
				.withSuppressed(
						isException( ElasticsearchSchemaValidationException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage(
								"\nIndex 'org.hibernate.search.elasticsearch.test.elasticsearchschemavalidationit$simpledateentity', mapping 'org.hibernate.search.elasticsearch.test.ElasticsearchSchemaValidationIT$SimpleDateEntity':"
								+ "\n\tInvalid value for attribute 'dynamic'. Expected 'STRICT', actual is 'FALSE'"
								+ "\nIndex 'org.hibernate.search.elasticsearch.test.elasticsearchschemavalidationit$simpledateentity', mapping 'org.hibernate.search.elasticsearch.test.ElasticsearchSchemaValidationIT$SimpleDateEntity', property 'myField':"
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
		elasticSearchClient.deleteAndCreateIndex( FacetedDateEntity.class );
		elasticSearchClient.putMapping(
				FacetedDateEntity.class,
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
		elasticSearchClient.deleteAndCreateIndex( FacetedDateEntity.class );
		elasticSearchClient.putMapping(
				FacetedDateEntity.class,
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
		elasticSearchClient.deleteAndCreateIndex( FacetedDateEntity.class );
		elasticSearchClient.putMapping(
				FacetedDateEntity.class,
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

}
