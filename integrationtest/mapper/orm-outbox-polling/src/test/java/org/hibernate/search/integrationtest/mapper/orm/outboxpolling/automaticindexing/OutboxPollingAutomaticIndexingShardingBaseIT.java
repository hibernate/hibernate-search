/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.outboxpolling.automaticindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.Arrays;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.analysis.AnalyzerNames;
import org.hibernate.search.integrationtest.mapper.orm.outboxpolling.testsupport.util.OutboxPollingTestUtils;
import org.hibernate.search.integrationtest.mapper.orm.outboxpolling.testsupport.util.PerSessionFactoryIndexingCountHelper;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.CoordinationStrategyExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.extension.StaticCounters;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.assertj.core.data.Percentage;

/**
 * Base tests for sharding with the outbox-polling coordination strategy:
 * checks that all events are handled by one and only one node
 * (if they were not, we would see missing or duplicate indexing work executions).
 */
@TestForIssue(jiraKey = { "HSEARCH-4141", "HSEARCH-4140" })
class OutboxPollingAutomaticIndexingShardingBaseIT {

	public static List<? extends Arguments> params() {
		return Arrays.asList(
				Arguments.of( false, 2 ),
				Arguments.of( true, 2 ),
				Arguments.of( false, 10 ),
				Arguments.of( true, 10 )
		);
	}

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public OrmSetupHelper ormSetupHelper =
			OrmSetupHelper.withCoordinationStrategy( CoordinationStrategyExpectations.outboxPolling() )
					.withBackendMock( backendMock );

	@RegisterExtension
	public StaticCounters counters = StaticCounters.create();

	private final PerSessionFactoryIndexingCountHelper indexingCountHelper =
			new PerSessionFactoryIndexingCountHelper( counters );

	public void setup(boolean isStatic, int totalShardCount) {
		for ( int i = 0; i < totalShardCount; i++ ) {
			setup( isStatic, totalShardCount,
					// Avoid session factories stepping on each other's feet.
					i == 0 ? "create-drop" : "none",
					i
			);
		}

		backendMock.verifyExpectationsMet();

		OutboxPollingTestUtils.awaitAllAgentsRunningInOneCluster( with( indexingCountHelper.sessionFactory( 0 ) ),
				totalShardCount );
	}

	private void setup(boolean isStatic, int totalShardCount, String hbm2ddlAction, int assignedShardIndex) {
		backendMock.expectSchema( IndexedEntity.NAME, b -> b
				.field( "text", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ) )
				.with( indexingCountHelper::expectSchema )
		);
		backendMock.expectSchema( IndexedAndContainingEntity.NAME, b -> b
				.field( "text", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ) )
				.with( indexingCountHelper::expectSchema )
				.objectField( "contained", b2 -> b2
						.field( "text", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ) ) )
		);

		ormSetupHelper.start()
				.withProperty( org.hibernate.cfg.Environment.HBM2DDL_AUTO, hbm2ddlAction )
				.with( indexingCountHelper::bind )
				.with( ctx -> {
					if ( isStatic ) {
						return ctx
								.withProperty( "hibernate.search.coordination.event_processor.shards.total_count",
										totalShardCount )
								.withProperty( "hibernate.search.coordination.event_processor.shards.assigned",
										String.valueOf( assignedShardIndex ) );
					}
					else {
						return ctx;
					}
				} )
				.setup( IndexedEntity.class, IndexedAndContainingEntity.class, ContainedEntity.class );
	}

	@ParameterizedTest(name = "static = {0}, totalShardCount = {1}")
	@MethodSource("params")
	void uniqueWorkAcrossSessionFactories_insertUpdateDelete_indexed(boolean isStatic, int totalShardCount) {
		setup( isStatic, totalShardCount );
		SessionFactory sessionFactory = indexingCountHelper.sessionFactory( 0 );

		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity = new IndexedEntity( 1, "initial" );
			session.persist( entity );

			backendMock.expectWorks( IndexedEntity.NAME )
					.add( "1", b -> b.field( "text", "initial" ) );
		} );
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity = session.getReference( IndexedEntity.class, 1 );
			entity.setText( "updated" );

			backendMock.expectWorks( IndexedEntity.NAME )
					.addOrUpdate( "1", b -> b.field( "text", "updated" ) );
		} );
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity = session.getReference( IndexedEntity.class, 1 );
			session.remove( entity );

			backendMock.expectWorks( IndexedEntity.NAME )
					.delete( "1" );
		} );
		backendMock.verifyExpectationsMet();

		indexingCountHelper.indexingCounts().assertAcrossAllSessionFactories().isEqualTo( 2 );
	}

	@ParameterizedTest(name = "static = {0}, totalShardCount = {1}")
	@MethodSource("params")
	void uniqueWorkAcrossSessionFactories_insertUpdateDelete_contained(boolean isStatic, int totalShardCount) {
		setup( isStatic, totalShardCount );
		SessionFactory sessionFactory = indexingCountHelper.sessionFactory( 0 );

		with( sessionFactory ).runInTransaction( session -> {
			IndexedAndContainingEntity containing = new IndexedAndContainingEntity( 1, "initial" );
			ContainedEntity contained = new ContainedEntity( 2, "initial" );
			containing.setContained( contained );
			contained.setContaining( containing );
			session.persist( containing );
			session.persist( contained );

			backendMock.expectWorks( IndexedAndContainingEntity.NAME )
					.add( "1", b -> b.field( "text", "initial" )
							.objectField( "contained", b2 -> b2
									.field( "text", "initial" ) ) );
		} );
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runInTransaction( session -> {
			ContainedEntity contained = session.getReference( ContainedEntity.class, 2 );
			contained.setText( "updated" );

			backendMock.expectWorks( IndexedAndContainingEntity.NAME )
					.addOrUpdate( "1", b -> b.field( "text", "initial" )
							.objectField( "contained", b2 -> b2
									.field( "text", "updated" ) ) );
		} );
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runInTransaction( session -> {
			IndexedAndContainingEntity containing = session.getReference( IndexedAndContainingEntity.class, 1 );
			ContainedEntity contained = containing.getContained();
			containing.setContained( null );
			session.remove( contained );

			backendMock.expectWorks( IndexedAndContainingEntity.NAME )
					.addOrUpdate( "1", b -> b.field( "text", "initial" ) );
		} );
		backendMock.verifyExpectationsMet();

		indexingCountHelper.indexingCounts().assertAcrossAllSessionFactories().isEqualTo( 3 );
	}

	@ParameterizedTest(name = "static = {0}, totalShardCount = {1}")
	@MethodSource("params")
	void uniformWorkDistribution_insertUpdateDelete_indexed(boolean isStatic, int totalShardCount) {
		setup( isStatic, totalShardCount );
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
		// The workload must be spread uniformly (with some tolerance)
		indexingCountHelper.indexingCounts().assertForEachSessionFactory()
				.allSatisfy( count -> assertThat( count )
						.isCloseTo( entityCount / totalShardCount, Percentage.withPercentage( 25 ) ) );

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
		// The workload must be spread uniformly (with some tolerance)
		indexingCountHelper.indexingCounts().assertForEachSessionFactory()
				.allSatisfy( count -> assertThat( count )
						.isCloseTo( entityCount / totalShardCount, Percentage.withPercentage( 25 ) ) );
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

	@Entity(name = IndexedAndContainingEntity.NAME)
	@Indexed
	public static class IndexedAndContainingEntity {

		static final String NAME = "IndexedAndContainingEntity";

		@Id
		private Integer id;
		@FullTextField
		private String text;
		@OneToOne(mappedBy = "containing")
		@IndexedEmbedded(includePaths = "text")
		private ContainedEntity contained;

		public IndexedAndContainingEntity() {
		}

		public IndexedAndContainingEntity(Integer id, String text) {
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

		public ContainedEntity getContained() {
			return contained;
		}

		public void setContained(
				ContainedEntity contained) {
			this.contained = contained;
		}

	}

	@Entity(name = ContainedEntity.NAME)
	public static class ContainedEntity {

		static final String NAME = "ContainedEntity";

		@Id
		private Integer id;
		@FullTextField
		private String text;
		@OneToOne
		private IndexedAndContainingEntity containing;

		public ContainedEntity() {
		}

		public ContainedEntity(Integer id, String text) {
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

		public IndexedAndContainingEntity getContaining() {
			return containing;
		}

		public void setContaining(
				IndexedAndContainingEntity containing) {
			this.containing = containing;
		}
	}

}
