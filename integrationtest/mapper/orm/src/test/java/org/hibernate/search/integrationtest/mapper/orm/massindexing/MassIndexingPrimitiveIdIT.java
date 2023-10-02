/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.massindexing;

import static org.assertj.core.api.Fail.fail;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@TestForIssue(jiraKey = "HSEARCH-3529")
class MassIndexingPrimitiveIdIT {

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private SessionFactory sessionFactory;

	@BeforeEach
	void setup() {
		backendMock.expectAnySchema( EntityWithPrimitiveId.INDEX );

		sessionFactory = ormSetupHelper.start()
				.withPropertyRadical( HibernateOrmMapperSettings.Radicals.INDEXING_LISTENERS_ENABLED, false )
				.setup( EntityWithPrimitiveId.class );

		backendMock.verifyExpectationsMet();

		initData();
	}

	@Test
	void entityWithPrimitiveId() {
		with( sessionFactory ).runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			MassIndexer indexer = searchSession.massIndexer();

			// add operations on indexes can follow any random order,
			// since they are executed by different threads
			backendMock.expectWorks(
					EntityWithPrimitiveId.INDEX, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
			)
					.add( "1", b -> {} )
					.add( "2", b -> {} )
					.add( "3", b -> {} );

			// purgeAtStart and mergeSegmentsAfterPurge are enabled by default,
			// so we expect 1 purge, 1 mergeSegments and 1 flush calls in this order:
			backendMock.expectIndexScaleWorks( EntityWithPrimitiveId.INDEX, session.getTenantIdentifier() )
					.purge()
					.mergeSegments()
					.flush()
					.refresh();

			try {
				indexer.startAndWait();
			}
			catch (InterruptedException e) {
				fail( "Unexpected InterruptedException: " + e.getMessage() );
			}

		} );

		backendMock.verifyExpectationsMet();
	}

	private void initData() {
		with( sessionFactory ).runInTransaction( session -> {
			session.persist( new EntityWithPrimitiveId( 1 ) );
			session.persist( new EntityWithPrimitiveId( 2 ) );
			session.persist( new EntityWithPrimitiveId( 3 ) );
		} );
	}

	@Entity
	@Table(name = "book")
	@Indexed(index = EntityWithPrimitiveId.INDEX)
	public static class EntityWithPrimitiveId {

		public static final String INDEX = "EntityWithPrimitiveId";

		@Id
		private int id;

		public EntityWithPrimitiveId() {
		}

		public EntityWithPrimitiveId(int id) {
			this.id = id;
		}
	}
}
