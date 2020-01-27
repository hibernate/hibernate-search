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
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Id;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.QueryTimeoutException;
import javax.persistence.TypedQuery;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.SearchTimeoutException;
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
 * Test the compatibility layer between our APIs and JPA APIs.
 */
public class ToJpaIT {

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void setup() {
		backendMock.expectAnySchema( IndexedEntity.NAME );
		entityManagerFactory = ormSetupHelper.start()
				.withProperty( AvailableSettings.JPA_QUERY_COMPLIANCE, true )
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setText( "this is text (1)" );
			IndexedEntity entity2 = new IndexedEntity();
			entity2.setId( 2 );
			entity2.setText( "some more text (2)" );

			entityManager.persist( entity1 );
			entityManager.persist( entity2 );

			backendMock.expectWorks( IndexedEntity.NAME )
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
	public void toJpaEntityManagerFactory() {
		SearchMapping searchMapping = Search.mapping( entityManagerFactory );
		assertThat( searchMapping.toEntityManagerFactory() ).isSameAs( entityManagerFactory );
	}

	@Test
	public void toJpaEntityManager() {
		OrmUtils.withinEntityManager( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			assertThat( searchSession.toEntityManager() ).isSameAs( entityManager );
		} );
	}

	@Test
	public void toJpaEntityManager_withClosedEntityManager() {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		try {
			entityManager = entityManagerFactory.createEntityManager();
		}
		finally {
			if ( entityManager != null ) {
				entityManager.close();
			}
		}

		EntityManager closedEntityManager = entityManager;
		SubTest.expectException( () -> {
			Search.session( closedEntityManager );
		} )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessage( "HSEARCH800016: Error trying to access Hibernate ORM session." );
	}

	@Test
	public void toJpaQuery() {
		OrmUtils.withinEntityManager( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			TypedQuery<IndexedEntity> query = Search.toJpaQuery( createSimpleQuery( searchSession ) );
			assertThat( query ).isNotNull();
		} );
	}

	@Test
	public void getResultList() {
		OrmUtils.withinEntityManager( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			TypedQuery<IndexedEntity> query = Search.toJpaQuery( createSimpleQuery( searchSession ) );

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.NAME ),
					b -> { },
					StubSearchWorkBehavior.of(
							6L,
							reference( IndexedEntity.NAME, "1" ),
							reference( IndexedEntity.NAME, "2" )
					)
			);
			List<IndexedEntity> result = query.getResultList();
			backendMock.verifyExpectationsMet();
			assertThat( result )
					.containsExactly(
							entityManager.getReference( IndexedEntity.class, 1 ),
							entityManager.getReference( IndexedEntity.class, 2 )
					);
		} );
	}

	@Test
	public void getSingleResult() {
		OrmUtils.withinEntityManager( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			TypedQuery<IndexedEntity> query = Search.toJpaQuery( createSimpleQuery( searchSession ) );

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.NAME ),
					b -> { },
					StubSearchWorkBehavior.of(
							1L,
							reference( IndexedEntity.NAME, "1" )
					)
			);
			IndexedEntity result = query.getSingleResult();
			backendMock.verifyExpectationsMet();
			assertThat( result )
					.isEqualTo( entityManager.getReference( IndexedEntity.class, 1 ) );

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.NAME ),
					b -> { },
					StubSearchWorkBehavior.empty()
			);
			SubTest.expectException( () -> {
				query.getSingleResult();
			} )
					.assertThrown()
					.isInstanceOf( NoResultException.class );
			backendMock.verifyExpectationsMet();

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.NAME ),
					b -> { },
					StubSearchWorkBehavior.of(
							2L,
							reference( IndexedEntity.NAME, "1" ),
							reference( IndexedEntity.NAME, "2" )
					)
			);
			SubTest.expectException( () -> {
				query.getSingleResult();
			} )
					.assertThrown()
					.isInstanceOf( NonUniqueResultException.class );
			backendMock.verifyExpectationsMet();

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.NAME ),
					b -> { },
					StubSearchWorkBehavior.of(
							2L,
							reference( IndexedEntity.NAME, "1" ),
							reference( IndexedEntity.NAME, "1" )
					)
			);
			result = query.getSingleResult();
			backendMock.verifyExpectationsMet();
			assertThat( result )
					.isEqualTo( entityManager.getReference( IndexedEntity.class, 1 ) );
		} );
	}

	@Test
	public void pagination() {
		OrmUtils.withinEntityManager( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			TypedQuery<IndexedEntity> query = Search.toJpaQuery( createSimpleQuery( searchSession ) );

			assertThat( query.getFirstResult() ).isEqualTo( 0 );
			assertThat( query.getMaxResults() ).isEqualTo( Integer.MAX_VALUE );

			query.setFirstResult( 3 );
			query.setMaxResults( 2 );

			assertThat( query.getFirstResult() ).isEqualTo( 3 );
			assertThat( query.getMaxResults() ).isEqualTo( 2 );

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.NAME ),
					b -> b
							.offset( 3 )
							.limit( 2 ),
					StubSearchWorkBehavior.empty()
			);
			query.getResultList();
			backendMock.verifyExpectationsMet();
		} );
	}

	@Test
	public void timeout_dsl() {
		OrmUtils.withinEntityManager( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			TypedQuery<IndexedEntity> query = Search.toJpaQuery(
					searchSession.search( IndexedEntity.class )
							.where( f -> f.matchAll() )
							.failAfter( 2, TimeUnit.SECONDS )
							.toQuery()
			);

			SearchTimeoutException timeoutException = new SearchTimeoutException( "Timed out" );

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.NAME ),
					b -> b.failAfter( 2, TimeUnit.SECONDS ),
					StubSearchWorkBehavior.failing( () -> timeoutException )
			);

			// Just check that the exception is propagated
			SubTest.expectException( () -> query.getResultList() )
					.assertThrown()
					.isInstanceOf( QueryTimeoutException.class )
					.hasCause( timeoutException );
		} );
	}

	@Test
	public void timeout_jpaHint() {
		OrmUtils.withinEntityManager( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			TypedQuery<IndexedEntity> query = Search.toJpaQuery( createSimpleQuery( searchSession ) );

			query.setHint( "javax.persistence.query.timeout", 200 );

			SearchTimeoutException timeoutException = new SearchTimeoutException( "Timed out" );

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.NAME ),
					b -> b.failAfter( 200, TimeUnit.MILLISECONDS ),
					StubSearchWorkBehavior.failing( () -> timeoutException )
			);

			// Just check that the exception is propagated
			SubTest.expectException( () -> query.getResultList() )
					.assertThrown()
					.isInstanceOf( QueryTimeoutException.class )
					.hasCause( timeoutException );
		} );
	}

	@Test
	public void timeout_override() {
		OrmUtils.withinEntityManager( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			TypedQuery<IndexedEntity> query = Search.toJpaQuery(
					searchSession.search( IndexedEntity.class )
							.where( f -> f.matchAll() )
							.failAfter( 2, TimeUnit.SECONDS )
							.toQuery()
			);

			query.setHint( "javax.persistence.query.timeout", 200 );

			SearchTimeoutException timeoutException = new SearchTimeoutException( "Timed out" );

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.NAME ),
					b -> b.failAfter( 200, TimeUnit.MILLISECONDS ),
					StubSearchWorkBehavior.failing( () -> timeoutException )
			);

			// Just check that the exception is propagated
			SubTest.expectException( () -> query.getResultList() )
					.assertThrown()
					.isInstanceOf( QueryTimeoutException.class )
					.hasCause( timeoutException );
		} );
	}

	@Test
	@TestForIssue( jiraKey = "HSEARCH-1857" )
	public void reuseSearchSessionAfterEntityManagerIsClosed_noMatching() {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		SearchSession searchSession = Search.session( entityManager );
		// a SearchSession instance is created lazily,
		// so we need to use it to have an instance of it
		createSimpleQuery( searchSession );
		entityManager.close();

		SubTest.expectException( () -> {
			createSimpleQuery( searchSession );
		} )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessage( "HSEARCH800017: Underlying Hibernate ORM Session seems to be closed." );
	}

	@Test
	public void lazyCreateSearchSessionAfterEntityManagerIsClosed() {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		// Search session is not created, since we don't use it
		SearchSession searchSession = Search.session( entityManager );
		entityManager.close();

		SubTest.expectException( () -> {
			createSimpleQuery( searchSession );
		} )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessage( "HSEARCH800017: Underlying Hibernate ORM Session seems to be closed." );
	}

	private SearchQuery<IndexedEntity> createSimpleQuery(SearchSession searchSession) {
		return searchSession.search( IndexedEntity.class )
				.asEntity()
				.where( f -> f.matchAll() )
				.toQuery();
	}

	@Entity(name = IndexedEntity.NAME)
	@Indexed(index = IndexedEntity.NAME)
	public static class IndexedEntity {

		public static final String NAME = "indexed";

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
