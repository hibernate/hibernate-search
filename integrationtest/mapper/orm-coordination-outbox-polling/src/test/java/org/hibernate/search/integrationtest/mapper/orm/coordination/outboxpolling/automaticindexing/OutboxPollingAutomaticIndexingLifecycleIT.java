/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling.automaticindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling.automaticindexing.OutboxPollingAutomaticIndexingEventSendingIT.verifyOutboxEntry;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.List;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling.testsupport.util.OutboxEventFilter;
import org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling.testsupport.util.TestingOutboxPollingInternalConfigurer;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cfg.HibernateOrmMapperOutboxPollingSettings;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cfg.impl.HibernateOrmMapperOutboxPollingImplSettings;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.OutboxEvent;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.CoordinationStrategyExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class OutboxPollingAutomaticIndexingLifecycleIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock )
			.coordinationStrategy( CoordinationStrategyExpectations.outboxPolling() );

	private final OutboxEventFilter eventFilter = new OutboxEventFilter();

	@Before
	public void cleanUp() {
		SessionFactory sessionFactory = setupWithCleanup();
		sessionFactory.close();
	}

	@Test
	public void stopWhileOutboxEventsIsBeingProcessed() {
		SessionFactory sessionFactory = setup();
		backendMock.verifyExpectationsMet();
		int size = 1000;

		with( sessionFactory ).runInTransaction( session -> {
			for ( int i = 1; i <= size; i++ ) {
				IndexedEntity entity = new IndexedEntity();
				entity.setId( i );
				entity.setIndexedField( "value for the field" );
				session.persist( entity );
			}

			BackendMock.DocumentWorkCallListContext context = backendMock.expectWorks( IndexedEntity.NAME );
			for ( int i = 1; i <= size; i++ ) {
				context.add( i + "", b -> b.field( "indexedField", "value for the field" ) );
			}
		} );

		// wait for the first call is processed (partial progressing)
		eventFilter.showAllEventsUpToNow( sessionFactory );
		SessionFactory finalSessionFactory = sessionFactory;
		backendMock.indexingWorkExpectations().awaitIndexingAssertions( () -> {
			with( finalSessionFactory ).runInTransaction( session -> {
				assertThat( eventFilter.findOutboxEventIdsNoFilter( session ) ).hasSizeLessThan( size );
			} );
		} );

		// stop Search on partial progressing
		sessionFactory.close();

		// verify some entities have not been processed
		eventFilter.hideAllEvents();
		sessionFactory = setup();
		with( sessionFactory ).runInTransaction( session -> {
			List<OutboxEvent> outboxEventsNoFilter = eventFilter.findOutboxEventsNoFilter( session );
			// partial processing, meaning that the events size is *strictly* between 0 and the full size:
			assertThat( outboxEventsNoFilter ).hasSizeBetween( 1, size - 1 );
		} );
		sessionFactory.close();

		// process the entities restarting Search:
		sessionFactory = setup();
		eventFilter.showAllEventsUpToNow( sessionFactory );

		backendMock.verifyExpectationsMet();
	}

	@Test
	public void processCreateUpdateDelete() {
		SessionFactory sessionFactory = setup();
		backendMock.verifyExpectationsMet();

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

		sessionFactory.close();
		sessionFactory = setup();

		with( sessionFactory ).runInTransaction( session -> {
			List<OutboxEvent> events = eventFilter.findOutboxEventsNoFilter( session );
			assertThat( events ).hasSize( 3 );
			// add
			verifyOutboxEntry( events.get( 0 ), OutboxPollingAutomaticIndexingOutOfOrderIdsIT.IndexedEntity.INDEX, "1", null );
			// update
			verifyOutboxEntry( events.get( 1 ), OutboxPollingAutomaticIndexingOutOfOrderIdsIT.IndexedEntity.INDEX, "1", null );
			// delete
			verifyOutboxEntry( events.get( 2 ), OutboxPollingAutomaticIndexingOutOfOrderIdsIT.IndexedEntity.INDEX, "1", null );
		} );

		// Only a delete work is expected to be executed by the time the outbox events are processed;
		// that's because we don't trust events to tell us whether the document already exists in the index.
		// We don't trust the events for this because they can arrive in the wrong order
		// (see OutboxPollingAutomaticIndexingOutOfOrderIdsIT).
		backendMock.expectWorks( IndexedEntity.NAME )
				.delete( "1" );

		// The events were hidden until now, to ensure they were not processed in separate batches.
		// Make them visible to Hibernate Search now.
		eventFilter.showAllEventsUpToNow( sessionFactory );

		eventFilter.awaitUntilNoMoreVisibleEvents( sessionFactory );

		backendMock.verifyExpectationsMet();
	}

	private SessionFactory setup() {
		SessionFactory sessionFactory;
		backendMock.expectSchema( IndexedEntity.NAME, b -> b.field( "indexedField", String.class ) );
		sessionFactory = ormSetupHelper.start()
				.withProperty( HibernateOrmMapperOutboxPollingImplSettings.COORDINATION_INTERNAL_CONFIGURER,
						new TestingOutboxPollingInternalConfigurer().outboxEventFilter( eventFilter ) )
				// Override OutboxPollingOrmSetupHelperConfig.overrideHibernateSearchDefaults
				// because a short polling interval could in theory make event processing too fast
				// and make this test flaky.
				.withProperty( HibernateOrmMapperOutboxPollingSettings.COORDINATION_EVENT_PROCESSOR_POLLING_INTERVAL,
						100 )
				.withProperty( "hibernate.hbm2ddl.auto", "update" )
				.setup( IndexedEntity.class );
		return sessionFactory;
	}

	private SessionFactory setupWithCleanup() {
		backendMock.expectSchema( IndexedEntity.NAME, b -> b.field( "indexedField", String.class ) );
		SessionFactory sessionFactory = ormSetupHelper.start()
				.withProperty( HibernateOrmMapperOutboxPollingImplSettings.COORDINATION_INTERNAL_CONFIGURER,
						new TestingOutboxPollingInternalConfigurer().outboxEventFilter( eventFilter ) )
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
