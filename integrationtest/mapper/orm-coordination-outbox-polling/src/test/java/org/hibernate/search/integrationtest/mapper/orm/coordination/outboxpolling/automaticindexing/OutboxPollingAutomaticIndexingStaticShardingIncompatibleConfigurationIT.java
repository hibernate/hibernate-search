/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling.automaticindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.Collections;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.cfg.Environment;
import org.hibernate.search.engine.backend.analysis.AnalyzerNames;
import org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling.testsupport.util.TestFailureHandler;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.CoordinationStrategyExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.rule.ExpectedLog4jLog;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test for static sharding where some nodes are configured in an incompatible way.
 */
@TestForIssue(jiraKey = "HSEARCH-4140")
public class OutboxPollingAutomaticIndexingStaticShardingIncompatibleConfigurationIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock )
			.coordinationStrategy( CoordinationStrategyExpectations.outboxPolling() );

	@Rule
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	private void setup(String hbm2ddlAction, TestFailureHandler failureHandler, int totalShardCount,
			List<Integer> assignedShardIndices) {
		backendMock.expectSchema( IndexedEntity.NAME, b -> b
				.field( "text", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ) )
		);

		OrmSetupHelper.SetupContext context = ormSetupHelper.start()
				.withProperty( Environment.HBM2DDL_AUTO, hbm2ddlAction )
				.withProperty( "hibernate.search.background_failure_handler", failureHandler )
				.withProperty( "hibernate.search.coordination.event_processor.shards.total_count", totalShardCount )
				.withProperty( "hibernate.search.coordination.event_processor.shards.assigned", assignedShardIndices );

		context.setup( IndexedEntity.class );
	}

	@Test
	public void conflictingTotalShardCount() throws InterruptedException {
		TestFailureHandler sessionFactory1FailureHandler = new TestFailureHandler();
		TestFailureHandler sessionFactory2FailureHandler = new TestFailureHandler();

		setup( "create-drop", sessionFactory1FailureHandler, 1, Collections.singletonList( 0 ) );
		setup( "none", sessionFactory2FailureHandler, 2, Collections.singletonList( 1 ) );
		backendMock.verifyExpectationsMet();

		await().untilAsserted( () -> {
			assertThat( sessionFactory1FailureHandler.genericFailures ).isNotEmpty();
			assertThat( sessionFactory2FailureHandler.genericFailures ).isNotEmpty();
		} );

		String[] expectedContent = new String[] {
				"is statically assigned to shard ",
				"this conflicts with agent '",
				"' which expects ",
				" shards.",
				"This can be a temporary situation caused by some application instances being forcibly stopped and replacements being spun up",
				"consider adjusting the configuration or switching to dynamic sharding.",
				"Registered agents:"
		};
		assertThat( sessionFactory1FailureHandler.genericFailures )
				.allSatisfy( failureContext -> {
					assertThat( failureContext.throwable() )
							.hasMessageContainingAll( expectedContent );
				} );
		assertThat( sessionFactory2FailureHandler.genericFailures )
				.allSatisfy( failureContext -> {
					assertThat( failureContext.throwable() )
							.hasMessageContainingAll( expectedContent );
				} );

		// Also check that we don't flood the logs because of repeated pulses every few milliseconds
		Thread.sleep( 1000 );
		assertThat( sessionFactory1FailureHandler.genericFailures ).hasSizeLessThan( 3 );
		assertThat( sessionFactory2FailureHandler.genericFailures ).hasSizeLessThan( 3 );
	}

	@Test
	public void conflictingAssignedShardIndex() throws InterruptedException {
		TestFailureHandler sessionFactory1FailureHandler = new TestFailureHandler();
		TestFailureHandler sessionFactory2FailureHandler = new TestFailureHandler();

		setup( "create-drop", sessionFactory1FailureHandler, 2, Collections.singletonList( 0 ) );
		setup( "none", sessionFactory2FailureHandler, 2, Collections.singletonList( 0 ) );
		backendMock.verifyExpectationsMet();

		await().untilAsserted( () -> {
			assertThat( sessionFactory1FailureHandler.genericFailures ).isNotEmpty();
			assertThat( sessionFactory2FailureHandler.genericFailures ).isNotEmpty();
		} );

		String[] expectedContent = new String[] {
				"is statically assigned to shard ",
				"this conflicts with agent '",
				"' which is also assigned to that shard.",
				"This can be a temporary situation caused by some application instances being forcibly stopped and replacements being spun up",
				"consider adjusting the configuration or switching to dynamic sharding.",
				"Registered agents:"
		};
		assertThat( sessionFactory1FailureHandler.genericFailures )
				.allSatisfy( failureContext -> {
					assertThat( failureContext.throwable() )
							.hasMessageContainingAll( expectedContent );
				} );
		assertThat( sessionFactory2FailureHandler.genericFailures )
				.allSatisfy( failureContext -> {
					assertThat( failureContext.throwable() )
							.hasMessageContainingAll( expectedContent );
				} );

		// Also check that we don't flood the logs because of repeated pulses every few milliseconds
		Thread.sleep( 1000 );
		assertThat( sessionFactory1FailureHandler.genericFailures ).hasSizeLessThan( 3 );
		assertThat( sessionFactory2FailureHandler.genericFailures ).hasSizeLessThan( 3 );
	}

	@Entity(name = IndexedEntity.NAME)
	@Indexed
	public static class IndexedEntity {

		static final String NAME = "IndexedEntity";

		@Id
		private Integer id;
		@FullTextField
		private String text;

		public IndexedEntity() {
		}

		public IndexedEntity(Integer id, String text) {
			this.id = id;
			this.text = text;
		}

		public Integer getId() {
			return id;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}

}
