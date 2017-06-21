/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import static org.hibernate.search.test.util.JsonHelper.assertJsonEquals;
import static org.hibernate.search.test.util.JsonHelper.assertJsonEqualsIgnoringUnknownFields;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.elasticsearch.cfg.IndexSchemaManagementStrategy;
import org.hibernate.search.elasticsearch.impl.ElasticsearchIndexManager;
import org.hibernate.search.elasticsearch.testutil.TestElasticsearchClient;
import org.hibernate.search.test.SearchInitializationTestBase;
import org.hibernate.search.test.util.ImmutableTestConfiguration;
import org.hibernate.testing.TestForIssue;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests for {@link ElasticsearchIndexManager}'s schema creation feature.
 *
 * @author Yoann Rodiere
 */
public class ElasticsearchSchemaCreateStrategyIT extends SearchInitializationTestBase {

	@Rule
	public final TestElasticsearchClient elasticSearchClient = new TestElasticsearchClient();

	@Override
	protected void init(Class<?>... annotatedClasses) {
		Map<String, Object> settings = new HashMap<>();
		settings.put(
				"hibernate.search.default." + ElasticsearchEnvironment.INDEX_SCHEMA_MANAGEMENT_STRATEGY,
				IndexSchemaManagementStrategy.CREATE.getExternalName()
		);
		init( new ImmutableTestConfiguration( settings, annotatedClasses ) );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2789")
	public void alreadyExists() throws Exception {
		elasticSearchClient.index( SimpleStringEntity.class ).deleteAndCreate();

		assertJsonEquals(
				"{ }",
				elasticSearchClient.type( SimpleStringEntity.class ).getMapping()
		);

		init( SimpleStringEntity.class );

		assertJsonEquals(
				"{ }",
				elasticSearchClient.type( SimpleStringEntity.class ).getMapping()
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2789")
	public void doesNotExist() throws Exception {
		elasticSearchClient.index( SimpleStringEntity.class )
				.ensureDoesNotExist().registerForCleanup();

		init( SimpleStringEntity.class );

		// Just check that *something* changed
		// Other test classes check that the changes actually make sense
		assertJsonEqualsIgnoringUnknownFields(
				"{ 'properties': { 'field': { } } }",
				elasticSearchClient.type( SimpleStringEntity.class ).getMapping()
		);
	}

	@Indexed
	@Entity
	private static class SimpleStringEntity {
		@DocumentId
		@Id
		Long id;

		@Field
		String field;
	}

}
