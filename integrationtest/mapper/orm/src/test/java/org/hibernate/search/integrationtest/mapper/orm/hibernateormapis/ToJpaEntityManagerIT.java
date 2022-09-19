/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.hibernateormapis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;

import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.ReusableOrmSetupHolder;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

/**
 * Test the compatibility layer between our APIs and JPA APIs
 * for the {@link EntityManager} class.
 */
public class ToJpaEntityManagerIT {

	@ClassRule
	public static BackendMock backendMock = new BackendMock();

	@ClassRule
	public static ReusableOrmSetupHolder setupHolder = ReusableOrmSetupHolder.withBackendMock( backendMock );

	@Rule
	public MethodRule setupHolderMethodRule = setupHolder.methodRule();

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext) {
		backendMock.expectAnySchema( IndexedEntity.NAME );
		setupContext.withAnnotatedTypes( IndexedEntity.class );
	}

	@Test
	public void toJpaEntityManagerFactory() {
		SearchMapping searchMapping = Search.mapping( setupHolder.entityManagerFactory() );
		assertThat( searchMapping.toEntityManagerFactory() ).isSameAs( setupHolder.entityManagerFactory() );
	}

	@Test
	public void toJpaEntityManager() {
		setupHolder.runNoTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			assertThat( searchSession.toEntityManager() ).isSameAs( entityManager );
		} );
	}

	@Test
	public void toJpaEntityManager_withClosedEntityManager() {
		EntityManager entityManager = setupHolder.entityManagerFactory().createEntityManager();
		try {
			entityManager = setupHolder.entityManagerFactory().createEntityManager();
		}
		finally {
			if ( entityManager != null ) {
				entityManager.close();
			}
		}

		EntityManager closedEntityManager = entityManager;
		assertThatThrownBy( () -> Search.session( closedEntityManager ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unable to access Hibernate ORM session" );
	}

	@Test
	@TestForIssue( jiraKey = "HSEARCH-1857" )
	public void reuseSearchSessionAfterEntityManagerIsClosed_noMatching() {
		EntityManager entityManager = setupHolder.entityManagerFactory().createEntityManager();
		SearchSession searchSession = Search.session( entityManager );
		// a SearchSession instance is created lazily,
		// so we need to use it to have an instance of it
		createSimpleQuery( searchSession );
		entityManager.close();

		assertThatThrownBy( () -> createSimpleQuery( searchSession ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unable to access Hibernate ORM session", "is closed" );
	}

	@Test
	public void lazyCreateSearchSessionAfterEntityManagerIsClosed() {
		EntityManager entityManager = setupHolder.entityManagerFactory().createEntityManager();
		// Search session is not created, since we don't use it
		SearchSession searchSession = Search.session( entityManager );
		entityManager.close();

		assertThatThrownBy( () -> createSimpleQuery( searchSession ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unable to access Hibernate ORM session", "is closed" );
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

		@FullTextField
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
