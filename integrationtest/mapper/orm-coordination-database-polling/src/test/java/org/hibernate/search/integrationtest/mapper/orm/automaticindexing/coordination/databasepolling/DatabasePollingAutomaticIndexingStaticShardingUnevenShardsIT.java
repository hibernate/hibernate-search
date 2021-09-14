/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.coordination.databasepolling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.withinTransaction;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Environment;
import org.hibernate.search.engine.backend.analysis.AnalyzerNames;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.TypeBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.TypeBinding;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.CoordinationStrategyExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.rule.StaticCounters;
import org.hibernate.tool.schema.Action;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.assertj.core.api.AbstractIntegerAssert;
import org.assertj.core.data.Percentage;

/**
 * Test for static sharding with the database-polling coordination strategy
 * where each nodes are assigned a different number of shards (some 1, some 0, some more than 1, ...).
 */
@TestForIssue(jiraKey = "HSEARCH-4141")
public class DatabasePollingAutomaticIndexingStaticShardingUnevenShardsIT {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public static final int TOTAL_SHARD_COUNT = 7;

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock )
			.coordinationStrategy( CoordinationStrategyExpectations.outboxPolling() );

	@Rule
	public StaticCounters counters = new StaticCounters();

	private final List<StaticCounters.Key> sessionFactoryCounterKeys = new ArrayList<>();
	private final List<SessionFactory> sessionFactories = new ArrayList<>();

	@Before
	public void setup() {
		sessionFactories.add( setup(
				Action.CREATE_DROP,
				true, Collections.singletonList( 0 ) // 1 shard
		) );
		sessionFactories.add( setup(
				Action.NONE, // Avoid session factories getting in each other's feet.
				// Make sure that nodes can disable processing,
				// and if so that they don't need to configure sharding.
				false, null // 0 shard
		) );
		sessionFactories.add( setup(
				Action.NONE,
				true, Arrays.asList( 1, 3 ) // 2 shards
		) );
		sessionFactories.add( setup(
				Action.NONE,
				true, Arrays.asList( 2, 4, 5, 6 ) // 4 shards
		) );

		backendMock.verifyExpectationsMet();
	}

	private SessionFactory setup(Action action, boolean processingEnabled, List<Integer> assignedShardIndices) {
		backendMock.expectSchema( IndexedEntity.NAME, b -> b
				.field( "text", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ) )
				.field( PerSessionFactoryIndexingTracingBridge.FAKE_FIELD_NAME, PerSessionFactoryIndexingTracingBridge.FAKE_FIELD_TYPE )
		);

		StaticCounters.Key counterKey = StaticCounters.createKey();
		sessionFactoryCounterKeys.add( counterKey );

		OrmSetupHelper.SetupContext context = ormSetupHelper.start()
				.withProperty( Environment.HBM2DDL_AUTO, action )
				.withProperty( PerSessionFactoryIndexingTracingBridge.SESSION_FACTORY_COUNTER_KEY_PROPERTY, counterKey );

		if ( processingEnabled ) {
			context = context.withProperty( "hibernate.search.coordination.shards.static", "true" )
					.withProperty( "hibernate.search.coordination.shards.total_count", TOTAL_SHARD_COUNT )
					.withProperty( "hibernate.search.coordination.shards.assigned", assignedShardIndices );
		}
		else {
			// If processing is disabled, sharding is irrelevant: we don't need to configure it.
			context = context.withProperty( "hibernate.search.coordination.processors.indexing.enabled", "false" );
		}

		return context.setup( IndexedEntity.class );
	}

	@Test
	public void workDistribution() {
		SessionFactory sessionFactory = sessionFactories.get( 0 );

		int entityCount = 1000;

		// A single big insert transaction
		withinTransaction( sessionFactory, session -> {
			for ( int i = 0; i < entityCount; i++ ) {
				IndexedEntity entity = new IndexedEntity( i, "initial" );
				session.save( entity );

				backendMock.expectWorks( IndexedEntity.NAME )
						.add( String.valueOf( i ), b -> b.field( "text", "initial" ) );
			}
		} );
		backendMock.verifyExpectationsMet();
		// All works must be executed exactly once
		assertIndexingCountAcrossAllSessionFactories().isEqualTo( entityCount );
		// The workload must be spread in accordance with the number of shards (with some tolerance)
		assertIndexingCountForSessionFactory( 0 )
				// 1 shard
				.isCloseTo( 1 * entityCount / TOTAL_SHARD_COUNT, Percentage.withPercentage( 25 ) );
		assertIndexingCountForSessionFactory( 1 )
				// 0 shard
				.isEqualTo( 0 );
		assertIndexingCountForSessionFactory( 2 )
				// 2 shards
				.isCloseTo( 2 * entityCount / TOTAL_SHARD_COUNT, Percentage.withPercentage( 25 ) );
		assertIndexingCountForSessionFactory( 3 )
				// 4 shards
				.isCloseTo( 4 * entityCount / TOTAL_SHARD_COUNT, Percentage.withPercentage( 25 ) );

		counters.clear();

		// Many small update transactions
		int batchSize = 100;
		for ( int i = 0; i < entityCount; i += batchSize ) {
			int idStart = i;
			int idEnd = Math.min( i + batchSize, entityCount );
			withinTransaction( sessionFactory, session -> {
				for ( int j = idStart; j < idEnd ; j++ ) {
					IndexedEntity entity = session.load( IndexedEntity.class, j );
					entity.setText( "updated" );

					backendMock.expectWorks( IndexedEntity.NAME )
							.addOrUpdate( String.valueOf( j ), b -> b.field( "text", "updated" ) );
				}
			} );
		}
		backendMock.verifyExpectationsMet();
		// All works must be executed exactly once
		assertIndexingCountAcrossAllSessionFactories().isEqualTo( entityCount );
		// The workload must be spread in accordance with the number of shards (with some tolerance)
		assertIndexingCountForSessionFactory( 0 )
				// 1 shard
				.isCloseTo( 1 * entityCount / TOTAL_SHARD_COUNT, Percentage.withPercentage( 25 ) );
		assertIndexingCountForSessionFactory( 1 )
				// 0 shard
				.isEqualTo( 0 );
		assertIndexingCountForSessionFactory( 2 )
				// 2 shards
				.isCloseTo( 2 * entityCount / TOTAL_SHARD_COUNT, Percentage.withPercentage( 25 ) );
		assertIndexingCountForSessionFactory( 3 )
				// 4 shards
				.isCloseTo( 4 * entityCount / TOTAL_SHARD_COUNT, Percentage.withPercentage( 25 ) );
	}

	private AbstractIntegerAssert<?> assertIndexingCountAcrossAllSessionFactories() {
		int sum = 0;
		for ( StaticCounters.Key counterKey : sessionFactoryCounterKeys ) {
			sum += counters.get( counterKey );
		}
		log.debugf( "Count of indexing operations across all session factories: %s", sum );
		return assertThat( sum )
				.as( "Count of indexing operations across all session factories" );
	}

	private AbstractIntegerAssert<?> assertIndexingCountForSessionFactory(int i) {
		int count = counters.get( sessionFactoryCounterKeys.get( i ) );
		log.debugf( "Count of indexing operations for session factory %d: %s", i, count );
		return assertThat( count )
				.as( "Count of indexing operations for session factory %d", i );
	}

	@Entity(name = IndexedEntity.NAME)
	@Indexed
	@TypeBinding(binder = @TypeBinderRef(type = PerSessionFactoryIndexingTracingBridge.Binder.class))
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
