/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.massindexing;

import static org.assertj.core.api.Fail.fail;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.PersistenceTypeKey;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.StubEntityLoadingBinder;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.StubLoadingContext;
import org.hibernate.search.mapper.pojo.loading.mapping.annotation.EntityLoadingBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.massindexing.MassIndexer;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@TestForIssue(jiraKey = "HSEARCH-3529")
class MassIndexingPrimitiveIdIT {

	@RegisterExtension
	public final BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public final StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	private SearchMapping mapping;

	private final StubLoadingContext loadingContext = new StubLoadingContext();

	@BeforeEach
	void setup() {
		backendMock.expectAnySchema( EntityWithPrimitiveId.NAME );

		mapping = setupHelper.start()
				.expectCustomBeans()
				.setup( EntityWithPrimitiveId.class );

		backendMock.verifyExpectationsMet();

		initData();
	}

	@Test
	void entityWithPrimitiveId() {
		try ( SearchSession searchSession = mapping.createSession() ) {
			MassIndexer indexer = searchSession.massIndexer()
					// Simulate passing information to connect to a DB, ...
					.context( StubLoadingContext.class, loadingContext )
					.mergeSegmentsOnFinish( true );

			// add operations on indexes can follow any random order,
			// since they are executed by different threads
			backendMock.expectWorks(
					EntityWithPrimitiveId.NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
			)
					.add( "1", b -> {} )
					.add( "2", b -> {} )
					.add( "3", b -> {} );

			// purgeAtStart and mergeSegmentsAfterPurge are enabled by default,
			// so we expect 1 purge, 1 mergeSegments and 1 flush calls in this order:
			backendMock.expectIndexScaleWorks( EntityWithPrimitiveId.NAME, searchSession.tenantIdentifierValue() )
					.purge()
					.mergeSegments()
					.mergeSegments()
					.flush()
					.refresh();

			try {
				indexer.startAndWait();
			}
			catch (InterruptedException e) {
				fail( "Unexpected InterruptedException: " + e.getMessage() );
			}

		}

		backendMock.verifyExpectationsMet();
	}

	private void initData() {
		persist( new EntityWithPrimitiveId( 1 ) );
		persist( new EntityWithPrimitiveId( 2 ) );
		persist( new EntityWithPrimitiveId( 3 ) );
	}

	private void persist(EntityWithPrimitiveId entity) {
		loadingContext.persistenceMap( EntityWithPrimitiveId.PERSISTENCE_KEY ).put( entity.id, entity );
	}

	@SearchEntity(name = EntityWithPrimitiveId.NAME,
			loadingBinder = @EntityLoadingBinderRef(type = StubEntityLoadingBinder.class))
	@Indexed
	public static class EntityWithPrimitiveId {

		public static final String NAME = "EntityWithPrimitiveId";
		public static final PersistenceTypeKey<EntityWithPrimitiveId, Integer> PERSISTENCE_KEY =
				new PersistenceTypeKey<>( EntityWithPrimitiveId.class, Integer.class );

		@DocumentId
		private int id;

		public EntityWithPrimitiveId() {
		}

		public EntityWithPrimitiveId(int id) {
			this.id = id;
		}
	}
}
