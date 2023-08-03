/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.spring.repackaged.application;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;

import acme.org.hibernate.search.integrationtest.spring.repackaged.model.MyEntity;
import acme.org.hibernate.search.integrationtest.spring.repackaged.model.MyProjection;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EntityScan({ "acme.org.hibernate.search.integrationtest.spring.repackaged.model" })
public class Config {

	@Bean
	public TestHibernateSearch testHibernateSearch(EntityManagerFactory entityManagerFactory) {
		return new TestHibernateSearch( entityManagerFactory );
	}

	public static class TestHibernateSearch {
		final EntityManagerFactory entityManagerFactory;

		public TestHibernateSearch(EntityManagerFactory entityManagerFactory) {
			this.entityManagerFactory = entityManagerFactory;
		}

		@PostConstruct
		public void check() {
			EntityManager entityManager = null;
			try {
				entityManager = entityManagerFactory.createEntityManager();
				EntityTransaction transaction = entityManager.getTransaction();
				transaction.begin();
				MyEntity entity = new MyEntity();
				entity.id = 1L;
				entity.name = "name";
				entityManager.persist( entity );
				transaction.commit();

				transaction.begin();
				SearchSession session = Search.session( entityManager );
				List<MyProjection> myProjections = session.search( MyEntity.class )
						.select( MyProjection.class )
						.where( f -> f.match().field( "name" ).matching( "name" ) )
						.fetchAllHits();
				if ( myProjections.isEmpty() ) {
					throw new IllegalStateException( "Incorrect count of projections." );
				}
				transaction.commit();
				System.out.println( "Hibernate Search read the nested JAR." );
			}
			finally {
				if ( entityManager != null ) {
					entityManager.close();
				}
			}
		}
	}
}
