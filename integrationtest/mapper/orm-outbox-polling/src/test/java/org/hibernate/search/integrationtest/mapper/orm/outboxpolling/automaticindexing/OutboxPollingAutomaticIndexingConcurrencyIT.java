/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.outboxpolling.automaticindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.engine.backend.analysis.AnalyzerNames;
import org.hibernate.search.integrationtest.mapper.orm.outboxpolling.testsupport.util.OutboxPollingTestUtils;
import org.hibernate.search.integrationtest.mapper.orm.outboxpolling.testsupport.util.TestFailureHandler;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.CoordinationStrategyExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.rule.ExpectedLog4jLog;

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

	@Rule
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

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

		OutboxPollingTestUtils.awaitAllAgentsRunningInOneCluster( with( sessionFactories.get( 0 ) ), TOTAL_SHARD_COUNT );
	}

	private SessionFactory setup(String hbm2ddlAction) {
		backendMock.expectSchema( IndexedEntity.NAME, b -> b
				.field( "text", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ) ) );

		OrmSetupHelper.SetupContext context = ormSetupHelper.start()
				.withProperty( Environment.HBM2DDL_AUTO, hbm2ddlAction )
				.withProperty( "hibernate.search.background_failure_handler", failureHandler );

		return context.setup( IndexedEntity.class );
	}

	// Check that database (transaction) deadlocks, if they happen,
	// do not prevent Hibernate Search from doing its job
	// and do not cause errors to be reported.
	// Ideally these deadlocks should not happen at all,
	// and indeed for most databases they will not due to how our queries are designed,
	// but on some databases (CockroachDB) that's very hard (impossible?) to achieve.
	@Test
	public void resilientToTransactionDeadlocks() {
		SessionFactory sessionFactory = sessionFactories.get( 0 );

		Dialect dialect = sessionFactory.unwrap( SessionFactoryImplementor.class ).getJdbcServices().getDialect();
		// CockroachDB seems to trigger deadlocks whatever we do.
		// For other DBs, our queries should be optimized so as not to trigger any deadlock.
		if ( !( dialect instanceof CockroachDialect ) ) {
			// We're using two different ways to match the logs,
			// in case the ID or message gets updated and we forget to update this test.
			logged.expectMessage( "HSEARCH850034" ).never();
			logged.expectMessage( "failed", "retrieve event", "lock" )
					.never();
		}

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
