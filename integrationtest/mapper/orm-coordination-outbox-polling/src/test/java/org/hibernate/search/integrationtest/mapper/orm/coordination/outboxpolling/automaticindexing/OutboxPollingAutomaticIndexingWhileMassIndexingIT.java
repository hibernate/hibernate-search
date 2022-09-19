/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling.automaticindexing;

import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.concurrent.atomic.AtomicReference;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.analysis.AnalyzerNames;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.CoordinationStrategyExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

@TestForIssue(jiraKey = "HSEARCH-4358")
public class OutboxPollingAutomaticIndexingWhileMassIndexingIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper setupHelper = OrmSetupHelper.withBackendMock( backendMock )
			.coordinationStrategy( CoordinationStrategyExpectations.outboxPollingAndMassIndexing() );

	@Test
	public void singleTenant() throws InterruptedException {
		backendMock.expectSchema( IndexedEntity.NAME, b -> b
				.field( "text", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ) ) );

		SessionFactory sessionFactory = setupHelper.start()
				.setup( IndexedEntity.class );

		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity = new IndexedEntity( 1, "initial" );
			session.persist( entity );

			backendMock.expectWorks( IndexedEntity.NAME )
					.addOrUpdate( String.valueOf( 1 ), b -> b.field( "text", "initial" ) );
		} );

		// Wait for the initial indexing to be over.
		backendMock.verifyExpectationsMet();

		// Before loading entities, we expect mass indexing to perform a few operations
		backendMock.expectIndexScaleWorks( IndexedEntity.NAME )
				.purge()
				.mergeSegments();

		// Upon loading the entity for mass indexing:
		IndexedEntity.getTextConcurrentOperation.set( () -> {
			// 1. We make sure this operation doesn't get executed multiple times.
			IndexedEntity.getTextConcurrentOperation.set( () -> {} );
			// 2. We simulate a concurrent transaction that updates the entity being mass-indexed.
			with( sessionFactory ).runInTransaction( session -> {
				IndexedEntity entity = session.get( IndexedEntity.class, 1 );
				entity.setText( "updated" );
			} );
			// 3. We give the event processor some time to process the change
			// (it shouldn't process it, since mass indexing is in progress).
			try {
				Thread.sleep( 1000 );
			}
			catch (InterruptedException e) {
				throw new RuntimeException( e );
			}
			// 4. We expect nothing to be indexed at this point,
			// because the event processor should be suspended.
			backendMock.verifyExpectationsMet();

			// Later, we expect the mass indexer to reindex the entity it is currently loading, with the initial value...
			backendMock.expectWorks( IndexedEntity.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( String.valueOf( 1 ), b -> b.field( "text", "initial" ) );

			// Then we expect the mass indexer to perform finishing operations...
			backendMock.expectIndexScaleWorks( IndexedEntity.NAME )
					.flush()
					.refresh();

			// ... and ONLY AFTER THAT,
			// we expect the event processor to reindex the same entity with the updated value.
			backendMock.expectWorks( IndexedEntity.NAME )
					.addOrUpdate( String.valueOf( 1 ), b -> b.field( "text", "updated" ) );
		} );

		Search.mapping( sessionFactory )
				.scope( Object.class ).massIndexer()
				.startAndWait();

		backendMock.verifyExpectationsMet();
	}

	@Test
	public void multiTenant() throws InterruptedException {
		String tenant1Id = "tenant1";
		String tenant2Id = "tenant2";

		backendMock.expectSchema( IndexedEntity.NAME, b -> b
				.field( "text", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ) ) );

		SessionFactory sessionFactory = setupHelper.start()
				.tenants( tenant1Id, tenant2Id )
				.setup( IndexedEntity.class );

		with( sessionFactory, tenant1Id ).runInTransaction( session -> {
			IndexedEntity entity = new IndexedEntity( 1, "initial value for tenant 1" );
			session.persist( entity );

			backendMock.expectWorks( IndexedEntity.NAME, tenant1Id )
					.addOrUpdate( String.valueOf( 1 ), b -> b
							.field( "text", "initial value for tenant 1" ) );
		} );

		with( sessionFactory, tenant2Id ).runInTransaction( session -> {
			IndexedEntity entity = new IndexedEntity( 1, "initial value for tenant 2" );
			session.persist( entity );

			backendMock.expectWorks( IndexedEntity.NAME, tenant2Id )
					.addOrUpdate( String.valueOf( 1 ), b -> b
							.field( "text", "initial value for tenant 2" ) );
		} );

		// Wait for the initial indexing to be over.
		backendMock.verifyExpectationsMet();

		// Before loading entities, we expect mass indexing to perform a few operations
		backendMock.expectIndexScaleWorks( IndexedEntity.NAME, tenant1Id )
				.purge()
				.mergeSegments();

		// Upon loading the entity for mass indexing:
		IndexedEntity.getTextConcurrentOperation.set( () -> {
			// 1. We make sure this operation doesn't get executed multiple times.
			IndexedEntity.getTextConcurrentOperation.set( () -> {} );
			// 2. We simulate a concurrent transaction that updates another entity in a different tenant.
			with( sessionFactory, tenant2Id ).runInTransaction( session -> {
				IndexedEntity entity = session.get( IndexedEntity.class, 1 );
				entity.setText( "updated value for tenant 2" );
				backendMock.expectWorks( IndexedEntity.NAME, tenant2Id )
						.addOrUpdate( String.valueOf( 1 ), b -> b
								.field( "text", "updated value for tenant 2" ) );
			} );
			// 3. We expect the resulting event to be processed concurrently,
			// because mass indexing is NOT in progress for this tenant.
			backendMock.verifyExpectationsMet();
			// 4. We simulate a concurrent transaction that updates the entity being mass-indexed, in the same tenant.
			with( sessionFactory, tenant1Id ).runInTransaction( session -> {
				IndexedEntity entity = session.get( IndexedEntity.class, 1 );
				entity.setText( "updated value for tenant 1" );
			} );
			// 5. We give the event processor some time to process the change
			// (it shouldn't process it, since mass indexing is in progress).
			try {
				Thread.sleep( 1000 );
			}
			catch (InterruptedException e) {
				throw new RuntimeException( e );
			}
			// 6. We expect nothing more to be indexed at this point,
			// because the event processor for tenant 1 should be suspended.
			backendMock.verifyExpectationsMet();

			// Later, we expect the mass indexer to reindex the entity it is currently loading, with the initial value...
			backendMock.expectWorks( IndexedEntity.NAME, tenant1Id, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
					.add( String.valueOf( 1 ), b -> b
							.field( "text", "initial value for tenant 1" ) );

			// Then we expect the mass indexer to perform finishing operations...
			backendMock.expectIndexScaleWorks( IndexedEntity.NAME, tenant1Id )
					.flush()
					.refresh();

			// ... and ONLY AFTER THAT,
			// we expect the event processor to reindex the same entity with the updated value.
			backendMock.expectWorks( IndexedEntity.NAME, tenant1Id )
					.addOrUpdate( String.valueOf( 1 ), b -> b
							.field( "text", "updated value for tenant 1" ) );
		} );

		Search.mapping( sessionFactory )
				.scope( Object.class ).massIndexer( tenant1Id )
				.startAndWait();

		backendMock.verifyExpectationsMet();
	}

	@Entity(name = IndexedEntity.NAME)
	@Indexed
	public static class IndexedEntity {

		static final String NAME = "IndexedEntity";

		static volatile AtomicReference<Runnable> getTextConcurrentOperation = new AtomicReference<>( () -> {} );

		private Integer id;
		private String text;

		public IndexedEntity() {
		}

		public IndexedEntity(Integer id, String text) {
			this.id = id;
			this.text = text;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@FullTextField
		public String getText() {
			getTextConcurrentOperation.get().run();
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}

}
