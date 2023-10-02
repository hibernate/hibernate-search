/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.outboxpolling.automaticindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.integrationtest.mapper.orm.outboxpolling.testsupport.util.OutboxEventFilter;
import org.hibernate.search.integrationtest.mapper.orm.outboxpolling.testsupport.util.TestingOutboxPollingInternalConfigurer;
import org.hibernate.search.mapper.orm.outboxpolling.cfg.impl.HibernateOrmMapperOutboxPollingImplSettings;
import org.hibernate.search.mapper.orm.outboxpolling.event.impl.OutboxEvent;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.StubDocumentNode;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.CoordinationStrategyExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class OutboxPollingAutomaticIndexingOutOfOrderIdsIT {

	private final OutboxEventFilter eventFilter = new OutboxEventFilter();

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public OrmSetupHelper ormSetupHelper =
			OrmSetupHelper.withCoordinationStrategy( CoordinationStrategyExpectations.outboxPolling() )
					.withBackendMock( backendMock );

	private SessionFactory sessionFactory;

	@BeforeEach
	void before() {
		backendMock.expectAnySchema( IndexedEntity.INDEX )
				.expectAnySchema( RoutedIndexedEntity.NAME );
		sessionFactory = ormSetupHelper.start()
				.withProperty( HibernateOrmMapperOutboxPollingImplSettings.COORDINATION_INTERNAL_CONFIGURER,
						new TestingOutboxPollingInternalConfigurer().outboxEventFilter( eventFilter ) )
				// use timebase uuids to get predictable sorting order
				.withProperty( "hibernate.search.coordination.entity.mapping.outboxevent.uuid_gen_strategy", "time" )
				// see HSEARCH-4749, as some DBs (MSSQL) might use a nonstring representation of UUID we want to force it
				// in this case to make row manipulation easier:
				.withProperty( "hibernate.search.coordination.entity.mapping.outboxevent.uuid_type", "CHAR" )
				.setup( IndexedEntity.class, RoutedIndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void processCreateUpdateDelete() {
		// An entity is created, updated, then deleted in separate transactions,
		// but the delete event has ID 1, the update event has ID 2, and the add event has ID 3.

		int id = 1;
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity = new IndexedEntity();
			entity.setId( id );
			entity.setIndexedField( "value for the field" );
			session.persist( entity );
		} );

		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity = session.getReference( IndexedEntity.class, id );
			entity.setIndexedField( "another value for the field" );
			session.merge( entity );
		} );

		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity = session.getReference( IndexedEntity.class, id );
			session.remove( entity );
		} );

		with( sessionFactory ).runNoTransaction( session -> {
			List<OutboxEvent> events = eventFilter.findOutboxEventsNoFilter( session );
			assertThat( events ).hasSize( 3 );
			// Correct order when ordered by id (you'll have to trust me on that)
			// add
			OutboxPollingAutomaticIndexingEventSendingIT.verifyOutboxEntry( events.get( 0 ), IndexedEntity.INDEX, "1", null );
			// update
			OutboxPollingAutomaticIndexingEventSendingIT.verifyOutboxEntry( events.get( 1 ), IndexedEntity.INDEX, "1", null );
			// delete
			OutboxPollingAutomaticIndexingEventSendingIT.verifyOutboxEntry( events.get( 2 ), IndexedEntity.INDEX, "1", null );
		} );

		with( sessionFactory ).runInTransaction( session -> {
			// Swap the IDs of event 1 (add) and 3 (delete)
			swapOutboxTableRowIdAndCreatedValues( session, 0, 2 );
		} );

		with( sessionFactory ).runNoTransaction( session -> {
			List<OutboxEvent> events = eventFilter.findOutboxEventsNoFilter( session );
			assertThat( events ).hasSize( 3 );
			// Out-of-order when ordered by id (you'll have to trust me on that)
			// delete
			OutboxPollingAutomaticIndexingEventSendingIT.verifyOutboxEntry( events.get( 0 ), IndexedEntity.INDEX, "1", null );
			// update
			OutboxPollingAutomaticIndexingEventSendingIT.verifyOutboxEntry( events.get( 1 ), IndexedEntity.INDEX, "1", null );
			// add
			OutboxPollingAutomaticIndexingEventSendingIT.verifyOutboxEntry( events.get( 2 ), IndexedEntity.INDEX, "1", null );
		} );

		// Only a delete work is expected to be executed by the time the outbox events are processed;
		// that's because we don't trust events to tell us whether the document already exists in the index.
		// We don't trust the events for this because they can arrive in the wrong order
		// (and that's the case here).
		backendMock.expectWorks( IndexedEntity.INDEX )
				.delete( "1" );

		// The events were hidden until now, to ensure they were not processed in separate batches.
		// Make them visible to Hibernate Search now.
		eventFilter.showAllEventsUpToNow( sessionFactory );

		eventFilter.awaitUntilNoMoreVisibleEvents( sessionFactory );

		backendMock.verifyExpectationsMet();
	}

	@Test
	void processDeleteRecreate_rightOrder() {
		// An entity is deleted, then re-created in separate transactions.

		int id = 1;
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity = new IndexedEntity();
			entity.setId( id );
			entity.setIndexedField( "value for the field" );
			session.persist( entity );
		} );

		backendMock.expectWorks( IndexedEntity.INDEX )
				.add( "1", b -> b.field( "indexedField", "value for the field" ) );
		eventFilter.showAllEventsUpToNow( sessionFactory );
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity = session.getReference( IndexedEntity.class, id );
			session.remove( entity );
		} );
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity = new IndexedEntity();
			entity.setId( id );
			entity.setIndexedField( "value for the field" );
			session.persist( entity );
		} );

		backendMock.expectWorks( IndexedEntity.INDEX )
				.addOrUpdate( "1", b -> b.field( "indexedField", "value for the field" ) );
		eventFilter.showAllEventsUpToNow( sessionFactory );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void processDeleteRecreate_outOfOrder() {
		// An entity is deleted, then re-created in separate transactions,
		// but the add event has ID 1, and the delete event has ID 2.

		int id = 1;
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity = new IndexedEntity();
			entity.setId( id );
			entity.setIndexedField( "value for the field" );
			session.persist( entity );
		} );

		backendMock.expectWorks( IndexedEntity.INDEX )
				.add( "1", b -> b.field( "indexedField", "value for the field" ) );
		eventFilter.showAllEventsUpToNow( sessionFactory );
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity = session.getReference( IndexedEntity.class, id );
			session.remove( entity );
		} );

		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity = new IndexedEntity();
			entity.setId( id );
			entity.setIndexedField( "value for the field" );
			session.persist( entity );
		} );

		with( sessionFactory ).runNoTransaction( session -> {
			List<OutboxEvent> events = eventFilter.findOutboxEventsNoFilter( session );
			assertThat( events ).hasSize( 2 );
			// Correct order when ordered by id (you'll have to trust me on that)
			// delete
			OutboxPollingAutomaticIndexingEventSendingIT.verifyOutboxEntry( events.get( 0 ), IndexedEntity.INDEX, "1", null );
			// add
			OutboxPollingAutomaticIndexingEventSendingIT.verifyOutboxEntry( events.get( 1 ), IndexedEntity.INDEX, "1", null );
		} );

		with( sessionFactory ).runInTransaction( session -> {
			// Swap the IDs of event 2 (delete) and 3 (add)
			swapOutboxTableRowIdAndCreatedValues( session, 0, 1 );
		} );

		with( sessionFactory ).runNoTransaction( session -> {
			List<OutboxEvent> events = eventFilter.findOutboxEventsNoFilter( session );
			assertThat( events ).hasSize( 2 );
			// Out-of-order when ordered by id (you'll have to trust me on that)
			// add
			OutboxPollingAutomaticIndexingEventSendingIT.verifyOutboxEntry( events.get( 0 ), IndexedEntity.INDEX, "1", null );
			// delete
			OutboxPollingAutomaticIndexingEventSendingIT.verifyOutboxEntry( events.get( 1 ), IndexedEntity.INDEX, "1", null );
		} );

		backendMock.expectWorks( IndexedEntity.INDEX )
				.addOrUpdate( "1", b -> b.field( "indexedField", "value for the field" ) );
		eventFilter.showAllEventsUpToNow( sessionFactory );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void processDifferentRoutesUpdates() {
		// An entity is updated twice in two separate transactions,
		// resulting in two events with different routing keys

		with( sessionFactory ).runInTransaction( session -> {
			RoutedIndexedEntity entity = new RoutedIndexedEntity( 1, "first", RoutedIndexedEntity.Status.FIRST );
			session.persist( entity );
		} );

		backendMock.expectWorks( RoutedIndexedEntity.NAME )
				.add( b -> b.identifier( "1" ).routingKey( "FIRST" )
						.document( StubDocumentNode.document()
								.field( "text", "first" )
								.build() ) );
		eventFilter.showAllEventsUpToNow( sessionFactory );
		backendMock.verifyExpectationsMet();

		// Update the current routing key (but don't trigger indexing yet: events are being filtered)
		with( sessionFactory ).runInTransaction( session -> {
			RoutedIndexedEntity entity = session.find( RoutedIndexedEntity.class, 1 );
			entity.setStatus( RoutedIndexedEntity.Status.SECOND );
			entity.setText( "second" );
		} );

		// Update the current routing key again (but don't trigger indexing yet: events are being filtered)
		with( sessionFactory ).runInTransaction( session -> {
			RoutedIndexedEntity entity = session.find( RoutedIndexedEntity.class, 1 );
			entity.setStatus( RoutedIndexedEntity.Status.THIRD );
			entity.setText( "third" );
		} );

		// Both orders FIRST then SECOND and SECOND then FIRST are reasonable
		backendMock.expectWorks( RoutedIndexedEntity.NAME )
				.createAndExecuteFollowingWorksOutOfOrder()
				.delete( b -> b.identifier( "1" ).routingKey( "FIRST" ) )
				.delete( b -> b.identifier( "1" ).routingKey( "SECOND" ) );

		backendMock.expectWorks( RoutedIndexedEntity.NAME )
				.addOrUpdate( b -> b.identifier( "1" ).routingKey( "THIRD" )
						.document( StubDocumentNode.document()
								.field( "text", "third" )
								.build() ) );
		eventFilter.showAllEventsUpToNow( sessionFactory );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void processDifferentRoutesUpdates_outOfOrder() {
		// An entity is updated twice in two separate transactions,
		// resulting in two events with different routing keys,
		// but the second update event has ID 1, and the first update has ID 2.

		with( sessionFactory ).runInTransaction( session -> {
			RoutedIndexedEntity entity = new RoutedIndexedEntity( 1, "first", RoutedIndexedEntity.Status.FIRST );
			session.persist( entity );
		} );

		backendMock.expectWorks( RoutedIndexedEntity.NAME )
				.add( b -> b.identifier( "1" ).routingKey( "FIRST" )
						.document( StubDocumentNode.document()
								.field( "text", "first" )
								.build() ) );
		eventFilter.showAllEventsUpToNow( sessionFactory );
		backendMock.verifyExpectationsMet();

		// Update the current routing key (but don't trigger indexing yet: events are being filtered)
		with( sessionFactory ).runInTransaction( session -> {
			RoutedIndexedEntity entity = session.find( RoutedIndexedEntity.class, 1 );
			entity.setStatus( RoutedIndexedEntity.Status.SECOND );
			entity.setText( "second" );
		} );

		// Update the current routing key again (but don't trigger indexing yet: events are being filtered)
		with( sessionFactory ).runInTransaction( session -> {
			RoutedIndexedEntity entity = session.find( RoutedIndexedEntity.class, 1 );
			entity.setStatus( RoutedIndexedEntity.Status.THIRD );
			entity.setText( "third" );
		} );

		with( sessionFactory ).runNoTransaction( session -> {
			List<OutboxEvent> events = eventFilter.findOutboxEventsNoFilter( session );
			assertThat( events ).hasSize( 2 );
			// Correct order when ordered by id
			OutboxPollingAutomaticIndexingEventSendingIT.verifyOutboxEntry( events.get( 0 ), RoutedIndexedEntity.NAME, "1",
					"SECOND", "FIRST"
			);
			OutboxPollingAutomaticIndexingEventSendingIT.verifyOutboxEntry( events.get( 1 ), RoutedIndexedEntity.NAME, "1",
					"THIRD", "SECOND"
			);
		} );

		with( sessionFactory ).runInTransaction( session -> {
			// Swap the IDs of event 2 (update routing key from "FIRST" to "SECOND") and
			// 3 (update routing key from "SECOND" to "THIRD")
			swapOutboxTableRowIdAndCreatedValues( session, 0, 1 );
		} );

		with( sessionFactory ).runNoTransaction( session -> {
			List<OutboxEvent> events = eventFilter.findOutboxEventsNoFilter( session );
			assertThat( events ).hasSize( 2 );
			// Out-of-order when ordered by id
			OutboxPollingAutomaticIndexingEventSendingIT.verifyOutboxEntry( events.get( 0 ), RoutedIndexedEntity.NAME, "1",
					"THIRD", "SECOND"
			);
			OutboxPollingAutomaticIndexingEventSendingIT.verifyOutboxEntry( events.get( 1 ), RoutedIndexedEntity.NAME, "1",
					"SECOND", "FIRST"
			);
		} );

		// Both orders FIRST then SECOND and SECOND then FIRST are reasonable
		backendMock.expectWorks( RoutedIndexedEntity.NAME )
				.createAndExecuteFollowingWorksOutOfOrder()
				.delete( b -> b.identifier( "1" ).routingKey( "FIRST" ) )
				.delete( b -> b.identifier( "1" ).routingKey( "SECOND" ) );

		backendMock.expectWorks( RoutedIndexedEntity.NAME )
				.addOrUpdate( b -> b.identifier( "1" ).routingKey( "THIRD" )
						.document( StubDocumentNode.document()
								.field( "text", "third" )
								.build() ) );
		eventFilter.showAllEventsUpToNow( sessionFactory );
		backendMock.verifyExpectationsMet();
	}

	private void swapOutboxTableRowIdAndCreatedValues(Session session, int row1, int row2) {
		List<OutboxEvent> events = session.createQuery(
				"select e from org.hibernate.search.mapper.orm.outboxpolling.event.impl.OutboxEvent e order by e.processAfter, e.id",
				OutboxEvent.class
		).getResultList();
		OutboxEvent event1 = events.get( row1 );
		OutboxEvent event2 = events.get( row2 );

		session.detach( event1 );
		session.detach( event2 );

		Instant processAfter = event1.getProcessAfter();
		event1.setProcessAfter( event2.getProcessAfter() );
		event2.setProcessAfter( processAfter );

		UUID id = event1.getId();
		event1.setId( event2.getId() );
		event2.setId( id );

		session.merge( event1 );
		session.merge( event2 );
	}

	@Entity(name = IndexedEntity.INDEX)
	@Indexed(index = IndexedEntity.INDEX)
	public static class IndexedEntity {
		static final String INDEX = "IndexedEntity";

		@Id
		private Integer id;

		@Basic
		@GenericField
		private String indexedField;

		public IndexedEntity() {
		}

		public IndexedEntity(Integer id, String indexedField) {
			this.id = id;
			this.indexedField = indexedField;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getIndexedField() {
			return indexedField;
		}

		public void setIndexedField(String indexedField) {
			this.indexedField = indexedField;
		}
	}
}
