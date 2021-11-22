/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling.automaticindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;
import static org.awaitility.Awaitility.await;
import static org.hibernate.search.mapper.orm.coordination.outboxpolling.cfg.impl.HibernateOrmMapperOutboxPollingImplSettings.CoordinationRadicals.EVENT_PROCESSOR_AGENT_REPOSITORY_PROVIDER;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.withinTransaction;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Environment;
import org.hibernate.search.engine.backend.analysis.AnalyzerNames;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.CoordinationStrategyExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.rule.StaticCounters;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test of automatic rebalancing for dynamic sharding with the outbox-polling coordination strategy.
 */
@TestForIssue(jiraKey = "HSEARCH-4140")
public class OutboxPollingAutomaticIndexingDynamicShardingRebalancingIT {

	// Use a low polling interval, pulse interval and batch size when testing rebalancing
	// so that we can observe rebalancing on a reasonably small timescale.
	private static final int POLLING_INTERVAL = 10;
	private static final int PULSE_INTERVAL = 10;
	private static final int BATCH_SIZE = 5;
	// Don't use a low expiration: we can't set it too low,
	// or agents will be likely to expire while processing a single batch.
	private static final int PULSE_EXPIRATION = 5000;

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock )
			.coordinationStrategy( CoordinationStrategyExpectations.outboxPolling() );

	@Rule
	public StaticCounters counters = new StaticCounters();

	private final PerSessionFactoryIndexingCountHelper indexingCountHelper =
			new PerSessionFactoryIndexingCountHelper( counters );

	private final List<DisconnectionSimulatingAgentRepositoryProvider> disconnectionSimulatingAgentRepositoryProviders =
			new ArrayList<>();

	public void setup() {
		setup( "create-drop" );
		setup( "none" );
		setup( "none" );

		backendMock.verifyExpectationsMet();

		OutboxPollingTestUtils.awaitAllAgentsRunningInOneCluster( indexingCountHelper.sessionFactory( 0 ), 3 );
	}

	private void setup(String hbm2ddlAction) {
		backendMock.expectSchema( IndexedEntity.NAME, b -> b
				.field( "text", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ) )
				.with( indexingCountHelper::expectSchema )
		);

		DisconnectionSimulatingAgentRepositoryProvider disconnectionSimulatingAgentRepositoryProvider = new DisconnectionSimulatingAgentRepositoryProvider();
		disconnectionSimulatingAgentRepositoryProviders.add( disconnectionSimulatingAgentRepositoryProvider );

		OrmSetupHelper.SetupContext context = ormSetupHelper.start()
				.withProperty( Environment.HBM2DDL_AUTO, hbm2ddlAction )
				.with( indexingCountHelper::bind )
				.withProperty( "hibernate.search.coordination.event_processor.polling_interval", POLLING_INTERVAL )
				.withProperty( "hibernate.search.coordination.event_processor.pulse_expiration", PULSE_EXPIRATION )
				.withProperty( "hibernate.search.coordination.event_processor.pulse_interval", PULSE_INTERVAL )
				.withProperty( "hibernate.search.coordination.event_processor.batch_size", BATCH_SIZE )
				.withProperty( "hibernate.search.coordination." + EVENT_PROCESSOR_AGENT_REPOSITORY_PROVIDER,
						disconnectionSimulatingAgentRepositoryProvider );

		context.setup( IndexedEntity.class );
	}

	@Test
	public void agentLeft() {
		setup();

		SessionFactory sessionFactory = indexingCountHelper.sessionFactory( 0 );

		int entityCount = 3000;
		int initialShardCount = 3;

		withinTransaction( sessionFactory, session -> {
			for ( int i = 0; i < entityCount; i++ ) {
				IndexedEntity entity = new IndexedEntity( i, "initial" );
				session.save( entity );

				backendMock.expectWorks( IndexedEntity.NAME )
						.add( String.valueOf( i ), b -> b.field( "text", "initial" ) );
			}
		} );

		// Stop the last factory as soon as it's processed at least one entity
		await()
				.pollInterval( 1, TimeUnit.MILLISECONDS )
				.untilAsserted( () -> indexingCountHelper.assertIndexingCountForSessionFactory( 2 ).isNotZero() );
		indexingCountHelper.sessionFactory( 2 ).close();

		backendMock.verifyExpectationsMet();
		// All works must be executed exactly once
		indexingCountHelper.assertIndexingCountAcrossAllSessionFactories().isEqualTo( entityCount );
		// We expect factory 2 to not have processed many events before it left the cluster,
		// but we can't predict exactly how many it will have processed:
		// how many events are processed during a pulse interval is a matter of performance.
		// Here we'll just expect it processed less than half of the normally assigned events.
		// This assertion could fail on a very well-performing machine where all events
		// are processed before the pulse; if that happens, either lower the pulse interval
		// or raise the number of entities.
		indexingCountHelper.assertIndexingCountForSessionFactory( 2 )
				.isLessThan( entityCount / initialShardCount / 2 );
		// The workload must be spread evenly over the other factories in accordance with
		// the number of shards (with some tolerance)
		int remainingShardCount = initialShardCount - 1;
		int entityCountNotProcessedByFactory2 = entityCount - indexingCountHelper.indexingCountForSessionFactory( 2 );
		indexingCountHelper.assertIndexingCountForSessionFactory( 0 )
				.isCloseTo( entityCountNotProcessedByFactory2 / remainingShardCount, withinPercentage( 25 ) );
		indexingCountHelper.assertIndexingCountForSessionFactory( 1 )
				.isCloseTo( entityCountNotProcessedByFactory2 / remainingShardCount, withinPercentage( 25 ) );

		counters.clear();
	}

	@Test
	public void agentExpired() {
		setup();

		SessionFactory sessionFactory = indexingCountHelper.sessionFactory( 0 );

		int entityCount = 3000;
		int initialShardCount = 3;

		withinTransaction( sessionFactory, session -> {
			for ( int i = 0; i < entityCount; i++ ) {
				IndexedEntity entity = new IndexedEntity( i, "initial" );
				session.save( entity );

				backendMock.expectWorks( IndexedEntity.NAME )
						.add( String.valueOf( i ), b -> b.field( "text", "initial" ) );
			}
		} );

		// Prevent the last factory from accessing the database as soon as it's processed at least one entity,
		// so that its registration ultimately expires
		await()
				.pollInterval( 1, TimeUnit.MILLISECONDS )
				.untilAsserted( () -> indexingCountHelper.assertIndexingCountForSessionFactory( 2 ).isNotZero() );
		disconnectionSimulatingAgentRepositoryProviders.get( 2 ).setPreventPulse( false );

		backendMock.verifyExpectationsMet();
		// All works must be executed exactly once
		indexingCountHelper.assertIndexingCountAcrossAllSessionFactories().isEqualTo( entityCount );
		// We expect factory 2 to not have processed many events before it was disconnected,
		// but we can't predict exactly how many it will have processed; see the similar comment in agentLeft().
		indexingCountHelper.assertIndexingCountForSessionFactory( 2 )
				.isLessThan( entityCount / initialShardCount / 2 );
		// The workload will most likely not be spread evenly over the other factories,
		// because they will have processed most of their own events by the time
		// factory 2 gets evicted and rebalancing happens,
		// and then rebalancing will actually assign part of the shard from factory 1 to factory 0 (so, almost nothing left)
		// and will assign the whole shard from factory 2 to factory 1 (so, most of the work still to do).
		// That's why we don't check the indexing count of individual factories.

		counters.clear();
	}

	@Test
	public void agentJoined() {
		setup();

		SessionFactory sessionFactory = indexingCountHelper.sessionFactory( 0 );

		int entityCount = 3000;
		int initialShardCount = 3;

		withinTransaction( sessionFactory, session -> {
			for ( int i = 0; i < entityCount; i++ ) {
				IndexedEntity entity = new IndexedEntity( i, "initial" );
				session.save( entity );

				backendMock.expectWorks( IndexedEntity.NAME )
						.add( String.valueOf( i ), b -> b.field( "text", "initial" ) );
			}
		} );

		// Start a new factory as soon as all others have processed at least one entity
		await()
				.pollInterval( 1, TimeUnit.MILLISECONDS )
				.untilAsserted( () -> indexingCountHelper.assertIndexingCountForEachSessionFactory()
						.allSatisfy( c -> assertThat( c ).isNotZero() ) );
		setup( "none" );
		int newShardCount = initialShardCount + 1;

		backendMock.verifyExpectationsMet();
		// All works must be executed exactly once
		indexingCountHelper.assertIndexingCountAcrossAllSessionFactories().isEqualTo( entityCount );
		// We expect the factory 3 to have "missed" a few events processed by another factory before factory 3 joined,
		// but we can't predict exactly how many it will have missed; see the similar comment in agentLeft().
		indexingCountHelper.assertIndexingCountForSessionFactory( 3 )
				.isGreaterThan( entityCount / newShardCount / 2 );
		// The workload must be spread evenly over the other factories in accordance with
		// the number of shards (with some tolerance)
		int entityCountNotProcessedByFactory3 = entityCount - indexingCountHelper.indexingCountForSessionFactory( 3 );
		indexingCountHelper.assertIndexingCountForSessionFactory( 0 )
				.isCloseTo( entityCountNotProcessedByFactory3 / initialShardCount, withinPercentage( 25 ) );
		indexingCountHelper.assertIndexingCountForSessionFactory( 1 )
				.isCloseTo( entityCountNotProcessedByFactory3 / initialShardCount, withinPercentage( 25 ) );
		indexingCountHelper.assertIndexingCountForSessionFactory( 2 )
				.isCloseTo( entityCountNotProcessedByFactory3 / initialShardCount, withinPercentage( 25 ) );
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
