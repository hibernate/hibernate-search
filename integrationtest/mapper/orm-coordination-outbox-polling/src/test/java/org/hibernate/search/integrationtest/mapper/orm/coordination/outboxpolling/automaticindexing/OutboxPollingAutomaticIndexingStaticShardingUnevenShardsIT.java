/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling.automaticindexing;

import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Environment;
import org.hibernate.search.engine.backend.analysis.AnalyzerNames;
import org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling.testsupport.util.PerSessionFactoryIndexingCountHelper;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.CoordinationStrategyExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.rule.StaticCounters;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.assertj.core.data.Percentage;

/**
 * Test for static sharding with the outbox-polling coordination strategy
 * where each nodes are assigned a different number of shards (some 1, some 0, some more than 1, ...).
 */
@TestForIssue(jiraKey = "HSEARCH-4141")
public class OutboxPollingAutomaticIndexingStaticShardingUnevenShardsIT {

	public static final int TOTAL_SHARD_COUNT = 7;

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock )
			.coordinationStrategy( CoordinationStrategyExpectations.outboxPolling() );

	@Rule
	public StaticCounters counters = new StaticCounters();

	private final PerSessionFactoryIndexingCountHelper indexingCountHelper =
			new PerSessionFactoryIndexingCountHelper( counters );

	@Before
	public void setup() {
		setup(
				"create-drop",
				true, Collections.singletonList( 0 ) // 1 shard
		);
		setup(
				"none", // Avoid session factories stepping on each other's feet.
				// Make sure that nodes can disable processing,
				// and if so that they don't need to configure sharding.
				false, null // 0 shard
		);
		setup(
				"none",
				true, Arrays.asList( 1, 3 ) // 2 shards
		);
		setup(
				"none",
				true, Arrays.asList( 2, 4, 5, 6 ) // 4 shards
		);

		backendMock.verifyExpectationsMet();
	}

	private void setup(String hbm2ddlAction, boolean processingEnabled, List<Integer> assignedShardIndices) {
		backendMock.expectSchema( IndexedEntity.NAME, b -> b
				.with( indexingCountHelper::expectSchema )
				.field( "text", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ) )
		);

		OrmSetupHelper.SetupContext context = ormSetupHelper.start()
				.withProperty( Environment.HBM2DDL_AUTO, hbm2ddlAction )
				.with( indexingCountHelper::bind );

		if ( processingEnabled ) {
			context = context
					.withProperty( "hibernate.search.coordination.event_processor.shards.total_count", TOTAL_SHARD_COUNT )
					.withProperty( "hibernate.search.coordination.event_processor.shards.assigned", assignedShardIndices );
		}
		else {
			// If processing is disabled, sharding is irrelevant: we don't need to configure it.
			context = context.withProperty( "hibernate.search.coordination.event_processor.enabled", "false" );
		}

		context.setup( IndexedEntity.class );
	}

	@Test
	public void workDistribution() {
		SessionFactory sessionFactory = indexingCountHelper.sessionFactory( 0 );

		int entityCount = 1000;

		// A single big insert transaction
		with( sessionFactory ).runInTransaction( session -> {
			for ( int i = 0; i < entityCount; i++ ) {
				IndexedEntity entity = new IndexedEntity( i, "initial" );
				session.persist( entity );

				backendMock.expectWorks( IndexedEntity.NAME )
						.add( String.valueOf( i ), b -> b.field( "text", "initial" ) );
			}
		} );
		backendMock.verifyExpectationsMet();
		// All works must be executed exactly once
		indexingCountHelper.indexingCounts().assertAcrossAllSessionFactories().isEqualTo( entityCount );
		// The workload must be spread in accordance with the number of shards (with some tolerance)
		indexingCountHelper.indexingCounts().assertForSessionFactory( 0 )
				// 1 shard
				.isCloseTo( 1 * entityCount / TOTAL_SHARD_COUNT, Percentage.withPercentage( 25 ) );
		indexingCountHelper.indexingCounts().assertForSessionFactory( 1 )
				// 0 shard
				.isEqualTo( 0 );
		indexingCountHelper.indexingCounts().assertForSessionFactory( 2 )
				// 2 shards
				.isCloseTo( 2 * entityCount / TOTAL_SHARD_COUNT, Percentage.withPercentage( 25 ) );
		indexingCountHelper.indexingCounts().assertForSessionFactory( 3 )
				// 4 shards
				.isCloseTo( 4 * entityCount / TOTAL_SHARD_COUNT, Percentage.withPercentage( 25 ) );

		counters.clear();

		// Many small update transactions
		int batchSize = 100;
		for ( int i = 0; i < entityCount; i += batchSize ) {
			int idStart = i;
			int idEnd = Math.min( i + batchSize, entityCount );
			with( sessionFactory ).runInTransaction( session -> {
				for ( int j = idStart; j < idEnd; j++ ) {
					IndexedEntity entity = session.getReference( IndexedEntity.class, j );
					entity.setText( "updated" );

					backendMock.expectWorks( IndexedEntity.NAME )
							.addOrUpdate( String.valueOf( j ), b -> b.field( "text", "updated" ) );
				}
			} );
		}
		backendMock.verifyExpectationsMet();
		// All works must be executed exactly once
		indexingCountHelper.indexingCounts().assertAcrossAllSessionFactories().isEqualTo( entityCount );
		// The workload must be spread in accordance with the number of shards (with some tolerance)
		indexingCountHelper.indexingCounts().assertForSessionFactory( 0 )
				// 1 shard
				.isCloseTo( 1 * entityCount / TOTAL_SHARD_COUNT, Percentage.withPercentage( 25 ) );
		indexingCountHelper.indexingCounts().assertForSessionFactory( 1 )
				// 0 shard
				.isEqualTo( 0 );
		indexingCountHelper.indexingCounts().assertForSessionFactory( 2 )
				// 2 shards
				.isCloseTo( 2 * entityCount / TOTAL_SHARD_COUNT, Percentage.withPercentage( 25 ) );
		indexingCountHelper.indexingCounts().assertForSessionFactory( 3 )
				// 4 shards
				.isCloseTo( 4 * entityCount / TOTAL_SHARD_COUNT, Percentage.withPercentage( 25 ) );
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
