/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.outboxpolling.automaticindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import org.hibernate.SessionFactory;
import org.hibernate.search.integrationtest.mapper.orm.outboxpolling.testsupport.util.OutboxEventFilter;
import org.hibernate.search.integrationtest.mapper.orm.outboxpolling.testsupport.util.TestingOutboxPollingInternalConfigurer;
import org.hibernate.search.mapper.orm.outboxpolling.cfg.impl.HibernateOrmMapperOutboxPollingImplSettings;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.CoordinationStrategyExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Extensive tests with edge cases for automatic indexing with the outbox-polling strategy.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OutboxPollingAutomaticIndexingEdgeCasesIT {

	private static final OutboxEventFilter eventFilter = new OutboxEventFilter();

	@RegisterExtension
	public static BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public static OrmSetupHelper ormSetupHelper =
			OrmSetupHelper.withCoordinationStrategy( CoordinationStrategyExpectations.outboxPolling() )
					.withBackendMock( backendMock );
	private SessionFactory sessionFactory;

	@BeforeAll
	void setup() {
		backendMock.expectSchema( IndexedEntity.NAME, b -> b
				.field( "text", String.class )
				.objectField( "contained", b2 -> b2
						.multiValued( true )
						.field( "text", String.class ) ) );
		backendMock.expectSchema( IndexedAndContainedEntity.NAME, b -> b
				.field( "text", String.class ) );

		sessionFactory = ormSetupHelper.start()
				.withProperty(
						HibernateOrmMapperOutboxPollingImplSettings.COORDINATION_INTERNAL_CONFIGURER,
						new TestingOutboxPollingInternalConfigurer().outboxEventFilter( eventFilter )
				)
				.withAnnotatedTypes( IndexedEntity.class, IndexedAndContainedEntity.class )
				.dataClearing( config -> config.clearOrder( IndexedAndContainedEntity.class, IndexedEntity.class ) )
				.setup();
	}

	@BeforeEach
	void resetFilter() {
		eventFilter.reset();
		// Disable the filter by default: only some of the tests actually need it.
		eventFilter.showAllEvents();
	}

	@Test
	void multipleChangesSameTransaction() {
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity( 1, "initialValue I" );
			session.persist( entity1 );

			IndexedEntity entity2 = new IndexedEntity( 2, "initialValue I" );
			session.persist( entity2 );

			IndexedEntity entity3 = new IndexedEntity( 3, "initialValue I" );
			session.persist( entity3 );

			entity1.setText( "initialValue II" );
			entity2.setText( "initialValue II" );
			entity3.setText( "initialValue II" );
			session.flush();

			entity1.setText( "initialValue III" );
			entity2.setText( "initialValue III" );
			entity3.setText( "initialValue III" );
			session.flush();

			backendMock.expectWorks( IndexedEntity.NAME )
					.add( "1", b -> b
							.field( "text", "initialValue III" )
					)
					.add( "2", b -> b
							.field( "text", "initialValue III" )
					)
					.add( "3", b -> b
							.field( "text", "initialValue III" )
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void massiveInsert() {
		for ( int i = 0; i < 5; i++ ) {
			int finalI = i;
			with( sessionFactory ).runInTransaction( session -> {
				BackendMock.DocumentWorkCallListContext context = backendMock.expectWorks( IndexedEntity.NAME );

				for ( int j = 0; j < 500; j++ ) {
					int id = finalI * 500 + j;

					IndexedEntity entity = new IndexedEntity( id, "indexed value: " + id );
					session.persist( entity );

					if ( j % 25 == 0 ) {
						session.flush();
						session.clear();
					}

					context.add( id + "", b -> b.field( "text", "indexed value: " + id ) );
				}
			} );
			backendMock.verifyExpectationsMet();
		}
	}

	@Test
	void addIndexedAndContained_addAndUpdateEventsProcessedInDifferentBatches() {
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity containing = new IndexedEntity( 1, "initialValue" );
			session.persist( containing );
			backendMock.expectWorks( IndexedEntity.NAME )
					.add( "1", b -> b
							.field( "text", "initialValue" ) );
		} );
		backendMock.verifyExpectationsMet();

		eventFilter.hideAllEvents();

		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity containing = session.getReference( IndexedEntity.class, 1 );
			IndexedAndContainedEntity contained = new IndexedAndContainedEntity( 2, "initialValue" );
			containing.getContained().add( contained );
			contained.setContaining( containing );
			session.persist( contained );

			backendMock.expectWorks( IndexedEntity.NAME )
					.addOrUpdate( "1", b -> b
							.field( "text", "initialValue" )
							.objectField( "contained", b2 -> b2
									.field( "text", "initialValue" ) ) );
			backendMock.expectWorks( IndexedAndContainedEntity.NAME )
					.add( "2", b -> b
							.field( "text", "initialValue" ) );
		} );

		// Make events visible one by one, so that they are processed in separate batches.
		List<UUID> eventIds = with( sessionFactory ).applyInTransaction( eventFilter::findOutboxEventIdsNoFilter );
		assertThat( eventIds ).hasSize( 2 );
		for ( UUID eventId : eventIds ) {
			eventFilter.showOnlyEvents( Collections.singletonList( eventId ) );
			eventFilter.awaitUntilNoMoreVisibleEvents( sessionFactory );
		}

		// If everything goes well, the above will have executed exactly one add work for "contained"
		// and one addOrUpdate work for "containing".
		// What we want to avoid here is that the "contained created" event triggers
		// reindexing of "containing", which would be wrong because "containing"
		// will already be reindexed when the "containing updated" event is processed.
		// If that happens, there will be a duplicate "addOrUpdate" event and this assertion will fail.
		backendMock.verifyExpectationsMet();
	}


	@Entity(name = IndexedEntity.NAME)
	@Indexed
	public static class IndexedEntity {
		static final String NAME = "Indexed";

		@Id
		private Integer id;
		@KeywordField
		private String text;
		@OneToMany(mappedBy = "containing")
		@IndexedEmbedded
		private List<IndexedAndContainedEntity> contained;

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

		public List<IndexedAndContainedEntity> getContained() {
			return contained;
		}
	}

	@Entity(name = IndexedAndContainedEntity.NAME)
	@Indexed
	public static class IndexedAndContainedEntity {
		static final String NAME = "IndexedAndContained";

		@Id
		private Integer id;
		@KeywordField
		private String text;
		@OneToOne
		private IndexedEntity containing;

		public IndexedAndContainedEntity() {
		}

		public IndexedAndContainedEntity(Integer id, String text) {
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

		public IndexedEntity getContaining() {
			return containing;
		}

		public void setContaining(IndexedEntity containing) {
			this.containing = containing;
		}
	}

}
