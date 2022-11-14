/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling.automaticindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;
import static org.junit.Assume.assumeTrue;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling.FilteringOutboxEventFinder;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.OutboxEvent;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.StubDocumentNode;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.CoordinationStrategyExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class OutboxPollingAutomaticIndexingOutOfOrderIdsIT {

	private static final String OUTBOX_EVENT_UPDATE_ID_AND_TIME = "UPDATE HSEARCH_OUTBOX_EVENT SET ID = ?, CREATED = ? WHERE ID = ?";

	private static final String OUTBOX_EVENT_SELECT_ORDERED_IDS_AND_CREATED_TIME = "SELECT ID, CREATED FROM HSEARCH_OUTBOX_EVENT ORDER BY CREATED, ID";

	private final FilteringOutboxEventFinder outboxEventFinder = new FilteringOutboxEventFinder();

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock )
			.coordinationStrategy( CoordinationStrategyExpectations.outboxPolling() );

	private SessionFactory sessionFactory;

	@Before
	public void before() {
		backendMock.expectAnySchema( IndexedEntity.INDEX )
				.expectAnySchema( RoutedIndexedEntity.NAME );
		sessionFactory = ormSetupHelper.start()
				.withProperty( "hibernate.search.coordination.outbox_event_finder.provider", outboxEventFinder.provider() )
				// use timebase uuids to get predictable sorting order
				.withProperty( "hibernate.search.coordination.entity.mapping.outboxevent.uuid_gen_strategy", "time" )
				// see HSEARCH-4749, as some DBs (MSSQL) might use a nonstring representation of UUID we want to force it
				// in this case to make row manipulation easier:
				.withProperty( "hibernate.search.coordination.entity.mapping.outboxevent.preferred_uuid_jdbc_type", "uuid-char" )
				.setup( IndexedEntity.class, RoutedIndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void processCreateUpdateDelete() {
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
			List<OutboxEvent> events = outboxEventFinder.findOutboxEventsNoFilter( session );
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
			List<OutboxEvent> events = outboxEventFinder.findOutboxEventsNoFilter( session );
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
		outboxEventFinder.showAllEventsUpToNow( sessionFactory );

		outboxEventFinder.awaitUntilNoMoreVisibleEvents( sessionFactory );

		backendMock.verifyExpectationsMet();
	}

	@Test
	public void processDeleteRecreate_rightOrder() {
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
		outboxEventFinder.showAllEventsUpToNow( sessionFactory );
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
		outboxEventFinder.showAllEventsUpToNow( sessionFactory );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void processDeleteRecreate_outOfOrder() {
		// An entity is deleted, then re-created in separate transactions,
		// but the add event has ID 1, the and the delete event has ID 2.

		int id = 1;
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity = new IndexedEntity();
			entity.setId( id );
			entity.setIndexedField( "value for the field" );
			session.persist( entity );
		} );

		backendMock.expectWorks( IndexedEntity.INDEX )
				.add( "1", b -> b.field( "indexedField", "value for the field" ) );
		outboxEventFinder.showAllEventsUpToNow( sessionFactory );
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
			List<OutboxEvent> events = outboxEventFinder.findOutboxEventsNoFilter( session );
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
			List<OutboxEvent> events = outboxEventFinder.findOutboxEventsNoFilter( session );
			assertThat( events ).hasSize( 2 );
			// Out-of-order when ordered by id (you'll have to trust me on that)
			// add
			OutboxPollingAutomaticIndexingEventSendingIT.verifyOutboxEntry( events.get( 0 ), IndexedEntity.INDEX, "1", null );
			// delete
			OutboxPollingAutomaticIndexingEventSendingIT.verifyOutboxEntry( events.get( 1 ), IndexedEntity.INDEX, "1", null );
		} );

		backendMock.expectWorks( IndexedEntity.INDEX )
				.addOrUpdate( "1", b -> b.field( "indexedField", "value for the field" ) );
		outboxEventFinder.showAllEventsUpToNow( sessionFactory );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void processDifferentRoutesUpdates() {
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
		outboxEventFinder.showAllEventsUpToNow( sessionFactory );
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
		outboxEventFinder.showAllEventsUpToNow( sessionFactory );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void processDifferentRoutesUpdates_outOfOrder() {
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
		outboxEventFinder.showAllEventsUpToNow( sessionFactory );
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
			List<OutboxEvent> events = outboxEventFinder.findOutboxEventsNoFilter( session );
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
			List<OutboxEvent> events = outboxEventFinder.findOutboxEventsNoFilter( session );
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
		outboxEventFinder.showAllEventsUpToNow( sessionFactory );
		backendMock.verifyExpectationsMet();
	}

	private void swapOutboxTableRowIdAndCreatedValues(Session session, int row1, int row2) {
		try {
			SharedSessionContractImplementor implementor = session.unwrap( SharedSessionContractImplementor.class );

			JdbcCoordinator jdbc = implementor.getJdbcCoordinator();
			JdbcEnvironment env = implementor.getJdbcServices().getJdbcEnvironment();

			String javaVersionString = System.getProperty( "java.version" );
			if ( javaVersionString != null && !javaVersionString.trim().isEmpty() ) {
				boolean oldJavaVersion = javaVersionString.startsWith( "1." );
				assumeTrue(
						"The H2 actual maximum available precision depends on operating system and JVM and can be 3 (milliseconds) or higher. " +
								"Higher precision is not available before Java 9.",
						!( oldJavaVersion && env.getDialect() instanceof H2Dialect )
				);
			}

			List<String> uuids = new ArrayList<>();
			List<java.sql.Timestamp> times = new ArrayList<>();
			try ( PreparedStatement statement = jdbc.getStatementPreparer().prepareStatement( OUTBOX_EVENT_SELECT_ORDERED_IDS_AND_CREATED_TIME ) ) {
				ResultSet resultSet = statement.executeQuery();
				while ( resultSet.next() ) {
					uuids.add( resultSet.getString( 1 ) );
					times.add( resultSet.getTimestamp( 2 ) );
				}
			}

			String temporaryUuid = UUID.randomUUID().toString();
			updateOutboxTableRow( jdbc, temporaryUuid, uuids.get( row2 ), times.get( row1 ) );
			updateOutboxTableRow( jdbc, uuids.get( row2 ), uuids.get( row1 ), times.get( row2 ) );
			updateOutboxTableRow( jdbc, uuids.get( row1 ), temporaryUuid, times.get( row1 ) );
		}
		catch (SQLException exception) {
			fail( "Unexpected SQL exception: " + exception );
		}
	}

	private void updateOutboxTableRow(JdbcCoordinator jdbc, String newId, String rowToUpdateId,
			java.sql.Timestamp newCreated) throws SQLException {
		try ( PreparedStatement ps = jdbc.getStatementPreparer().prepareStatement( OUTBOX_EVENT_UPDATE_ID_AND_TIME ) ) {
			ps.setString( 1, newId );
			ps.setTimestamp( 2, newCreated );
			ps.setString( 3, rowToUpdateId );

			jdbc.getResultSetReturn().executeUpdate( ps );
		}
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
