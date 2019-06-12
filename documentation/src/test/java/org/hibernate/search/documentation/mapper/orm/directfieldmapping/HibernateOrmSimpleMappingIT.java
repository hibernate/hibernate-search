/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.directfieldmapping;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import javax.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendSetupStrategy;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmAutomaticIndexingSynchronizationStrategyName;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.orm.OrmUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class HibernateOrmSimpleMappingIT {
	private static final String BOOK1_TITLE = "I, Robot";
	private static final Integer BOOK1_PAGECOUNT = 224;

	private static final String BOOK2_TITLE = "The Caves of Steel";
	private static final Integer BOOK2_PAGECOUNT = 206;

	private static final String BOOK3_TITLE = "The Robots of Dawn";
	private static final Integer BOOK3_PAGECOUNT = 435;

	@Parameterized.Parameters(name = "{0}")
	public static Object[] backendSetups() {
		return BackendSetupStrategy.simple().toArray();
	}

	@Rule
	public OrmSetupHelper setupHelper = new OrmSetupHelper();

	private final BackendSetupStrategy backendSetupStrategy;

	private EntityManagerFactory entityManagerFactory;

	public HibernateOrmSimpleMappingIT(BackendSetupStrategy backendSetupStrategy) {
		this.backendSetupStrategy = backendSetupStrategy;
	}

	@Before
	public void setup() {
		entityManagerFactory = backendSetupStrategy.withSingleBackend( setupHelper )
				.withProperty(
						HibernateOrmMapperSettings.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY,
						HibernateOrmAutomaticIndexingSynchronizationStrategyName.SEARCHABLE
				)
				.setup( Book.class );
		initData();
	}

	@Test
	public void predicate_simple() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			// tag::predicate-simple-objects[]
			SearchSession searchSession = Search.getSearchSession( entityManager );

			SearchScope<Book> scope = searchSession.scope( Book.class );

			List<Book> result = scope.search()
					.predicate( scope.predicate().match().onField( "title" )
							.matching( "robot" )
							.toPredicate() )
					.fetchHits();
			// end::predicate-simple-objects[]

			assertThat( result )
					.extracting( "title" )
					.containsExactlyInAnyOrder( BOOK1_TITLE, BOOK3_TITLE );
		} );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			// tag::predicate-simple-lambdas[]
			SearchSession searchSession = Search.getSearchSession( entityManager );

			List<Book> result = searchSession.search( Book.class ) // <1>
					.predicate( f -> f.match().onField( "title" ) // <2>
							.matching( "robot" ) )
					.fetchHits(); // <3>
			// end::predicate-simple-lambdas[]

			assertThat( result )
					.extracting( "title" )
					.containsExactlyInAnyOrder( BOOK1_TITLE, BOOK3_TITLE );
		} );
	}

	@Test
	public void sort_simple() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			// tag::sort-simple-objects[]
			SearchSession searchSession = Search.getSearchSession( entityManager );

			SearchScope<Book> scope = searchSession.scope( Book.class );

			List<Book> result = scope.search()
					.predicate( scope.predicate().matchAll().toPredicate() )
					.sort(
							scope.sort()
							.byField( "pageCount" ).desc()
							.then().byField( "title_sort" )
							.toSort()
					)
					.fetchHits();
			// end::sort-simple-objects[]

			assertThat( result )
					.extracting( "title" )
					.containsExactly( BOOK3_TITLE, BOOK1_TITLE, BOOK2_TITLE );
		} );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			// tag::sort-simple-lambdas[]
			SearchSession searchSession = Search.getSearchSession( entityManager );

			List<Book> result = searchSession.search( Book.class ) // <1>
					.predicate( f -> f.matchAll() )
					.sort( f -> f.byField( "pageCount" ).desc() // <2>
							.then().byField( "title_sort" )
					)
					.fetchHits(); // <3>
			// end::sort-simple-lambdas[]

			assertThat( result )
					.extracting( "title" )
					.containsExactly( BOOK3_TITLE, BOOK1_TITLE, BOOK2_TITLE );
		} );
	}

	@Test
	public void projection_simple() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			// tag::projection-simple-objects[]
			SearchSession searchSession = Search.getSearchSession( entityManager );

			SearchScope<Book> scope = searchSession.scope( Book.class );

			List<String> result = scope.search()
					.asProjection( scope.projection().field( "title", String.class ).toProjection() )
					.predicate( scope.predicate().matchAll().toPredicate() )
					.fetchHits();
			// end::projection-simple-objects[]

			assertThat( result )
					.containsExactlyInAnyOrder( BOOK1_TITLE, BOOK2_TITLE, BOOK3_TITLE );
		} );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			// tag::projection-simple-lambdas[]
			SearchSession searchSession = Search.getSearchSession( entityManager );

			List<String> result = searchSession.search( Book.class ) // <1>
					.asProjection( f -> f.field( "title", String.class ) ) // <2>
					.predicate( f -> f.matchAll() )
					.fetchHits(); // <3>
			// end::projection-simple-lambdas[]

			assertThat( result )
					.containsExactlyInAnyOrder( BOOK1_TITLE, BOOK2_TITLE, BOOK3_TITLE );
		} );
	}

	@Test
	public void projection_advanced() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			// tag::projection-advanced[]
			SearchSession searchSession = Search.getSearchSession( entityManager );

			List<MyEntityAndScoreBean<Book>> result = searchSession.search( Book.class )
					.asProjection( f -> f.composite(
							MyEntityAndScoreBean::new,
							f.entity(),
							f.score()
					) )
					.predicate( f -> f.matchAll() )
					.fetchHits();
			// end::projection-advanced[]

			assertThat( result )
					.extracting( "entity" )
					.extracting( "title", String.class )
					.containsExactlyInAnyOrder( BOOK1_TITLE, BOOK2_TITLE, BOOK3_TITLE );
		} );
	}

	private void initData() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			Book book1 = new Book();
			book1.setTitle( BOOK1_TITLE );
			book1.setPageCount( BOOK1_PAGECOUNT );
			Book book2 = new Book();
			book2.setTitle( BOOK2_TITLE );
			book2.setPageCount( BOOK2_PAGECOUNT );
			Book book3 = new Book();
			book3.setTitle( BOOK3_TITLE );
			book3.setPageCount( BOOK3_PAGECOUNT );

			entityManager.persist( book1 );
			entityManager.persist( book2 );
			entityManager.persist( book3 );
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
