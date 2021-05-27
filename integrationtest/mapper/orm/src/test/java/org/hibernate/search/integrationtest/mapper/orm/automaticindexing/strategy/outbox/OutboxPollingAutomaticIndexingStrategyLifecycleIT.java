/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.strategy.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.integrationtest.mapper.orm.automaticindexing.strategy.outbox.OutboxPollingNoProcessingIT.verifyOutboxEntry;

import java.util.List;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.orm.outbox.impl.OutboxEvent;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.AutomaticIndexingStrategyExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.awaitility.Awaitility;

public class OutboxPollingAutomaticIndexingStrategyLifecycleIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock )
			.automaticIndexingStrategy( AutomaticIndexingStrategyExpectations.outboxPolling() );

	private final FilteringOutboxEventFinder outboxEventFinder = new FilteringOutboxEventFinder();

	@Before
	public void cleanUp() {
		SessionFactory sessionFactory = setupWithCleanup();
		sessionFactory.close();
	}

	@Test
	public void stopWhileOutboxEventsIsBeingProcessed() {
		SessionFactory sessionFactory = setup();
		backendMock.verifyExpectationsMet();

		OrmUtils.withinTransaction( sessionFactory, session -> {
			for ( int i = 1; i <= 1000; i++ ) {
				IndexedEntity entity = new IndexedEntity();
				entity.setId( i );
				entity.setIndexedField( "value for the field" );
				session.persist( entity );
			}

			BackendMock.DocumentWorkCallListContext context = backendMock.expectWorks( IndexedEntity.NAME );
			for ( int i = 1; i <= 1000; i++ ) {
				context.add( i + "", b -> b.field( "indexedField", "value for the field" ) );
			}
			context.createdThenExecuted();
		} );

		// wait for the first call is processed (partial progressing)
		outboxEventFinder.showAllEventsUpToNow( sessionFactory );
		backendMock.awaitFirstDocumentWorkCall();

		// stop Search on partial progressing
		sessionFactory.close();

		// verify some entities have not been processed
		outboxEventFinder.hideAllEvents();
		sessionFactory = setup();
		OrmUtils.withinTransaction( sessionFactory, session -> {
			assertThat( outboxEventFinder.findOutboxEventsNoFilter( session ) ).isNotEmpty();
		} );
		sessionFactory.close();

		// process the entities restarting Search:
		sessionFactory = setup();
		outboxEventFinder.showAllEventsUpToNow( sessionFactory );

		backendMock.verifyExpectationsMet();
	}

	@Test
	public void processCreateUpdateDelete() {
		SessionFactory sessionFactory = setup();
		backendMock.verifyExpectationsMet();

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

		sessionFactory.close();
		sessionFactory = setup();

		OrmUtils.withinTransaction( sessionFactory, session -> {
			List<OutboxEvent> outboxEntries = outboxEventFinder.findOutboxEventsNoFilter( session );
			assertThat( outboxEntries ).hasSize( 3 );
			verifyOutboxEntry( outboxEntries.get( 0 ), IndexedEntity.NAME, "1", OutboxEvent.Type.ADD, null );
			verifyOutboxEntry( outboxEntries.get( 1 ), IndexedEntity.NAME, "1", OutboxEvent.Type.ADD_OR_UPDATE, null );
			verifyOutboxEntry( outboxEntries.get( 2 ), IndexedEntity.NAME, "1", OutboxEvent.Type.DELETE, null );
		} );

		// The events were hidden until now, to ensure they were not processed in separate batches.
		// Make them visible to Hibernate Search now.
		outboxEventFinder.showAllEventsUpToNow( sessionFactory );

		SessionFactory finalSessionFactory = sessionFactory;
		Awaitility.await().untilAsserted( () -> thereAreNoMoreOutboxEntities( finalSessionFactory ) );

		// No works are expected to be executed by the time the outbox events are processed
		backendMock.verifyExpectationsMet();
	}

	private void thereAreNoMoreOutboxEntities(SessionFactory sessionFactory) {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			List<OutboxEvent> outboxEntries = outboxEventFinder.findOutboxEventsNoFilter( session );
			assertThat( outboxEntries ).isEmpty();
		} );
	}

	private SessionFactory setup() {
		SessionFactory sessionFactory;
		backendMock.expectSchema( IndexedEntity.NAME, b -> b.field( "indexedField", String.class ) );
		sessionFactory = ormSetupHelper.start()
				.withProperty( "hibernate.search.automatic_indexing.outbox_event_finder", outboxEventFinder )
				.withProperty( "hibernate.hbm2ddl.auto", "update" )
				.setup( IndexedEntity.class );
		return sessionFactory;
	}

	private SessionFactory setupWithCleanup() {
		backendMock.expectSchema( IndexedEntity.NAME, b -> b.field( "indexedField", String.class ) );
		SessionFactory sessionFactory = ormSetupHelper.start()
				.withProperty( "hibernate.search.automatic_indexing.outbox_event_finder", outboxEventFinder )
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
		return sessionFactory;
	}

	@Entity(name = IndexedEntity.NAME)
	@Indexed(index = IndexedEntity.NAME)
	public static class IndexedEntity {
		static final String NAME = "IndexedEntity";

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
