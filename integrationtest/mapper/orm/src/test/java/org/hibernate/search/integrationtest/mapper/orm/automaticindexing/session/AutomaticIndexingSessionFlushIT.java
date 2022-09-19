/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.session;

import static org.junit.Assert.assertEquals;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.ReusableOrmSetupHolder;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

/**
 * Tests the impact of different kinds of {@link Session#flush()} on automatic indexing.
 * Each one has to trigger a {@link PojoIndexingPlan#process()} exactly when the flush is expected.
 */
@TestForIssue(jiraKey = "HSEARCH-3360")
public class AutomaticIndexingSessionFlushIT {

	@ClassRule
	public static BackendMock backendMock = new BackendMock();

	@ClassRule
	public static ReusableOrmSetupHolder setupHolder = ReusableOrmSetupHolder.withBackendMock( backendMock );

	@Rule
	public MethodRule setupHolderMethodRule = setupHolder.methodRule();

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext) {
		backendMock.expectAnySchema( IndexedEntity.INDEX_NAME );
		setupContext.withAnnotatedTypes( IndexedEntity.class );
	}

	@Test
	public void onExplicitFlush() {
		setupHolder.runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity( 1, "number1" );
			session.setHibernateFlushMode( FlushMode.AUTO );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX_NAME )
					.createFollowingWorks()
					.add( "1", b -> b.field( "text", "number1" ) );

			session.flush();
			if ( setupHolder.areEntitiesProcessedInSession() ) {
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
	public void onAutoFlush() {
		setupHolder.runInTransaction( session -> {
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

			if ( setupHolder.areEntitiesProcessedInSession() ) {
				// Entities should be processed and works created on flush
				backendMock.verifyExpectationsMet();
			}

			assertEquals( 1, resultList.size() );

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
