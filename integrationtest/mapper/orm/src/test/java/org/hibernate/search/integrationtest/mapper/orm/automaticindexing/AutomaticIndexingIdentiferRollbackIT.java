/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.withinTransaction;

import java.util.concurrent.atomic.AtomicReference;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test that Hibernate Search manages to delete entities even when {@code hibernate.use_identifier_rollback=true}.
 */
@TestForIssue(jiraKey = {"HSEARCH-650", "HSEARCH-3985"})
@PortedFromSearch5(original = "org.hibernate.search.test.engine.UsingIdentifierRollbackTest")
public class AutomaticIndexingIdentiferRollbackIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private SessionFactory sessionFactory;

	@Before
	public void before() {
		backendMock.expectAnySchema( EntityWithJpaIdAsDocumentId.NAME );
		backendMock.expectAnySchema( EntityWithNonJpaIdAsDocumentId.NAME );
		sessionFactory = ormSetupHelper
				.start()
				.withProperty( AvailableSettings.USE_IDENTIFIER_ROLLBACK, "true" )
				.setup( EntityWithJpaIdAsDocumentId.class, EntityWithNonJpaIdAsDocumentId.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void jpaIdAsDocumentId() {
		AtomicReference<Integer> entity1Id = new AtomicReference<>();

		withinTransaction( sessionFactory, session -> {
			EntityWithJpaIdAsDocumentId entity1 = new EntityWithJpaIdAsDocumentId();

			session.persist( entity1 );

			entity1Id.set( entity1.getId() );

			backendMock.expectWorks( EntityWithJpaIdAsDocumentId.NAME )
					.add( entity1.getId().toString(), b -> { } )
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		withinTransaction( sessionFactory, session -> {
			EntityWithJpaIdAsDocumentId entity1 = session.getReference( EntityWithJpaIdAsDocumentId.class,
					entity1Id.get() );

			session.delete( entity1 );

			// entity1.getId() will no longer return the ID...
			assertThat( entity1.getId() ).isNull();

			// ... but this should work regardless,
			// because we rely on the entity ID supplied by the delete event.
			backendMock.expectWorks( EntityWithJpaIdAsDocumentId.NAME )
					.delete( entity1Id.get().toString() )
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void nonJpaIdAsDocumentId() {
		AtomicReference<Integer> entity1Id = new AtomicReference<>();

		withinTransaction( sessionFactory, session -> {
			EntityWithNonJpaIdAsDocumentId entity1 = new EntityWithNonJpaIdAsDocumentId();
			entity1.setDocumentId( "document1" );

			session.persist( entity1 );

			entity1Id.set( entity1.getId() );

			backendMock.expectWorks( EntityWithNonJpaIdAsDocumentId.NAME )
					.add( "document1", b -> { } )
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		withinTransaction( sessionFactory, session -> {
			EntityWithNonJpaIdAsDocumentId entity1 = session.getReference( EntityWithNonJpaIdAsDocumentId.class,
					entity1Id.get() );

			session.delete( entity1 );

			// entity1.getId() was reset...
			assertThat( entity1.getId() ).isNull();

			// ... but this should work regardless,
			// because we only rely on the document ID.
			backendMock.expectWorks( EntityWithNonJpaIdAsDocumentId.NAME )
					.delete( "document1" )
					.processedThenExecuted();
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
