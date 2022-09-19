/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling.automaticindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling.testsupport.util.OutboxPollingTestUtils.awaitAllAgentsRunningInOneCluster;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Environment;
import org.hibernate.search.engine.backend.analysis.AnalyzerNames;
import org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling.testsupport.util.TestFailureHandler;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.CoordinationStrategyExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests highly concurrent background processing of events,
 * and checks in particular that it does not result in processing errors and/or deadlocks.
 * <p>
 * This used to fail on Microsoft SQL Server, in particular, because of its lock escalation mechanism.
 */
@TestForIssue(jiraKey = "HSEARCH-4141")
public class OutboxPollingAutomaticIndexingConcurrencyIT {

	public static final int TOTAL_SHARD_COUNT = 23;
	public static final int ENTITY_COUNT = 2000;
	// Experimentation showed that larger batch sizes tend to reproduce the deadlock more reliably.
	public static final int ENTITY_UPDATE_BATCH_SIZE = 500;

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock )
			.coordinationStrategy( CoordinationStrategyExpectations.outboxPolling() );

	private final TestFailureHandler failureHandler = new TestFailureHandler();

	private final List<SessionFactory> sessionFactories = new ArrayList<>();

	@Before
	public void setup() {
		sessionFactories.add( setup( "create-drop" ) );
		for ( int i = 1; i < TOTAL_SHARD_COUNT; i++ ) {
			// Avoid session factories stepping on each other's feet: use Action.NONE here.
			sessionFactories.add( setup( "none" ) );
		}

		backendMock.verifyExpectationsMet();

		awaitAllAgentsRunningInOneCluster( with( sessionFactories.get( 0 ) ), TOTAL_SHARD_COUNT );
	}

	private SessionFactory setup(String hbm2ddlAction) {
		backendMock.expectSchema( IndexedEntity.NAME, b -> b
				.field( "text", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ) ) );

		OrmSetupHelper.SetupContext context = ormSetupHelper.start()
				.withProperty( Environment.HBM2DDL_AUTO, hbm2ddlAction )
				.withProperty( "hibernate.search.background_failure_handler", failureHandler );

		return context.setup( IndexedEntity.class );
	}

	// Note: we're talking about *database* deadlocks here.
	// We're not protecting against infinite execution because we expect
	// the deadlock to be detected by the database,
	// which would abort the transaction,
	// which would cause a failure in the background thread and eventually would fail the test.
	@Test
	public void noDeadlock() {
		SessionFactory sessionFactory = sessionFactories.get( 0 );

		for ( int i = 0; i < ENTITY_COUNT; i += ENTITY_UPDATE_BATCH_SIZE ) {
			int idStart = i;
			int idEnd = Math.min( i + ENTITY_UPDATE_BATCH_SIZE, ENTITY_COUNT );
			with( sessionFactory ).runInTransaction( session -> {
				for ( int j = idStart; j < idEnd; j++ ) {
					IndexedEntity entity = new IndexedEntity( j, "initial" );
					session.persist( entity );

					backendMock.expectWorks( IndexedEntity.NAME )
							.add( String.valueOf( j ), b -> b.field( "text", "initial" ) );
				}
			} );
		}
		backendMock.verifyExpectationsMet();

		assertThat( failureHandler.genericFailures ).isEmpty();
		assertThat( failureHandler.entityFailures ).isEmpty();
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
