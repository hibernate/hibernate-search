/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.concurrent.atomic.AtomicReference;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Test that Hibernate Search manages to delete entities even when {@code hibernate.use_identifier_rollback=true}.
 */
@TestForIssue(jiraKey = { "HSEARCH-650", "HSEARCH-3985" })
@PortedFromSearch5(original = "org.hibernate.search.test.engine.UsingIdentifierRollbackTest")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AutomaticIndexingIdentiferRollbackIT {

	@RegisterExtension
	public static BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public static OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );
	private SessionFactory sessionFactory;


	@BeforeAll
	void setup() {
		backendMock.expectAnySchema( EntityWithJpaIdAsDocumentId.NAME );
		backendMock.expectAnySchema( EntityWithNonJpaIdAsDocumentId.NAME );
		sessionFactory = ormSetupHelper.start().withProperty(
				AvailableSettings.USE_IDENTIFIER_ROLLBACK, "true" )
				.withAnnotatedTypes( EntityWithJpaIdAsDocumentId.class, EntityWithNonJpaIdAsDocumentId.class )
				.setup();
	}

	@Test
	void jpaIdAsDocumentId() {
		AtomicReference<Integer> entity1Id = new AtomicReference<>();

		with( sessionFactory ).runInTransaction( session -> {
			EntityWithJpaIdAsDocumentId entity1 = new EntityWithJpaIdAsDocumentId();

			session.persist( entity1 );

			entity1Id.set( entity1.getId() );

			backendMock.expectWorks( EntityWithJpaIdAsDocumentId.NAME )
					.add( entity1.getId().toString(), b -> {} );
		} );
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runInTransaction( session -> {
			EntityWithJpaIdAsDocumentId entity1 = session.getReference( EntityWithJpaIdAsDocumentId.class,
					entity1Id.get() );

			session.remove( entity1 );

			// entity1.getId() will no longer return the ID...
			assertThat( entity1.getId() ).isNull();

			// ... but this should work regardless,
			// because we rely on the entity ID supplied by the delete event.
			backendMock.expectWorks( EntityWithJpaIdAsDocumentId.NAME )
					.delete( entity1Id.get().toString() );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void nonJpaIdAsDocumentId() {
		AtomicReference<Integer> entity1Id = new AtomicReference<>();

		with( sessionFactory ).runInTransaction( session -> {
			EntityWithNonJpaIdAsDocumentId entity1 = new EntityWithNonJpaIdAsDocumentId();
			entity1.setDocumentId( "document1" );

			session.persist( entity1 );

			entity1Id.set( entity1.getId() );

			backendMock.expectWorks( EntityWithNonJpaIdAsDocumentId.NAME )
					.add( "document1", b -> {} );
		} );
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runInTransaction( session -> {
			EntityWithNonJpaIdAsDocumentId entity1 = session.getReference( EntityWithNonJpaIdAsDocumentId.class,
					entity1Id.get() );

			session.remove( entity1 );

			// entity1.getId() was reset...
			assertThat( entity1.getId() ).isNull();

			// ... but this should work regardless,
			// because we only rely on the document ID.
			backendMock.expectWorks( EntityWithNonJpaIdAsDocumentId.NAME )
					.delete( "document1" );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Entity(name = EntityWithJpaIdAsDocumentId.NAME)
	@Indexed
	public static class EntityWithJpaIdAsDocumentId {
		static final String NAME = "jpaid";

		@Id
		@GeneratedValue
		private Integer id;

		public Integer getId() {
			return id;
		}
	}

	@Entity(name = EntityWithNonJpaIdAsDocumentId.NAME)
	@Indexed
	public static class EntityWithNonJpaIdAsDocumentId {
		static final String NAME = "nonjpaid";

		@Id
		@GeneratedValue
		private Integer id;

		@DocumentId
		private String documentId;

		public Integer getId() {
			return id;
		}

		public String getDocumentId() {
			return documentId;
		}

		public void setDocumentId(String documentId) {
			this.documentId = documentId;
		}
	}
}
