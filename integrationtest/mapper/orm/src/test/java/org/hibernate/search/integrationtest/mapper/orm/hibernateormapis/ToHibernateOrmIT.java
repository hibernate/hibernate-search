/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.hibernateormapis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendUtils.reference;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;
import org.hibernate.search.util.impl.test.SubTest;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test the compatibility layer between our APIs and Hibernate ORM APIs.
 */
public class ToHibernateOrmIT {

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private SessionFactory sessionFactory;

	@Before
	public void setup() {
		backendMock.expectAnySchema( IndexedEntity.INDEX );
		sessionFactory = ormSetupHelper.start()
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setText( "this is text (1)" );
			IndexedEntity entity2 = new IndexedEntity();
			entity2.setId( 2 );
			entity2.setText( "some more text (2)" );

			session.persist( entity1 );
			session.persist( entity2 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "text", entity1.getText() )
					)
					.add( "2", b -> b
							.field( "text", entity2.getText() )
					)
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void toHibernateOrmSessionFactory() {
		SearchMapping searchMapping = Search.mapping( sessionFactory );
		assertThat( searchMapping.toOrmSessionFactory() ).isSameAs( sessionFactory );
	}

	@Test
	public void toHibernateOrmSession() {
		OrmUtils.withinSession( sessionFactory, session -> {
			SearchSession searchSession = Search.session( session );
			assertThat( searchSession.toOrmSession() ).isSameAs( session );
		} );
	}

	@Test
	public void toHibernateOrmSession_withClosedSession() {
		Session session = null;
		try {
			session = sessionFactory.openSession();
		}
		finally {
			if ( session != null ) {
				session.close();
			}
		}

		Session closedSession = session;
		SubTest.expectException( () -> {
			Search.session( closedSession );
		} )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessage( "HSEARCH800016: Error trying to access Hibernate ORM session." );
	}

	@Test
	public void toHibernateOrmQuery() {
		OrmUtils.withinSession( sessionFactory, session -> {
			SearchSession searchSession = Search.session( session );
			Query<IndexedEntity> query = Search.toOrmQuery( createSimpleQuery( searchSession ) );
			assertThat( query ).isNotNull();
		} );
	}

	@Test
	public void list() {
		OrmUtils.withinSession( sessionFactory, session -> {
			SearchSession searchSession = Search.session( session );
			Query<IndexedEntity> query = Search.toOrmQuery( createSimpleQuery( searchSession ) );

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.INDEX ),
					b -> { },
					StubSearchWorkBehavior.of(
							6L,
							reference( IndexedEntity.INDEX, "1" ),
							reference( IndexedEntity.INDEX, "2" )
					)
			);
			List<IndexedEntity> result = query.list();
			backendMock.verifyExpectationsMet();
			assertThat( result )
					.containsExactly(
							session.getReference( IndexedEntity.class, 1 ),
							session.getReference( IndexedEntity.class, 2 )
					);
		} );
	}

	@Test
	public void uniqueResult() {
		OrmUtils.withinSession( sessionFactory, session -> {
			SearchSession searchSession = Search.session( session );
			Query<IndexedEntity> query = Search.toOrmQuery( createSimpleQuery( searchSession ) );

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.INDEX ),
					b -> { },
					StubSearchWorkBehavior.of(
							1L,
							reference( IndexedEntity.INDEX, "1" )
					)
			);
			IndexedEntity result = query.uniqueResult();
			backendMock.verifyExpectationsMet();
			assertThat( result )
					.isEqualTo( session.getReference( IndexedEntity.class, 1 ) );

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.INDEX ),
					b -> { },
					StubSearchWorkBehavior.empty()
			);
			result = query.uniqueResult();
			backendMock.verifyExpectationsMet();
			assertThat( result ).isNull();

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.INDEX ),
					b -> { },
					StubSearchWorkBehavior.of(
							2L,
							reference( IndexedEntity.INDEX, "1" ),
							reference( IndexedEntity.INDEX, "2" )
					)
			);
			SubTest.expectException( () -> {
				query.uniqueResult();
			} )
					.assertThrown()
					.isInstanceOf( org.hibernate.NonUniqueResultException.class );
			backendMock.verifyExpectationsMet();

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.INDEX ),
					b -> { },
					StubSearchWorkBehavior.of(
							2L,
							reference( IndexedEntity.INDEX, "1" ),
							reference( IndexedEntity.INDEX, "1" )
					)
			);
			result = query.uniqueResult();
			backendMock.verifyExpectationsMet();
			assertThat( result )
					.isEqualTo( session.getReference( IndexedEntity.class, 1 ) );
		} );
	}

	@Test
	public void pagination() {
		OrmUtils.withinSession( sessionFactory, session -> {
			SearchSession searchSession = Search.session( session );
			Query<IndexedEntity> query = Search.toOrmQuery( createSimpleQuery( searchSession ) );

			assertThat( query.getFirstResult() ).isEqualTo( 0 );
			assertThat( query.getMaxResults() ).isEqualTo( Integer.MAX_VALUE );

			query.setFirstResult( 3 );
			query.setMaxResults( 2 );

			assertThat( query.getFirstResult() ).isEqualTo( 3 );
			assertThat( query.getMaxResults() ).isEqualTo( 2 );

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.INDEX ),
					b -> b
							.offset( 3 )
							.limit( 2 ),
					StubSearchWorkBehavior.empty()
			);
			query.list();
			backendMock.verifyExpectationsMet();
		} );
	}

	@Test
	public void timeout_dsl() {
		OrmUtils.withinSession( sessionFactory, session -> {
			SearchSession searchSession = Search.session( session );
			Query<IndexedEntity> query = Search.toOrmQuery(
					searchSession.search( IndexedEntity.class )
							.predicate( f -> f.matchAll() )
							.failAfter( 2, TimeUnit.SECONDS )
							.toQuery()
			);

			SearchException timeoutException = new SearchException( "Timed out" );

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.INDEX ),
					b -> b.failAfter( 2, TimeUnit.SECONDS ),
					StubSearchWorkBehavior.failing( () -> timeoutException )
			);

			// Just check that the exception is propagated
			SubTest.expectException( () -> query.list() )
					.assertThrown()
					.isSameAs( timeoutException );
		} );
	}

	@Test
	public void timeout_jpaHint() {
		OrmUtils.withinSession( sessionFactory, session -> {
			SearchSession searchSession = Search.session( session );
			Query<IndexedEntity> query = Search.toOrmQuery( createSimpleQuery( searchSession ) );

			query.setHint( "javax.persistence.query.timeout", 200 );

			SearchException timeoutException = new SearchException( "Timed out" );

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.INDEX ),
					b -> b.failAfter( 200, TimeUnit.MILLISECONDS ),
					StubSearchWorkBehavior.failing( () -> timeoutException )
			);

			// Just check that the exception is propagated
			SubTest.expectException( () -> query.list() )
					.assertThrown()
					.isSameAs( timeoutException );
		} );
	}

	@Test
	public void timeout_ormHint() {
		OrmUtils.withinSession( sessionFactory, session -> {
			SearchSession searchSession = Search.session( session );
			Query<IndexedEntity> query = Search.toOrmQuery( createSimpleQuery( searchSession ) );

			query.setHint( "org.hibernate.timeout", 4 );

			SearchException timeoutException = new SearchException( "Timed out" );

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.INDEX ),
					b -> b.failAfter( 4, TimeUnit.SECONDS ),
					StubSearchWorkBehavior.failing( () -> timeoutException )
			);

			// Just check that the exception is propagated
			SubTest.expectException( () -> query.list() )
					.assertThrown()
					.isSameAs( timeoutException );
		} );
	}

	@Test
	public void timeout_setter() {
		OrmUtils.withinSession( sessionFactory, session -> {
			SearchSession searchSession = Search.session( session );
			Query<IndexedEntity> query = Search.toOrmQuery( createSimpleQuery( searchSession ) );

			query.setTimeout( 3 );

			SearchException timeoutException = new SearchException( "Timed out" );

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.INDEX ),
					b -> b.failAfter( 3, TimeUnit.SECONDS ),
					StubSearchWorkBehavior.failing( () -> timeoutException )
			);

			// Just check that the exception is propagated
			SubTest.expectException( () -> query.list() )
					.assertThrown()
					.isSameAs( timeoutException );
		} );
	}

	@Test
	public void timeout_override_ormHint() {
		OrmUtils.withinSession( sessionFactory, session -> {
			SearchSession searchSession = Search.session( session );
			Query<IndexedEntity> query = Search.toOrmQuery(
					searchSession.search( IndexedEntity.class )
							.predicate( f -> f.matchAll() )
							.failAfter( 2, TimeUnit.SECONDS )
							.toQuery()
			);

			query.setHint( "org.hibernate.timeout", 4 );

			SearchException timeoutException = new SearchException( "Timed out" );

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.INDEX ),
					b -> b.failAfter( 4, TimeUnit.SECONDS ),
					StubSearchWorkBehavior.failing( () -> timeoutException )
			);

			// Just check that the exception is propagated
			SubTest.expectException( () -> query.list() )
					.assertThrown()
					.isSameAs( timeoutException );
		} );
	}

	@Test
	public void timeout_override_setter() {
		OrmUtils.withinSession( sessionFactory, session -> {
			SearchSession searchSession = Search.session( session );
			Query<IndexedEntity> query = Search.toOrmQuery(
					searchSession.search( IndexedEntity.class )
							.predicate( f -> f.matchAll() )
							.failAfter( 2, TimeUnit.SECONDS )
							.toQuery()
			);

			query.setTimeout( 3 );

			SearchException timeoutException = new SearchException( "Timed out" );

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.INDEX ),
					b -> b.failAfter( 3, TimeUnit.SECONDS ),
					StubSearchWorkBehavior.failing( () -> timeoutException )
			);

			// Just check that the exception is propagated
			SubTest.expectException( () -> query.list() )
					.assertThrown()
					.isSameAs( timeoutException );
		} );
	}

	@Test
	@TestForIssue( jiraKey = "HSEARCH-1857" )
	public void reuseSearchSessionAfterOrmSessionIsClosed_noMatching() {
		Session session = sessionFactory.openSession();
		SearchSession searchSession = Search.session( session );
		// a SearchSession instance is created lazily,
		// so we need to use it to have an instance of it
		createSimpleQuery( searchSession );
		session.close();

		SubTest.expectException( () -> {
			createSimpleQuery( searchSession );
		} )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessage( "HSEARCH800017: Underlying Hibernate ORM Session seems to be closed." );
	}

	@Test
	public void lazyCreateSearchSessionAfterOrmSessionIsClosed() {
		Session session = sessionFactory.openSession();
		// Search session is not created, since we don't use it
		SearchSession searchSession = Search.session( session );
		session.close();

		SubTest.expectException( () -> {
			createSimpleQuery( searchSession );
		} )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessage( "HSEARCH800017: Underlying Hibernate ORM Session seems to be closed." );
	}

	@Test
	@TestForIssue( jiraKey = "HSEARCH-1857" )
	public void reuseSearchQueryAfterOrmSessionIsClosed_noMatching() {
		Session session = sessionFactory.openSession();
		SearchSession searchSession = Search.session( session );
		SearchQuery<IndexedEntity> query = createSimpleQuery( searchSession );
		session.close();

		backendMock.expectSearchObjects(
				Arrays.asList( IndexedEntity.INDEX ),
				b -> { },
				// The call will fail, this doesn't matter
				StubSearchWorkBehavior.empty()
		);

		SubTest.expectException( () -> {
			query.fetchAllHits();
		} )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessage( "HSEARCH800017: Underlying Hibernate ORM Session seems to be closed." );
	}

	private SearchQuery<IndexedEntity> createSimpleQuery(SearchSession searchSession) {
		return searchSession.search( IndexedEntity.class )
				.asEntity()
				.predicate( f -> f.matchAll() )
				.toQuery();
	}

	@Entity
	@Table(name = "indexed")
	@Indexed(index = IndexedEntity.INDEX)
	public static class IndexedEntity {

		public static final String INDEX = "IndexedEntity";

		@Id
		private Integer id;

		@FullTextField(analyzer = "myAnalyzer")
		private String text;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

	}

}
