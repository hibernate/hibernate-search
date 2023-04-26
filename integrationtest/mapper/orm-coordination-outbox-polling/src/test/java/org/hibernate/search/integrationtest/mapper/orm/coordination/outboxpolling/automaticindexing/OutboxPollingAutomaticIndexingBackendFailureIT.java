/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling.automaticindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.reporting.EntityIndexingFailureContext;
import org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling.testsupport.util.OutboxEventFilter;
import org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling.testsupport.util.TestFailureHandler;
import org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling.testsupport.util.TestingOutboxPollingInternalConfigurer;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cfg.impl.HibernateOrmMapperOutboxPollingImplSettings;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.CoordinationStrategyExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.Rule;
import org.junit.Test;

import org.awaitility.core.ThrowingRunnable;

/**
 * Extensive tests of backend failures for automatic indexing with outbox-polling.
 */
public class OutboxPollingAutomaticIndexingBackendFailureIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock )
			.coordinationStrategy( CoordinationStrategyExpectations.outboxPolling() );

	private final OutboxEventFilter eventFilter = new OutboxEventFilter()
			// Disable the filter by default: only some of the tests actually need it.
			.showAllEvents();

	private SessionFactory sessionFactory;
	private TestFailureHandler failureHandler;

	@Test
	public void backendFailure() {
		setup( 0 );
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setIndexedField( "initialValue" );
			session.persist( entity1 );

			IndexedEntity entity2 = new IndexedEntity();
			entity2.setId( 2 );
			entity2.setIndexedField( "initialValue" );
			session.persist( entity2 );

			IndexedEntity entity3 = new IndexedEntity();
			entity3.setId( 3 );
			entity3.setIndexedField( "initialValue" );
			session.persist( entity3 );

			CompletableFuture<?> failingFuture = new CompletableFuture<>();
			failingFuture.completeExceptionally( new SimulatedFailure( "Indexing work #2 failed!" ) );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.createAndExecuteFollowingWorks()
					.add( "1", b -> b
							.field( "indexedField", "initialValue" )
					)
					.add( "3", b -> b
							.field( "indexedField", "initialValue" )
					)
					.createAndExecuteFollowingWorks( failingFuture )
					.add( "2", b -> b
							.field( "indexedField", "initialValue" )
					)
					// retry (succeeds):
					.createAndExecuteFollowingWorks()
					.addOrUpdate( "2", b -> b
							.field( "indexedField", "initialValue" )
					);

		} );
		backendMock.verifyExpectationsMet();

		assertThat( failureHandler.genericFailures ).isEmpty();

		List<EntityIndexingFailureContext> entityFailures = failureHandler.entityFailures.get( 2 );
		awaitFor( () -> assertThat( entityFailures ).hasSize( 1 ) );

		EntityIndexingFailureContext entityFailure = entityFailures.get( 0 );
		checkId2EntityEventFailure( entityFailure );
	}

	@Test
	public void backendFailure_retryAfter() {
		setup( 3 );
		AtomicLong timeOfTheException = new AtomicLong();
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setIndexedField( "initialValue" );
			session.persist( entity1 );

			IndexedEntity entity2 = new IndexedEntity();
			entity2.setId( 2 );
			entity2.setIndexedField( "initialValue" );
			session.persist( entity2 );

			IndexedEntity entity3 = new IndexedEntity();
			entity3.setId( 3 );
			entity3.setIndexedField( "initialValue" );
			session.persist( entity3 );

			CompletableFuture<?> failingFuture = new CompletableFuture<>();
			failingFuture.completeExceptionally( new SimulatedFailure( "Indexing work #2 failed!" ) );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.createAndExecuteFollowingWorks()
					.add( "1", b -> b
							.field( "indexedField", "initialValue" )
					)
					.add( "3", b -> b
							.field( "indexedField", "initialValue" )
					)
					.createAndExecuteFollowingWorks( failingFuture )
					.add( "2", b -> b
							.field( "indexedField", "initialValue" )
					);

			timeOfTheException.set( System.currentTimeMillis() );
		} );
		backendMock.verifyExpectationsMet();

		backendMock.expectWorks( IndexedEntity.INDEX )
				// retry (succeeds):
				.createAndExecuteFollowingWorks()
				.addOrUpdate( "2", b -> b
						.field( "indexedField", "initialValue" )
				);
		backendMock.verifyExpectationsMet();

		long timeOfTheProcess = System.currentTimeMillis();

		// retry only once
		assertThat( failureHandler.entityFailures.get( 2 ) ).hasSize( 1 );
		// time is at least 3 sec
		assertThat( timeOfTheProcess - timeOfTheException.get() ).isGreaterThan( 3000 );
	}

	@Test
	public void backendFailure_twoFailuresOfTheSameIndexingWork() {
		setup( 0 );
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setIndexedField( "initialValue" );
			session.persist( entity1 );

			IndexedEntity entity2 = new IndexedEntity();
			entity2.setId( 2 );
			entity2.setIndexedField( "initialValue" );
			session.persist( entity2 );

			IndexedEntity entity3 = new IndexedEntity();
			entity3.setId( 3 );
			entity3.setIndexedField( "initialValue" );
			session.persist( entity3 );

			CompletableFuture<?> failingFuture = new CompletableFuture<>();
			failingFuture.completeExceptionally( new SimulatedFailure( "Indexing work #2 failed!" ) );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "indexedField", "initialValue" )
					)
					.add( "3", b -> b
							.field( "indexedField", "initialValue" )
					)
					.createAndExecuteFollowingWorks( failingFuture )
					.add( "2", b -> b
							.field( "indexedField", "initialValue" )
					)
					// retry (fails too):
					.addOrUpdate( "2", b -> b
							.field( "indexedField", "initialValue" )
					);

			// finally it works:
			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "2", b -> b
							.field( "indexedField", "initialValue" )
					);

		} );
		backendMock.verifyExpectationsMet();

		assertThat( failureHandler.genericFailures ).isEmpty();

		List<EntityIndexingFailureContext> entityFailures = failureHandler.entityFailures.get( 2 );
		awaitFor( () -> assertThat( entityFailures ).hasSize( 2 ) );

		EntityIndexingFailureContext entityFailure = entityFailures.get( 0 );
		checkId2EntityEventFailure( entityFailure );

		entityFailure = entityFailures.get( 1 );
		checkId2EntityEventFailure( entityFailure );
	}

	@Test
	public void backendFailure_numberOfTrialsExhausted() {
		setup( 0 );
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setIndexedField( "initialValue" );
			session.persist( entity1 );

			IndexedEntity entity2 = new IndexedEntity();
			entity2.setId( 2 );
			entity2.setIndexedField( "initialValue" );
			session.persist( entity2 );

			IndexedEntity entity3 = new IndexedEntity();
			entity3.setId( 3 );
			entity3.setIndexedField( "initialValue" );
			session.persist( entity3 );

			CompletableFuture<?> failingFuture = new CompletableFuture<>();
			failingFuture.completeExceptionally( new SimulatedFailure( "Indexing work #2 failed!" ) );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "indexedField", "initialValue" )
					)
					.add( "3", b -> b
							.field( "indexedField", "initialValue" )
					)
					.createAndExecuteFollowingWorks( failingFuture )
					.add( "2", b -> b
							.field( "indexedField", "initialValue" )
					)
					// retry (fails too):
					.createAndExecuteFollowingWorks( failingFuture )
					.addOrUpdate( "2", b -> b
							.field( "indexedField", "initialValue" )
					)
					// retry (fails too):
					.createAndExecuteFollowingWorks( failingFuture )
					.addOrUpdate( "2", b -> b
							.field( "indexedField", "initialValue" )
					);

			// no more retry
		} );
		backendMock.verifyExpectationsMet();

		assertThat( failureHandler.genericFailures ).isEmpty();

		List<EntityIndexingFailureContext> entityFailures = failureHandler.entityFailures.get( 2 );
		awaitFor( () -> assertThat( entityFailures ).hasSize( 4 ) );

		for ( int i = 0; i < 3; i++ ) {
			EntityIndexingFailureContext entityFailure = entityFailures.get( i );
			checkId2EntityEventFailure( entityFailure );
		}

		EntityIndexingFailureContext entityFailure = entityFailures.get( 3 );
		assertThat( entityFailure.failingOperation() ).isEqualTo( "Processing an outbox event." );
		assertThat( entityFailure.throwable() )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Max '3' retries exhausted to process the event. Event will be aborted." );
		hasOneReference( entityFailure.failingEntityReferences(), "indexed", 2 );
	}

	private void setup(int retryDelay) {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b.field( "indexedField", String.class ) );
		failureHandler = new TestFailureHandler();
		sessionFactory = ormSetupHelper.start()
				.withProperty( "hibernate.search.background_failure_handler", failureHandler )
				.withProperty( HibernateOrmMapperOutboxPollingImplSettings.COORDINATION_INTERNAL_CONFIGURER,
						new TestingOutboxPollingInternalConfigurer().outboxEventFilter( eventFilter ) )
				.withProperty( "hibernate.search.coordination.event_processor.retry_delay", retryDelay )
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	private void checkId2EntityEventFailure(EntityIndexingFailureContext entityFailure) {
		assertThat( entityFailure.failingOperation() ).isEqualTo( "Processing an outbox event." );
		assertThat( entityFailure.throwable() )
				.isInstanceOf( SimulatedFailure.class )
				.hasMessageContaining( "Indexing work #2 failed!" );
		hasOneReference( entityFailure.failingEntityReferences(), "indexed", 2 );
	}

	private void hasOneReference(List<EntityReference> entityReferences, String entityName, Object id) {
		assertThat( entityReferences ).hasSize( 1 );
		EntityReference entityReference = entityReferences.get( 0 );
		assertThat( entityReference.name() ).isEqualTo( entityName );
		assertThat( entityReference.id() ).isEqualTo( id );
	}

	private static void awaitFor(ThrowingRunnable assertion) {
		await().timeout( 2, TimeUnit.SECONDS ).untilAsserted( assertion );
	}

	@Entity(name = "indexed")
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

	private static class SimulatedFailure extends RuntimeException {
		SimulatedFailure(String message) {
			super( message );
		}
	}

}
