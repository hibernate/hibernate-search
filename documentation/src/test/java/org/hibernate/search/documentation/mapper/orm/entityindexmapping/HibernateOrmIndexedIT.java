/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.entityindexmapping;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingSynchronizationStrategyName;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class HibernateOrmIndexedIT {
	private static final String BACKEND_1 = "backend1";
	private static final String BACKEND_2 = "backend2";

	private static final Map<String, BackendConfiguration> BACKEND_CONFIGURATIONS;
	static {
		List<BackendConfiguration> backendConfigurations = BackendConfigurations.simple();
		if ( backendConfigurations.size() != 2 ) {
			throw new IllegalStateException(
					"This test assumes there are only two types of backends."
							+ " If this changed, please update this test to add/remove entity types mapped to each backend as necessary."
			);
		}
		Map<String, BackendConfiguration> map = new HashMap<>();
		map.put( BACKEND_1, backendConfigurations.get( 0 ) );
		map.put( BACKEND_2, backendConfigurations.get( 1 ) );
		BACKEND_CONFIGURATIONS = map;
	}

	@Rule
	public OrmSetupHelper setupHelper = OrmSetupHelper.withMultipleBackends( BACKEND_1, BACKEND_CONFIGURATIONS );

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void setup() {
		OrmSetupHelper.SetupContext setupContext = setupHelper.start();

		setupContext.withProperty(
				HibernateOrmMapperSettings.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY,
				AutomaticIndexingSynchronizationStrategyName.SEARCHABLE
		);

		entityManagerFactory = setupContext.setup( Book.class, User.class, Author.class );
		initData();
	}

	@Test
	public void search_separateQueries() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			List<Author> authorResult = searchSession.search( Author.class )
					.where( f -> f.matchAll() )
					.fetchHits( 20 );
			assertThat( authorResult ).hasSize( 1 );

			List<User> userResult = searchSession.search( User.class )
					.where( f -> f.matchAll() )
					.fetchHits( 20 );
			assertThat( userResult ).hasSize( 1 );
		} );
	}

	@Test
	public void search_singleQuery() {
		SubTest.expectException(
				() -> OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
					SearchSession searchSession = Search.session( entityManager );

					// tag::cross-backend-search[]
					// This will fail because Author and User are indexed in different backends
					List<Object> hits = searchSession.search(
									Arrays.asList( Author.class, User.class )
							)
							.where( f -> f.matchAll() )
							.fetchHits( 20 );
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
