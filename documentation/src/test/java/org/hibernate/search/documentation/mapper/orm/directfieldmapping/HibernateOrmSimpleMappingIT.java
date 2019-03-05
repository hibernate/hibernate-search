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

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.lucene.cfg.LuceneBackendSettings;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.search.query.FullTextQuery;
import org.hibernate.search.mapper.orm.search.FullTextSearchTarget;
import org.hibernate.search.mapper.orm.session.FullTextSession;
import org.hibernate.search.util.impl.integrationtest.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.orm.OrmUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class HibernateOrmSimpleMappingIT {
	private static final String BACKEND_NAME = "backendName";

	private static final String BOOK1_TITLE = "I, Robot";
	private static final Integer BOOK1_PAGECOUNT = 224;

	private static final String BOOK2_TITLE = "The Caves of Steel";
	private static final Integer BOOK2_PAGECOUNT = 206;

	private static final String BOOK3_TITLE = "The Robots of Dawn";
	private static final Integer BOOK3_PAGECOUNT = 435;

	@Parameterized.Parameters(name = "{0}")
	public static Object[] backendSetups() {
		return new Object[] {
				new BackendSetupStrategy() {
					@Override
					public String toString() {
						return "lucene";
					}
					@Override
					public OrmSetupHelper.SetupContext startSetup(OrmSetupHelper setupHelper) {
						return setupHelper.withBackend( "lucene", BACKEND_NAME )
								.withBackendProperty(
										BACKEND_NAME,
										LuceneBackendSettings.ANALYSIS_CONFIGURER,
										new LuceneSimpleMappingAnalysisConfigurer()
								);
					}
				},
				new BackendSetupStrategy() {
					@Override
					public String toString() {
						return "elasticsearch";
					}
					@Override
					public OrmSetupHelper.SetupContext startSetup(OrmSetupHelper setupHelper) {
						return setupHelper.withBackend( "elasticsearch", "backendName" )
								.withBackendProperty(
										BACKEND_NAME,
										ElasticsearchBackendSettings.ANALYSIS_CONFIGURER,
										new ElasticsearchSimpleMappingAnalysisConfigurer()
								);
					}
				}
		};
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
		entityManagerFactory = backendSetupStrategy.startSetup( setupHelper ).setup( Book.class );
		initData();
	}

	@Test
	public void sort_simple() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			// tag::sort-simple-objects[]
			FullTextSession fullTextSession = Search.getFullTextSession( entityManager );

			FullTextSearchTarget<Book> searchTarget = fullTextSession.target( Book.class );

			FullTextQuery<Book> query = searchTarget.search() // <1>
					.asEntity()
					.predicate( searchTarget.predicate().matchAll().toPredicate() )
					.sort(
							searchTarget.sort() // <2>
							.byField( "pageCount" ).desc()
							.then().byField( "title_sort" )
							.toSort()
					)
					.build();

			List<Book> result = query.getResultList(); // <3>
			// end::sort-simple-objects[]

			assertThat( result )
					.extracting( "title" )
					.containsExactly( BOOK3_TITLE, BOOK1_TITLE, BOOK2_TITLE );
		} );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			// tag::sort-simple-lambda[]
			FullTextSession fullTextSession = Search.getFullTextSession( entityManager );

			FullTextSearchTarget<Book> searchTarget = fullTextSession.target( Book.class );

			FullTextQuery<Book> query = searchTarget.search()
					.asEntity()
					.predicate( f -> f.matchAll() )
					.sort( f -> f.byField( "pageCount" ).desc()
							.then().byField( "title_sort" )
					)
					.build();

			List<Book> result = query.getResultList(); // <3>
			// end::sort-simple-lambda[]

			assertThat( result )
					.extracting( "title" )
					.containsExactly( BOOK3_TITLE, BOOK1_TITLE, BOOK2_TITLE );
		} );
	}

	@Test
	public void projection_simple() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			// tag::projection-simple-objects[]
			FullTextSession fullTextSession = Search.getFullTextSession( entityManager );

			FullTextSearchTarget<Book> searchTarget = fullTextSession.target( Book.class );

			FullTextQuery<String> query = searchTarget.search() // <1>
					.asProjection( searchTarget.projection().field( "title", String.class ).toProjection() ) // <2>
					.predicate( searchTarget.predicate().matchAll().toPredicate() )
					.build();

			List<String> result = query.getResultList(); // <3>
			// end::projection-simple-objects[]

			assertThat( result )
					.containsExactlyInAnyOrder( BOOK1_TITLE, BOOK2_TITLE, BOOK3_TITLE );
		} );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			// tag::projection-simple-lambda[]
			FullTextSession fullTextSession = Search.getFullTextSession( entityManager );

			FullTextQuery<String> query = fullTextSession.search( Book.class )
					.asProjection( f -> f.field( "title", String.class ) )
					.predicate( f -> f.matchAll() )
					.build();

			List<String> result = query.getResultList();
			// end::projection-simple-lambda[]

			assertThat( result )
					.containsExactlyInAnyOrder( BOOK1_TITLE, BOOK2_TITLE, BOOK3_TITLE );
		} );
	}

	@Test
	public void projection_advanced() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			// tag::projection-advanced[]
			FullTextSession fullTextSession = Search.getFullTextSession( entityManager );

			FullTextQuery<MyEntityAndScoreBean<Book>> query = fullTextSession.search( Book.class )
					.asProjection( f ->
							f.composite(
									MyEntityAndScoreBean::new,
									f.object(),
									f.score()
							)
					)
					.predicate( f -> f.matchAll() )
					.build();

			List<MyEntityAndScoreBean<Book>> result = query.getResultList();
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

	private interface BackendSetupStrategy {
		OrmSetupHelper.SetupContext startSetup(OrmSetupHelper setupHelper);
	}

}
