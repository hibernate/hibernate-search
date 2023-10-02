/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.massindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

import java.lang.invoke.MethodHandles;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.PersistenceTypeKey;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.StubEntityLoadingBinder;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.StubLoadingContext;
import org.hibernate.search.mapper.pojo.loading.mapping.annotation.EntityLoadingBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingEnvironment;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.massindexing.MassIndexer;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class MassIndexingEnvironmentIT {

	@RegisterExtension
	public final BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public final StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	private SearchMapping mapping;

	private final StubLoadingContext loadingContext = new StubLoadingContext();

	@BeforeEach
	void setup() {
		backendMock.expectAnySchema( Entity.NAME );

		mapping = setupHelper.start()
				.expectCustomBeans()
				.setup( Entity.class );

		backendMock.verifyExpectationsMet();

		initData();
	}

	@Test
	void success() throws InterruptedException {
		try ( SearchSession searchSession = mapping.createSession() ) {
			Queue<String> before = new ArrayBlockingQueue<>( 10 );
			Queue<String> after = new ArrayBlockingQueue<>( 10 );

			MassIndexer indexer = searchSession.massIndexer()
					// Simulate passing information to connect to a DB, ...
					.context( StubLoadingContext.class, loadingContext )
					.mergeSegmentsOnFinish( true )
					.typesToIndexInParallel( 1 )
					.threadsToLoadObjects( 1 )
					.environment( new MassIndexingEnvironment() {
						@Override
						public void beforeExecution(Context context) {
							before.add( Thread.currentThread().getName() );
						}

						@Override
						public void afterExecution(Context context) {
							after.add( Thread.currentThread().getName() );
						}
					} );

			backendMock.expectWorks(
					Entity.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
			)
					.add( "1", b -> {} );

			// purgeAtStart and mergeSegmentsAfterPurge are enabled by default,
			// so we expect 1 purge, 1 mergeSegments and 1 flush calls in this order:
			backendMock.expectIndexScaleWorks( Entity.NAME, searchSession.tenantIdentifierValue() )
					.purge()
					.mergeSegments()
					.mergeSegments()
					.flush()
					.refresh();

			Futures.unwrappedExceptionGet( indexer.start().toCompletableFuture() );

			assertThat( before ).containsOnly(
					"Hibernate Search - Mass indexing - Entity - Entity loading - 0",
					"Hibernate Search - Mass indexing - Entity - ID loading - 0"
			);
			assertThat( after ).containsOnly(
					"Hibernate Search - Mass indexing - Entity - Entity loading - 0",
					"Hibernate Search - Mass indexing - Entity - ID loading - 0"
			);
		}

		backendMock.verifyExpectationsMet();
	}

	@Test
	void testFailingEntityLoading() {
		testFailingBeforeHook(
				MassIndexingEnvironment.EntityLoadingContext.class,
				"don't call me in entity loading."
		);
	}

	@Test
	void testFailingEntityIdentifierLoading() {
		testFailingBeforeHook(
				MassIndexingEnvironment.EntityIdentifierLoadingContext.class,
				"don't call me in identifier loading."
		);
	}

	public void testFailingBeforeHook(Class<? extends MassIndexingEnvironment.Context> contextClass, String message) {
		try ( SearchSession searchSession = mapping.createSession() ) {
			Queue<String> threadNames = new ArrayBlockingQueue<>( 10 );

			MassIndexer indexer = searchSession.massIndexer()
					// Simulate passing information to connect to a DB, ...
					.context( StubLoadingContext.class, loadingContext )
					.mergeSegmentsOnFinish( true )
					.typesToIndexInParallel( 1 )
					.threadsToLoadObjects( 1 )
					.environment( new MassIndexingEnvironment() {
						@Override
						public void beforeExecution(Context context) {
							if ( contextClass.isAssignableFrom( EntityIdentifierLoadingContext.class )
									&& context instanceof EntityIdentifierLoadingContext ) {
								throw new UnsupportedOperationException( "don't call me in identifier loading." );
							}
							if ( contextClass.isAssignableFrom( EntityLoadingContext.class )
									&& context instanceof EntityIdentifierLoadingContext ) {
								throw new UnsupportedOperationException( "don't call me in entity loading." );
							}

							// if we've successfully executed before - let's save the name so we can remove it in after
							threadNames.add( Thread.currentThread().getName() );
						}

						@Override
						public void afterExecution(Context context) {
							if ( !threadNames.remove( Thread.currentThread().getName() ) ) {
								fail( "We should not have reached this state as after call is only possible " +
										"with successful before call which would've added the thread name." );
							}
						}
					} );

			// purgeAtStart and mergeSegmentsAfterPurge are enabled by default,
			// so we expect 1 purge, 1 mergeSegments after that we'll get exceptions so no flush or refresh:
			backendMock.expectIndexScaleWorks( Entity.NAME, searchSession.tenantIdentifierValue() )
					.purge()
					.mergeSegments();

			assertThatThrownBy( () -> Futures.unwrappedExceptionGet( indexer.start().toCompletableFuture() ) )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll(
							"1 failure(s) occurred during mass indexing",
							"First failure: ",
							message
					);

			await().untilAsserted( () -> assertThat( threadNames ).isEmpty() );
		}
	}

	private void initData() {
		persist( new Entity( 1 ) );
	}

	private void persist(Entity entity) {
		loadingContext.persistenceMap( Entity.PERSISTENCE_KEY ).put( entity.id, entity );
	}

	@SearchEntity(name = Entity.NAME,
			loadingBinder = @EntityLoadingBinderRef(type = StubEntityLoadingBinder.class))
	@Indexed
	public static class Entity {

		public static final String NAME = "Entity";
		public static final PersistenceTypeKey<Entity, Integer> PERSISTENCE_KEY =
				new PersistenceTypeKey<>( Entity.class, Integer.class );

		@DocumentId
		private Integer id;

		public Entity() {
		}

		public Entity(int id) {
			this.id = id;
		}
	}
}
