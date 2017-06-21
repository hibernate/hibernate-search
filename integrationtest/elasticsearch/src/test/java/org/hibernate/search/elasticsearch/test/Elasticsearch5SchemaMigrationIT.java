/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import static org.hibernate.search.test.util.JsonHelper.assertJsonEquals;
import static org.hibernate.search.test.util.impl.ExceptionMatcherBuilder.isException;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.elasticsearch.cfg.IndexSchemaManagementStrategy;
import org.hibernate.search.elasticsearch.impl.ElasticsearchIndexManager;
import org.hibernate.search.elasticsearch.testutil.TestElasticsearchClient;
import org.hibernate.search.elasticsearch.testutil.junit.SkipBelowElasticsearch50;
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
@Category(SkipBelowElasticsearch50.class)
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
		elasticSearchClient.index( SimpleTextEntity.class ).deleteAndCreate(
				"index.analysis", generateAnalysisSettings()
				);
		elasticSearchClient.type( SimpleTextEntity.class ).putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'keyword',"
									+ "'index': 'true',"
									+ "'store': true"
							+ "},"
							+ "'defaultAnalyzer': {"
									+ "'type': 'text'"
							+ "},"
							+ "'nonDefaultAnalyzer': {"
									+ "'type': 'text',"
									+ "'analyzer': 'customAnalyzer'"
							+ "},"
							+ "'normalizer': {"
									+ "'type': 'text',"
									+ "'analyzer': 'customNormalizer'"
							+ "}"
					+ "}"
				+ "}"
				);

		init( SimpleDateEntity.class, SimpleBooleanEntity.class, SimpleTextEntity.class );

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
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'keyword',"
									+ "'store': true"
							+ "},"
							+ "'defaultAnalyzer': {"
									+ "'type': 'text'"
							+ "},"
							+ "'nonDefaultAnalyzer': {"
									+ "'type': 'text',"
									+ "'analyzer': 'customAnalyzer'"
							+ "},"
							+ "'normalizer': {"
									+ "'type': 'text',"
									+ "'analyzer': 'customNormalizer'"
							+ "}"
					+ "}"
				+ "}",
				elasticSearchClient.type( SimpleTextEntity.class ).getMapping()
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
		elasticSearchClient.index( SimpleTextEntity.class ).deleteAndCreate(
				"index.analysis", generateAnalysisSettings()
				);
		elasticSearchClient.type( SimpleTextEntity.class ).putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'keyword',"
									+ "'index': true,"
									+ "'store': true"
							+ "},"
							+ "'defaultAnalyzer': {"
									+ "'type': 'text'"
							+ "},"
							+ "'nonDefaultAnalyzer': {"
									+ "'type': 'text',"
									+ "'analyzer': 'standard'" // Invalid
							+ "},"
							+ "'normalizer': {"
									+ "'type': 'text',"
									+ "'analyzer': 'customNormalizer'"
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

		init( SimpleTextEntity.class );
	}

	@Test
	public void property_attribute_invalid_conflictingNormalizer() throws Exception {
		elasticSearchClient.index( SimpleTextEntity.class ).deleteAndCreate(
				"index.analysis", generateAnalysisSettings()
				);
		elasticSearchClient.type( SimpleTextEntity.class ).putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'keyword',"
									+ "'index': true,"
									+ "'store': true"
							+ "},"
							+ "'defaultAnalyzer': {"
									+ "'type': 'text'"
							+ "},"
							+ "'nonDefaultAnalyzer': {"
									+ "'type': 'text',"
									+ "'analyzer': 'customAnalyzer'"
							+ "},"
							+ "'normalizer': {"
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

		init( SimpleTextEntity.class );
	}

	private String generateAnalysisSettings() throws IOException {
		return "{"
					+ "'analyzer': {"
							+ "'customAnalyzer': {"
									+ "'tokenizer': 'whitespace'"
							+ "},"
							+ "'customNormalizer': {"
									+ "'tokenizer': 'keyword'"
							+ "}"
					+ "}"
				+ "}";
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
	public static class SimpleTextEntity {
		@DocumentId
		@Id
		Long id;

		@Field
		String defaultAnalyzer;

		@Field(analyzer = @Analyzer(definition = "customAnalyzer"))
		String nonDefaultAnalyzer;

		@Field(analyzer = @Analyzer(definition = "customNormalizer"))
		String normalizer;
	}

}
