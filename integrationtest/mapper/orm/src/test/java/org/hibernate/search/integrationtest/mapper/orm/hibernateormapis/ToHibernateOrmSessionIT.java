/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.hibernateormapis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.withinSession;

import java.util.Arrays;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test the compatibility layer between our APIs and Hibernate ORM APIs
 * for the {@link Session} class.
 */
public class ToHibernateOrmSessionIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private SessionFactory sessionFactory;

	@Before
	public void setup() {
		backendMock.expectAnySchema( IndexedEntity.NAME );
		sessionFactory = ormSetupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void toHibernateOrmSessionFactory() {
		SearchMapping searchMapping = Search.mapping( sessionFactory );
		assertThat( searchMapping.toOrmSessionFactory() ).isSameAs( sessionFactory );
	}

	@Test
	public void toHibernateOrmSession() {
		withinSession( sessionFactory, session -> {
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
		assertThatThrownBy( () -> {
			Search.session( closedSession );
		} )
				.isInstanceOf( SearchException.class )
				.hasMessage( "HSEARCH800016: Error trying to access Hibernate ORM session." );
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

		assertThatThrownBy( () -> createSimpleQuery( searchSession ) )
				.isInstanceOf( SearchException.class )
				.hasMessage( "HSEARCH800017: Underlying Hibernate ORM Session seems to be closed." );
	}

	@Test
	public void lazyCreateSearchSessionAfterOrmSessionIsClosed() {
		Session session = sessionFactory.openSession();
		// Search session is not created, since we don't use it
		SearchSession searchSession = Search.session( session );
		session.close();

		assertThatThrownBy( () -> createSimpleQuery( searchSession ) )
				.isInstanceOf( SearchException.class )
				.hasMessage( "HSEARCH800017: Underlying Hibernate ORM Session seems to be closed." );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1857")
	public void reuseSearchQueryAfterOrmSessionIsClosed() {
		Session session = sessionFactory.openSession();
		SearchSession searchSession = Search.session( session );
		SearchQuery<IndexedEntity> query = createSimpleQuery( searchSession );
		session.close();

		backendMock.expectSearchObjects(
				Arrays.asList( IndexedEntity.NAME ),
				b -> { },
				// The call will fail, this doesn't matter
				StubSearchWorkBehavior.empty()
		);

		assertThatThrownBy( () -> query.fetchAllHits() )
				.isInstanceOf( SearchException.class )
				.hasMessage( "HSEARCH800017: Underlying Hibernate ORM Session seems to be closed." );
	}

	private SearchQuery<IndexedEntity> createSimpleQuery(SearchSession searchSession) {
		return searchSession.search( IndexedEntity.class )
				.selectEntity()
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

		@Override
		public String toString() {
			return "IndexedEntity[id=" + id + "]";
		}

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
