/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.entityindexmapping;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import javax.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendSetupStrategy;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmAutomaticIndexingSynchronizationStrategyName;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.orm.OrmUtils;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class HibernateOrmIndexedIT {
	private static final String BACKEND_1 = "backend1";
	private static final String BACKEND_2 = "backend2";

	@Rule
	public OrmSetupHelper setupHelper = new OrmSetupHelper();

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void setup() {
		OrmSetupHelper.SetupContext setupContext = setupHelper.startSetup();

		List<BackendSetupStrategy> backendSetupStrategies = BackendSetupStrategy.simple();
		if ( backendSetupStrategies.size() != 2 ) {
			throw new IllegalStateException(
					"This test assumes there are only two types of backends."
					+ " If this changed, please update this test to add/remove entity types mapped to each backend as necessary."
			);
		}
		backendSetupStrategies.get( 0 ).withBackend( setupContext, BACKEND_1 );
		backendSetupStrategies.get( 1 ).withBackend( setupContext, BACKEND_2 );
		setupContext.withDefaultBackend( BACKEND_1 );

		setupContext.withProperty(
				HibernateOrmMapperSettings.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY,
				HibernateOrmAutomaticIndexingSynchronizationStrategyName.SEARCHABLE
		);

		entityManagerFactory = setupContext.setup( Book.class, User.class, Author.class );
		initData();
	}

	@Test
	public void search_separateQueries() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.getSearchSession( entityManager );

			List<Author> authorResult = searchSession.search( Author.class )
					.predicate( f -> f.matchAll() )
					.fetchHits();
			assertThat( authorResult ).hasSize( 1 );

			List<User> userResult = searchSession.search( User.class )
					.predicate( f -> f.matchAll() )
					.fetchHits();
			assertThat( userResult ).hasSize( 1 );
		} );
	}

	@Test
	public void search_singleQuery() {
		SubTest.expectException(
				() -> OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
					SearchSession searchSession = Search.getSearchSession( entityManager );

					// tag::cross-backend-search[]
					// This will fail
					SearchQuery<Object> query = searchSession.search(
									Arrays.asList( Author.class, User.class )
							)
							.predicate( f -> f.matchAll() )
							.toQuery();
					// end::cross-backend-search[]
				} )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "A multi-index scope cannot include both " )
				.hasMessageContaining( " and another type of index" );
	}

	private void initData() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			Book book1 = new Book();
			book1.setTitle( "Some title" );
			Author author1 = new Author();
			author1.setFirstName( "Jane" );
			author1.setLastName( "Doe" );
			User user1 = new User();
			user1.setFirstName( "John" );
			user1.setLastName( "Smith" );

			entityManager.persist( book1 );
			entityManager.persist( author1 );
			entityManager.persist( user1 );
		} );
	}

	// Note: ideally we should use "static" here, but it looks better without it in the documentation.
	// tag::projection-advanced-bean[]
	public class MyEntityAndScoreBean<T> {
		public final T entity;
		public final float score;
		public MyEntityAndScoreBean(T entity, float score) {
			this.entity = entity;
			this.score = score;
		}
	}
	// end::projection-advanced-bean[]

}
