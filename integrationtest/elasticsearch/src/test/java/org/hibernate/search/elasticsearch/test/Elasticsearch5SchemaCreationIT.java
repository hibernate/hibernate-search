/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import static org.hibernate.search.test.util.JsonHelper.assertJsonEquals;

import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Norms;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.elasticsearch.cfg.IndexSchemaManagementStrategy;
import org.hibernate.search.elasticsearch.impl.ElasticsearchIndexManager;
import org.hibernate.search.elasticsearch.testutil.TestElasticsearchClient;
import org.hibernate.search.elasticsearch.testutil.junit.SkipBelowElasticsearch50;
import org.hibernate.search.test.SearchInitializationTestBase;
import org.hibernate.search.test.util.ImmutableTestConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests for {@link ElasticsearchIndexManager}'s schema creation feature.
 *
 * @author Yoann Rodiere
 */
@RunWith(Parameterized.class)
@Category(SkipBelowElasticsearch50.class)
public class Elasticsearch5SchemaCreationIT extends SearchInitializationTestBase {

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

	public Elasticsearch5SchemaCreationIT(IndexSchemaManagementStrategy strategy) {
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
	public void dateField() throws Exception {
		elasticSearchClient.index( SimpleDateEntity.class )
				.ensureDoesNotExist().registerForCleanup();

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
							+ "}"
					+ "}"
				+ "}",
				elasticSearchClient.type( SimpleDateEntity.class ).getMapping()
				);
	}

	@Test
	public void booleanField() throws Exception {
		elasticSearchClient.index( SimpleBooleanEntity.class )
				.ensureDoesNotExist().registerForCleanup();

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
	public void keywordField() throws Exception {
		elasticSearchClient.index( SimpleKeywordEntity.class )
				.ensureDoesNotExist().registerForCleanup();

		init( SimpleKeywordEntity.class );

		assertJsonEquals(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'keyword',"
									+ "'store': true"
							+ "},"
							+ "'myField': {"
									+ "'type': 'keyword',"
									+ "'norms': true"
							+ "}"
					+ "}"
				+ "}",
				elasticSearchClient.type( SimpleKeywordEntity.class ).getMapping()
				);
	}

	@Test
	public void textField() throws Exception {
		elasticSearchClient.index( SimpleTextEntity.class )
				.ensureDoesNotExist().registerForCleanup();

		init( SimpleTextEntity.class );

		assertJsonEquals(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'keyword',"
									+ "'store': true"
							+ "},"
							+ "'myField': {"
									+ "'type': 'text'"
							+ "}"
					+ "}"
				+ "}",
				elasticSearchClient.type( SimpleTextEntity.class ).getMapping()
				);
	}

	@Test
	public void textField_noNorms() throws Exception {
		elasticSearchClient.index( NoNormsTextEntity.class )
				.ensureDoesNotExist().registerForCleanup();

		init( NoNormsTextEntity.class );

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
									+ "'norms': false"
							+ "}"
					+ "}"
				+ "}",
				elasticSearchClient.type( NoNormsTextEntity.class ).getMapping()
				);
	}

	@Indexed
	@Entity
	private static class SimpleBooleanEntity {
		@DocumentId
		@Id
		Long id;

		@Field
		Boolean myField;
	}

	@Indexed
	@Entity
	private static class SimpleDateEntity {
		@DocumentId
		@Id
		Long id;

		@Field
		Date myField;
	}

	@Indexed
	@Entity
	private static class SimpleKeywordEntity {
		@DocumentId
		@Id
		Long id;

		@Field(analyze = Analyze.NO)
		String myField;
	}

	@Indexed
	@Entity
	private static class SimpleTextEntity {
		@DocumentId
		@Id
		Long id;

		@Field
		String myField;
	}

	@Indexed
	@Entity
	private static class NoNormsTextEntity {
		@DocumentId
		@Id
		Long id;

		@Field(norms = Norms.NO)
		String myField;
	}

}
