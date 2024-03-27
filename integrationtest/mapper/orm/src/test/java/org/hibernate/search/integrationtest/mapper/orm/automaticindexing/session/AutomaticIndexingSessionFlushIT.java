/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests the impact of different kinds of {@link Session#flush()} on automatic indexing.
 * Each one has to trigger a {@link PojoIndexingPlan#process()} exactly when the flush is expected.
 */
@TestForIssue(jiraKey = "HSEARCH-3360")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AutomaticIndexingSessionFlushIT {

	@RegisterExtension
	public static BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public static OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );
	private SessionFactory sessionFactory;

	@BeforeAll
	void setup() {
		backendMock.expectAnySchema( IndexedEntity.INDEX_NAME );
		sessionFactory = ormSetupHelper.start().withAnnotatedTypes( IndexedEntity.class ).setup();
	}

	@Test
	void onExplicitFlush() {
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity( 1, "number1" );
			session.setHibernateFlushMode( FlushMode.AUTO );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX_NAME )
					.createFollowingWorks()
					.add( "1", b -> b.field( "text", "number1" ) );

			session.flush();
			if ( ormSetupHelper.areEntitiesProcessedInSession() ) {
				// Entities should be processed and works created on flush
				backendMock.verifyExpectationsMet();
			}

			backendMock.expectWorks( IndexedEntity.INDEX_NAME )
					.executeFollowingWorks()
					.add( "1", b -> b.field( "text", "number1" ) );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void onAutoFlush() {
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity( 1, "number1" );
			session.setHibernateFlushMode( FlushMode.AUTO );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX_NAME )
					.createFollowingWorks()
					.add( "1", b -> b.field( "text", "number1" ) );

			// An auto flush is performed on query invocation
			List<?> resultList = session.createQuery( "select i from IndexedEntity i", IndexedEntity.class )
					.setHibernateFlushMode( FlushMode.AUTO )
					.getResultList();

			if ( ormSetupHelper.areEntitiesProcessedInSession() ) {
				// Entities should be processed and works created on flush
				backendMock.verifyExpectationsMet();
			}

			assertThat( resultList ).hasSize( 1 );

			backendMock.expectWorks( IndexedEntity.INDEX_NAME )
					.executeFollowingWorks()
					.add( "1", b -> b.field( "text", "number1" ) );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Entity(name = "IndexedEntity")
	@Indexed(index = IndexedEntity.INDEX_NAME)
	public static class IndexedEntity {
		static final String INDEX_NAME = "indexName";

		@Id
		private Integer id;
		@GenericField
		private String text;

		protected IndexedEntity() { // For ORM
		}

		IndexedEntity(int id, String text) {
			this.id = id;
			this.text = text;
		}
	}
}
