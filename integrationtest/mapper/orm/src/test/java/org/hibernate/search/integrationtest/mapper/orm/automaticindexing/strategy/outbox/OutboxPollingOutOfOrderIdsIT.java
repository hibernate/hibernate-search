/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.strategy.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.hibernate.search.integrationtest.mapper.orm.automaticindexing.strategy.outbox.OutboxPollingEventSendingIT.verifyOutboxEntry;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.withinTransaction;
import static org.junit.Assume.assumeTrue;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.search.mapper.orm.outbox.impl.OutboxEvent;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.StubDocumentNode;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.AutomaticIndexingStrategyExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.awaitility.Awaitility;

public class OutboxPollingOutOfOrderIdsIT {

	private static final String OUTBOX_TABLE_UPDATE_ID = "UPDATE HSEARCH_OUTBOX_TABLE SET ID = ? WHERE ID = ?";

	private final FilteringOutboxEventFinder outboxEventFinder = new FilteringOutboxEventFinder();

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock )
			.automaticIndexingStrategy( AutomaticIndexingStrategyExpectations.outboxPolling() );

	private SessionFactory sessionFactory;

	@Before
	public void before() {
		backendMock.expectAnySchema( IndexedEntity.INDEX )
				.expectAnySchema( RoutedIndexedEntity.NAME );
		sessionFactory = ormSetupHelper.start()
				.withProperty( "hibernate.search.automatic_indexing.outbox_event_finder", outboxEventFinder )
				.setup( IndexedEntity.class, RoutedIndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void processCreateUpdateDelete() {
		// An entity is created, updated, then deleted in separate transactions,
		// but the delete event has ID 1, the update event has ID 2, and the add event has ID 3.

		int id = 1;
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity = new IndexedEntity();
			entity.setId( id );
			entity.setIndexedField( "value for the field" );
			session.persist( entity );
		} );

		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity = session.load( IndexedEntity.class, id );
			entity.setIndexedField( "another value for the field" );
			session.merge( entity );
		} );

		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity = session.load( IndexedEntity.class, id );
			session.delete( entity );
		} );

		OrmUtils.withinSession( sessionFactory, session -> {
			List<OutboxEvent> events = outboxEventFinder.findOutboxEventsNoFilterById( session );
			assertThat( events ).hasSize( 3 );
			// right order
			verifyOutboxEntry( events.get( 0 ), IndexedEntity.INDEX, "1", OutboxEvent.Type.ADD, null );
			verifyOutboxEntry( events.get( 1 ), IndexedEntity.INDEX, "1", OutboxEvent.Type.ADD_OR_UPDATE, null );
			verifyOutboxEntry( events.get( 2 ), IndexedEntity.INDEX, "1", OutboxEvent.Type.DELETE, null );
		} );

		OrmUtils.withinTransaction( sessionFactory, session -> {
			// change the ids 1 <--> 3
			updateOutboxTableRow( session, 1, 4 );
			updateOutboxTableRow( session, 3, 1 );
			updateOutboxTableRow( session, 4, 3 );
		} );

		OrmUtils.withinSession( sessionFactory, session -> {
			List<OutboxEvent> events = outboxEventFinder.findOutboxEventsNoFilterById( session );
			assertThat( events ).hasSize( 3 );
			// out-of-order now
			verifyOutboxEntry( events.get( 0 ), IndexedEntity.INDEX, "1", OutboxEvent.Type.DELETE, null );
			verifyOutboxEntry( events.get( 1 ), IndexedEntity.INDEX, "1", OutboxEvent.Type.ADD_OR_UPDATE, null );
			verifyOutboxEntry( events.get( 2 ), IndexedEntity.INDEX, "1", OutboxEvent.Type.ADD, null );
		} );

		// The events were hidden until now, to ensure they were not processed in separate batches.
		// Make them visible to Hibernate Search now.
		outboxEventFinder.showAllEventsUpToNow( sessionFactory );

		SessionFactory finalSessionFactory = sessionFactory;
		Awaitility.await().untilAsserted( () -> thereAreNoMoreOutboxEntities( finalSessionFactory ) );

		// No works are expected to be executed by the time the outbox events are processed
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void processDeleteRecreate_rightOrder() {
		// An entity is deleted, then re-created in separate transactions.

		int id = 1;
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity = new IndexedEntity();
			entity.setId( id );
			entity.setIndexedField( "value for the field" );
			session.persist( entity );
		} );

		backendMock.expectWorks( IndexedEntity.INDEX )
				.add( "1", b -> b.field( "indexedField", "value for the field" ) );
		outboxEventFinder.showAllEventsUpToNow( sessionFactory );
		backendMock.verifyExpectationsMet();

		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity = session.load( IndexedEntity.class, id );
			session.remove( entity );
		} );
		OrmUtils.withinTransaction( sessionFactory, session -> {
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
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity = new IndexedEntity();
			entity.setId( id );
			entity.setIndexedField( "value for the field" );
			session.persist( entity );
		} );

		backendMock.expectWorks( IndexedEntity.INDEX )
				.add( "1", b -> b.field( "indexedField", "value for the field" ) );
		outboxEventFinder.showAllEventsUpToNow( sessionFactory );
		backendMock.verifyExpectationsMet();

		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity = session.load( IndexedEntity.class, id );
			session.remove( entity );
		} );
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity = new IndexedEntity();
			entity.setId( id );
			entity.setIndexedField( "value for the field" );
			session.persist( entity );
		} );

		OrmUtils.withinSession( sessionFactory, session -> {
			List<OutboxEvent> events = outboxEventFinder.findOutboxEventsNoFilterById( session );
			assertThat( events ).hasSize( 2 );
			// right order
			verifyOutboxEntry( events.get( 0 ), IndexedEntity.INDEX, "1", OutboxEvent.Type.DELETE, null );
			verifyOutboxEntry( events.get( 1 ), IndexedEntity.INDEX, "1", OutboxEvent.Type.ADD, null );
		} );

		OrmUtils.withinTransaction( sessionFactory, session -> {
			// change the ids 2 <--> 3
			updateOutboxTableRow( session, 2, 4 );
			updateOutboxTableRow( session, 3, 2 );
			updateOutboxTableRow( session, 4, 3 );
		} );

		OrmUtils.withinSession( sessionFactory, session -> {
			List<OutboxEvent> events = outboxEventFinder.findOutboxEventsNoFilterById( session );
			assertThat( events ).hasSize( 2 );
			// out-of-order now
			verifyOutboxEntry( events.get( 0 ), IndexedEntity.INDEX, "1", OutboxEvent.Type.ADD, null );
			verifyOutboxEntry( events.get( 1 ), IndexedEntity.INDEX, "1", OutboxEvent.Type.DELETE, null );
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

		withinTransaction( sessionFactory, session -> {
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
		withinTransaction( sessionFactory, session -> {
			RoutedIndexedEntity entity = session.find( RoutedIndexedEntity.class, 1 );
			entity.setStatus( RoutedIndexedEntity.Status.SECOND );
			entity.setText( "second" );
		} );

		// Update the current routing key again (but don't trigger indexing yet: events are being filtered)
		withinTransaction( sessionFactory, session -> {
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

		withinTransaction( sessionFactory, session -> {
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
		withinTransaction( sessionFactory, session -> {
			RoutedIndexedEntity entity = session.find( RoutedIndexedEntity.class, 1 );
			entity.setStatus( RoutedIndexedEntity.Status.SECOND );
			entity.setText( "second" );
		} );

		// Update the current routing key again (but don't trigger indexing yet: events are being filtered)
		withinTransaction( sessionFactory, session -> {
			RoutedIndexedEntity entity = session.find( RoutedIndexedEntity.class, 1 );
			entity.setStatus( RoutedIndexedEntity.Status.THIRD );
			entity.setText( "third" );
		} );

		OrmUtils.withinSession( sessionFactory, session -> {
			List<OutboxEvent> events = outboxEventFinder.findOutboxEventsNoFilterById( session );
			assertThat( events ).hasSize( 2 );
			// right order
			verifyOutboxEntry( events.get( 0 ), RoutedIndexedEntity.NAME, "1", OutboxEvent.Type.ADD_OR_UPDATE,
					"SECOND", "FIRST"
			);
			verifyOutboxEntry( events.get( 1 ), RoutedIndexedEntity.NAME, "1", OutboxEvent.Type.ADD_OR_UPDATE,
					"THIRD", "SECOND"
			);
		} );

		OrmUtils.withinTransaction( sessionFactory, session -> {
			// change the ids 2 <--> 3
			updateOutboxTableRow( session, 2, 4 );
			updateOutboxTableRow( session, 3, 2 );
			updateOutboxTableRow( session, 4, 3 );
		} );

		OrmUtils.withinSession( sessionFactory, session -> {
			List<OutboxEvent> events = outboxEventFinder.findOutboxEventsNoFilterById( session );
			assertThat( events ).hasSize( 2 );
			// out-of-order now
			verifyOutboxEntry( events.get( 0 ), RoutedIndexedEntity.NAME, "1", OutboxEvent.Type.ADD_OR_UPDATE,
					"THIRD", "SECOND"
			);
			verifyOutboxEntry( events.get( 1 ), RoutedIndexedEntity.NAME, "1", OutboxEvent.Type.ADD_OR_UPDATE,
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

	private void updateOutboxTableRow(Session session, Integer oldId, Integer newId) {
		try {
			SharedSessionContractImplementor implementor = session.unwrap( SharedSessionContractImplementor.class );

			JdbcCoordinator jdbc = implementor.getJdbcCoordinator();
			JdbcEnvironment env = implementor.getJdbcServices().getJdbcEnvironment();
			assumeTrue(
					"This test uses SQL directly which can currently only be set up with H2",
					env.getDialect() instanceof H2Dialect
			);

			try ( PreparedStatement ps = jdbc.getStatementPreparer().prepareStatement( OUTBOX_TABLE_UPDATE_ID ) ) {
				ps.setInt( 1, newId );
				ps.setInt( 2, oldId );

				jdbc.getResultSetReturn().executeUpdate( ps );
			}
		}
		catch (SQLException exception) {
			fail( "Unexpected SQL exception: " + exception );
		}
	}

	private void thereAreNoMoreOutboxEntities(SessionFactory sessionFactory) {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			List<OutboxEvent> outboxEntries = outboxEventFinder.findOutboxEventsNoFilter( session );
			assertThat( outboxEntries ).isEmpty();
		} );
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
