/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import static org.hibernate.search.test.util.impl.ExceptionMatcherBuilder.isException;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchIndexStatus;
import org.hibernate.search.elasticsearch.cfg.IndexSchemaManagementStrategy;
import org.hibernate.search.elasticsearch.impl.ElasticsearchIndexManager;
import org.hibernate.search.elasticsearch.impl.JsonBuilder;
import org.hibernate.search.elasticsearch.testutil.TestElasticsearchClient;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.test.SearchInitializationTestBase;
import org.hibernate.search.test.util.ImmutableTestConfiguration;
import org.hibernate.testing.TestForIssue;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests for {@link ElasticsearchIndexManager}'s behavior with respect
 * to checking that the index has the correct status before starting to work with it.
 *
 * @author Yoann Rodiere
 */
@RunWith(Parameterized.class)
@TestForIssue(jiraKey = "HSEARCH-2456")
public class ElasticsearchIndexStatusCheckIT extends SearchInitializationTestBase {

	@Parameters(name = "With strategy {0}")
	public static Iterable<IndexSchemaManagementStrategy> strategies() {
		// The "NONE" strategy never checks that the index exists.
		return EnumSet.complementOf( EnumSet.of( IndexSchemaManagementStrategy.NONE ) );
	}

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public TestElasticsearchClient elasticSearchClient = new TestElasticsearchClient();

	private IndexSchemaManagementStrategy strategy;

	public ElasticsearchIndexStatusCheckIT(IndexSchemaManagementStrategy strategy) {
		super();
		this.strategy = strategy;
	}

	@Test
	public void indexMissing() throws Exception {
		Assume.assumeFalse( "The strategy " + strategy + " creates an index automatically."
				+ " No point running this test.",
				createsIndex( strategy ) );

		elasticSearchClient.index( SimpleEntity.class ).ensureDoesNotExist();

		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH400050" );

		init( SimpleEntity.class );
	}

	@Test
	public void invalidIndexStatus_creatingIndex() throws Exception {
		Assume.assumeTrue( "The strategy " + strategy + " doesn't creates an index automatically."
				+ " No point running this test.",
				createsIndex( strategy ) );

		// Make sure automatically created indexes will never be green
		elasticSearchClient.template( "yellow_index_because_not_enough_nodes_for_so_many_replicas" )
				.create(
						"*",
						/*
						 * The exact number of replicas we ask for doesn't matter much,
						 * since we're testing with only 1 node (the cluster can't replicate shards)
						 */
						JsonBuilder.object().addProperty( "number_of_replicas", 5 ).build()
				);

		elasticSearchClient.index( SimpleEntity.class ).ensureDoesNotExist();

		thrown.expect(
				isException( SearchException.class )
						.withMessage( "HSEARCH400007" )
				.causedBy( SearchException.class )
						.withMessage( "HSEARCH400024" )
						.withMessage( "100ms" )
				.build()
		);

		init( SimpleEntity.class );
	}

	@Test
	public void invalidIndexStatus_usingPreexistingIndex() throws Exception {
		// Make sure automatically created indexes will never be green
		elasticSearchClient.template( "yellow_index_because_not_enough_nodes_for_so_many_replicas" )
				.create(
						"*",
						/*
						 * The exact number of replicas we ask for doesn't matter much,
						 * since we're testing with only 1 node (the cluster can't replicate shards)
						 */
						JsonBuilder.object().addProperty( "number_of_replicas", 5 ).build()
				);

		elasticSearchClient.index( SimpleEntity.class ).deleteAndCreate();

		thrown.expect(
				isException( SearchException.class )
						.withMessage( "HSEARCH400007" )
				.causedBy( SearchException.class )
						.withMessage( "HSEARCH400024" )
						.withMessage( "100ms" )
				.build()
		);

		init( SimpleEntity.class );
	}

	@Override
	protected void init(Class<?> ... entityClasses) {
		Map<String, Object> settings = new HashMap<>();
		settings.put(
				"hibernate.search.default." + ElasticsearchEnvironment.INDEX_SCHEMA_MANAGEMENT_STRATEGY,
				strategy.getExternalName()
		);
		settings.put(
				"hibernate.search.default." + ElasticsearchEnvironment.INDEX_MANAGEMENT_WAIT_TIMEOUT,
				"100"
		);
		settings.put(
				"hibernate.search.default." + ElasticsearchEnvironment.REQUIRED_INDEX_STATUS,
				ElasticsearchIndexStatus.GREEN.getElasticsearchString()
		);

		init( new ImmutableTestConfiguration( settings, entityClasses ) );
	}

	private boolean createsIndex(IndexSchemaManagementStrategy strategy) {
		return !IndexSchemaManagementStrategy.NONE.equals( strategy )
				&& !IndexSchemaManagementStrategy.VALIDATE.equals( strategy );
	}

	@Indexed
	@Entity
	private static class SimpleEntity {
		@DocumentId
		@Id
		Long id;

		@Field
		String myField;
	}

}
