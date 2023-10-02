/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.hibernateormapis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Test the compatibility layer between our APIs and JPA APIs
 * for the {@link EntityManager} class.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ToJpaEntityManagerIT {

	@RegisterExtension
	public static BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public static OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );
	private SessionFactory sessionFactory;

	@BeforeAll
	void setup() {
		backendMock.expectAnySchema( IndexedEntity.NAME );
		sessionFactory = ormSetupHelper.start().withAnnotatedTypes( IndexedEntity.class ).setup();
	}

	@Test
	void toJpaEntityManagerFactory() {
		SearchMapping searchMapping = Search.mapping( sessionFactory );
		assertThat( searchMapping.toEntityManagerFactory() ).isSameAs( sessionFactory );
	}

	@Test
	void toJpaEntityManager() {
		with( sessionFactory ).runNoTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			assertThat( searchSession.toEntityManager() ).isSameAs( entityManager );
		} );
	}

	@Test
	void toJpaEntityManager_withClosedEntityManager() {
		EntityManager entityManager = sessionFactory.createEntityManager();
		try {
			entityManager = sessionFactory.createEntityManager();
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
	@TestForIssue(jiraKey = "HSEARCH-1857")
	void reuseSearchSessionAfterEntityManagerIsClosed_noMatching() {
		EntityManager entityManager = sessionFactory.createEntityManager();
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
	void lazyCreateSearchSessionAfterEntityManagerIsClosed() {
		EntityManager entityManager = sessionFactory.createEntityManager();
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
