/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing;

import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.withinTransaction;
import static org.junit.Assert.assertEquals;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests the behaviour of different kinds of {@link Session#flush()}.
 * Each of ones has to trigger a {@link PojoIndexingPlan#process()} exactly when the flush is expected.
 */
@TestForIssue( jiraKey = "HSEARCH-3360" )
public class SearchSessionFlushIT {

	@Rule
	public BackendMock backendMock = new BackendMock( "myBackend" );

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMocks( backendMock );

	private SessionFactory sessionFactory;

	@Before
	public void before() {
		backendMock.expectAnySchema( IndexedEntity.INDEX_NAME );
		sessionFactory = ormSetupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void onExplicitFlush() {
		withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity( 1, "number1" );
			session.setHibernateFlushMode( FlushMode.AUTO );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX_NAME )
					.add( "1", b -> b.field( "text", "number1" ) )
					.processed();

			session.flush();
			backendMock.verifyExpectationsMet();

			backendMock.expectWorks( IndexedEntity.INDEX_NAME )
					.add( "1", b -> b.field( "text", "number1" ) )
					.executed();
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void onAutoFlush() {
		withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity( 1, "number1" );
			session.setHibernateFlushMode( FlushMode.AUTO );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX_NAME )
					.add( "1", b -> b.field( "text", "number1" ) )
					.processed();

			// An auto flush is performed on query invocation
			List<?> resultList = session.createQuery( "select i from IndexedEntity i" )
					.setHibernateFlushMode( FlushMode.AUTO )
					.getResultList();

			backendMock.verifyExpectationsMet();

			assertEquals( 1, resultList.size() );

			backendMock.expectWorks( IndexedEntity.INDEX_NAME )
					.add( "1", b -> b.field( "text", "number1" ) )
					.executed();
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

		IndexedEntity(int id, String text) {
			this.id = id;
			this.text = text;
		}
	}
}
