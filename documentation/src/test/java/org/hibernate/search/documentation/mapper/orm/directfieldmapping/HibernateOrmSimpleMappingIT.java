/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.directfieldmapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.List;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Test that the simple mapping defined in Book works as expected.
 */
@RunWith(Parameterized.class)
public class HibernateOrmSimpleMappingIT {
	private static final String BOOK1_TITLE = "I, Robot";
	private static final Integer BOOK1_PAGECOUNT = 224;

	private static final String BOOK2_TITLE = "The Caves of Steel";
	private static final Integer BOOK2_PAGECOUNT = 206;

	private static final String BOOK3_TITLE = "The Robots of Dawn";
	private static final Integer BOOK3_PAGECOUNT = 435;

	@Parameterized.Parameters(name = "{0}")
	public static List<?> params() {
		return DocumentationSetupHelper.testParamsForBothAnnotationsAndProgrammatic(
				BackendConfigurations.simple(),
				mapping -> {
				// @formatter:off
					//tag::programmatic[]
					TypeMappingStep bookMapping = mapping.type( Book.class );
					bookMapping.indexed();
					bookMapping.property( "title" )
							.fullTextField()
									.analyzer( "english" ).projectable( Projectable.YES )
							.keywordField( "title_sort" )
									.normalizer( "english" ).sortable( Sortable.YES );
					bookMapping.property( "pageCount" )
							.genericField().projectable( Projectable.YES ).sortable( Sortable.YES );
					//end::programmatic[]
					// @formatter:on
				} );
	}

	@Parameterized.Parameter
	@Rule
	public DocumentationSetupHelper setupHelper;

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start().setup( Book.class );
		initData();
	}

	@Test
	public void sort() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			List<Book> result = searchSession.search( Book.class ) // <1>
					.where( f -> f.matchAll() )
					.sort( f -> f.field( "pageCount" ).desc() // <2>
							.then().field( "title_sort" )
					)
					.fetchHits( 20 ); // <3>

			assertThat( result )
					.extracting( "title" )
					.containsExactly( BOOK3_TITLE, BOOK1_TITLE, BOOK2_TITLE );
		} );
	}

	@Test
	public void projection_simple() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			List<String> result = searchSession.search( Book.class ) // <1>
					.select( f -> f.field( "title", String.class ) ) // <2>
					.where( f -> f.matchAll() )
					.fetchHits( 20 ); // <3>

			assertThat( result )
					.containsExactlyInAnyOrder( BOOK1_TITLE, BOOK2_TITLE, BOOK3_TITLE );
		} );
	}

	private void initData() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
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

}
