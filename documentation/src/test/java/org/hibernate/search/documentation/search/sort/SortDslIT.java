/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.search.sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.engine.search.common.SortMode;
import org.hibernate.search.engine.search.sort.dsl.SortOrder;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.hibernate.search.mapper.orm.session.SearchSession;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class SortDslIT {

	private static final int ASIMOV_ID = 1;
	private static final int MARTINEZ_ID = 2;

	private static final int BOOK1_ID = 1;
	private static final int BOOK2_ID = 2;
	private static final int BOOK3_ID = 3;
	private static final int BOOK4_ID = 4;

	@Rule
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start().setup( Book.class, Author.class, EmbeddableGeoPoint.class );
		initData();
	}

	@Test
	public void entryPoint() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			// tag::entryPoint-lambdas[]
			SearchSession searchSession = /* ... */ // <1>
					// end::entryPoint-lambdas[]
					Search.session( entityManager );
			// tag::entryPoint-lambdas[]

			List<Book> result = searchSession.search( Book.class ) // <2>
					.where( f -> f.matchAll() )
					.sort( f -> f.field( "pageCount" ).desc() // <3>
							.then().field( "title_sort" ) )
					.fetchHits( 20 ); // <4>
			// end::entryPoint-lambdas[]
			assertThat( result )
					.extracting( Book::getId )
					.containsExactly( BOOK4_ID, BOOK1_ID, BOOK2_ID, BOOK3_ID );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			// tag::entryPoint-objects[]
			SearchSession searchSession = /* ... */
					// end::entryPoint-objects[]
					Search.session( entityManager );
			// tag::entryPoint-objects[]

			SearchScope<Book> scope = searchSession.scope( Book.class );

			List<Book> result = searchSession.search( scope )
					.where( scope.predicate().matchAll().toPredicate() )
					.sort( scope.sort()
							.field( "pageCount" ).desc()
							.then().field( "title_sort" )
							.toSort() )
					.fetchHits( 20 );
			// end::entryPoint-objects[]
			assertThat( result )
					.extracting( Book::getId )
					.containsExactly( BOOK4_ID, BOOK1_ID, BOOK2_ID, BOOK3_ID );
		} );
	}

	@Test
	public void score() {
		withinSearchSession( searchSession -> {
			// tag::score[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.match().field( "title" )
							.matching( "robot dawn" ) )
					.sort( f -> f.score() )
					.fetchHits( 20 );
			// end::score[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactly( BOOK3_ID, BOOK1_ID );
		} );
	}

	@Test
	public void indexOrder() {
		withinSearchSession( searchSession -> {
			// tag::indexOrder[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.sort( f -> f.indexOrder() )
					.fetchHits( 20 );
			// end::indexOrder[]
			assertThat( hits )
					.extracting( Book::getId )
					// Not checking the order here, it's implementation-dependent
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID, BOOK3_ID, BOOK4_ID );
		} );
	}

	@Test
	public void stabilization() {
		withinSearchSession( searchSession -> {
			// tag::stabilization[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.sort( f -> f.field( "genre_sort" ).then().field( "id_sort" ) )
					.fetchHits( 20 );
			// end::stabilization[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactly( BOOK4_ID, BOOK1_ID, BOOK2_ID, BOOK3_ID );
		} );
	}

	@Test
	public void field() {
		withinSearchSession( searchSession -> {
			// tag::field[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.sort( f -> f.field( "title_sort" ) )
					.fetchHits( 20 );
			// end::field[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactly( BOOK1_ID, BOOK4_ID, BOOK2_ID, BOOK3_ID );
		} );
	}

	@Test
	public void order() {
		withinSearchSession( searchSession -> {
			// tag::order-asc[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.sort( f -> f.field( "title_sort" ).asc() )
					.fetchHits( 20 );
			// end::order-asc[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactly( BOOK1_ID, BOOK4_ID, BOOK2_ID, BOOK3_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::order-desc[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.sort( f -> f.field( "title_sort" ).desc() )
					.fetchHits( 20 );
			// end::order-desc[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactly( BOOK3_ID, BOOK2_ID, BOOK4_ID, BOOK1_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::order-order[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.sort( f -> f.field( "title_sort" ).order( SortOrder.DESC ) )
					.fetchHits( 20 );
			// end::order-order[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactly( BOOK3_ID, BOOK2_ID, BOOK4_ID, BOOK1_ID );
		} );
	}

	@Test
	public void missing() {
		withinSearchSession( searchSession -> {
			// tag::missing-first[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.sort( f -> f.field( "pageCount" ).missing().first() )
					.fetchHits( 20 );
			// end::missing-first[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactly( BOOK3_ID, BOOK2_ID, BOOK1_ID, BOOK4_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::missing-last[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.sort( f -> f.field( "pageCount" ).missing().last() )
					.fetchHits( 20 );
			// end::missing-last[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactly( BOOK2_ID, BOOK1_ID, BOOK4_ID, BOOK3_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::missing-use[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.sort( f -> f.field( "pageCount" ).missing().use( 300 ) )
					.fetchHits( 20 );
			// end::missing-use[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactly( BOOK2_ID, BOOK1_ID, BOOK3_ID, BOOK4_ID );
		} );
	}

	@Test
	public void mode() {
		withinSearchSession( searchSession -> {
			// tag::mode-avg[]
			List<Author> hits = searchSession.search( Author.class )
					.where( f -> f.matchAll() )
					.sort( f -> f.field( "books.pageCount" ).mode( SortMode.AVG ) )
					.fetchHits( 20 );
			// end::mode-avg[]
			assertThat( hits )
					.extracting( Author::getId )
					.containsExactly( ASIMOV_ID, MARTINEZ_ID );
		} );
	}

	@Test
	public void filter() {
		withinSearchSession( searchSession -> {
			// tag::filter[]
			List<Author> hits = searchSession.search( Author.class )
					.where( f -> f.matchAll() )
					.sort( f -> f.field( "books.pageCount" )
							.mode( SortMode.AVG )
							.filter( pf -> pf.match().field( "books.genre" )
									.matching( Genre.CRIME_FICTION ) ) )
					.fetchHits( 20 );
			// end::filter[]
			assertThat( hits )
					.extracting( Author::getId )
					.containsExactly( MARTINEZ_ID, ASIMOV_ID );
		} );
	}

	@Test
	public void composite() {
		withinSearchSession( searchSession -> {
			// tag::composite[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.sort( f -> f.composite() // <1>
							.add( f.field( "genre_sort" ) ) // <2>
							.add( f.field( "title_sort" ) ) ) // <3>
					.fetchHits( 20 ); // <4>
			// end::composite[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactly( BOOK4_ID, BOOK1_ID, BOOK2_ID, BOOK3_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::composite_dynamicParameters[]
			MySearchParameters searchParameters = getSearchParameters(); // <1>
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.sort( f -> f.composite( b -> { // <2>
						for ( MySort mySort : searchParameters.getSorts() ) { // <3>
							switch ( mySort.getType() ) {
								case GENRE:
									b.add( f.field( "genre_sort" ).order( mySort.getOrder() ) );
									break;
								case TITLE:
									b.add( f.field( "title_sort" ).order( mySort.getOrder() ) );
									break;
								case PAGE_COUNT:
									b.add( f.field( "pageCount" ).order( mySort.getOrder() ) );
									break;
							}
						}
					} ) )
					.fetchHits( 20 ); // <4>
			// end::composite_dynamicParameters[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactly( BOOK4_ID, BOOK3_ID, BOOK2_ID, BOOK1_ID );
		} );
	}

	@Test
	public void then() {
		withinSearchSession( searchSession -> {
			// tag::then[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.sort( f -> f.field( "genre_sort" )
							.then().field( "title_sort" ) )
					.fetchHits( 20 );
			// end::then[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactly( BOOK4_ID, BOOK1_ID, BOOK2_ID, BOOK3_ID );
		} );
	}

	@Test
	public void distance() {
		withinSearchSession( searchSession -> {
			// tag::distance[]
			GeoPoint center = GeoPoint.of( 47.506060, 2.473916 );
			List<Author> hits = searchSession.search( Author.class )
					.where( f -> f.matchAll() )
					.sort( f -> f.distance( "placeOfBirth", center ) )
					.fetchHits( 20 );
			// end::distance[]
			assertThat( hits )
					.extracting( Author::getId )
					.containsExactly( ASIMOV_ID, MARTINEZ_ID );
		} );
	}

	private MySearchParameters getSearchParameters() {
		return () -> Arrays.asList(
			new MySort( MySortType.GENRE, SortOrder.ASC ),
			new MySort( MySortType.TITLE, SortOrder.DESC ),
			new MySort( MySortType.PAGE_COUNT, SortOrder.DESC )
		);
	}

	private void withinSearchSession(Consumer<SearchSession> action) {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			action.accept( searchSession );
		} );
	}

	private void initData() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			Author isaacAsimov = new Author();
			isaacAsimov.setId( ASIMOV_ID );
			isaacAsimov.setFirstName( "Isaac" );
			isaacAsimov.setLastName( "Asimov" );
			isaacAsimov.setPlaceOfBirth( EmbeddableGeoPoint.of( 53.976177, 32.158627 ) );

			Author aLeeMartinez = new Author();
			aLeeMartinez.setId( MARTINEZ_ID );
			aLeeMartinez.setFirstName( "A. Lee" );
			aLeeMartinez.setLastName( "Martinez" );
			aLeeMartinez.setPlaceOfBirth( EmbeddableGeoPoint.of( 31.814315, -106.475524 ) );

			Book book1 = new Book();
			book1.setId( BOOK1_ID );
			book1.setTitle( "I, Robot" );
			book1.setPageCount( 250 );
			book1.setGenre( Genre.SCIENCE_FICTION );
			book1.getAuthors().add( isaacAsimov );
			isaacAsimov.getBooks().add( book1 );

			Book book2 = new Book();
			book2.setId( BOOK2_ID );
			book2.setTitle( "The Caves of Steel" );
			book2.setPageCount( 206 );
			book2.setGenre( Genre.SCIENCE_FICTION );
			book2.getAuthors().add( isaacAsimov );
			isaacAsimov.getBooks().add( book2 );

			Book book3 = new Book();
			book3.setId( BOOK3_ID );
			book3.setTitle( "The Robots of Dawn" );
			book3.setPageCount( null ); // Missing page count: this is on purpose
			book3.setGenre( Genre.SCIENCE_FICTION );
			book3.getAuthors().add( isaacAsimov );
			isaacAsimov.getBooks().add( book3 );

			Book book4 = new Book();
			book4.setId( BOOK4_ID );
			book4.setTitle( "The Automatic Detective" );
			book4.setPageCount( 435 );
			book4.setGenre( Genre.CRIME_FICTION );
			book4.getAuthors().add( aLeeMartinez );
			aLeeMartinez.getBooks().add( book4 );

			entityManager.persist( isaacAsimov );
			entityManager.persist( aLeeMartinez );

			entityManager.persist( book1 );
			entityManager.persist( book2 );
			entityManager.persist( book3 );
			entityManager.persist( book4 );
		} );
	}

	private interface MySearchParameters {
		List<MySort> getSorts();
	}

	private static final class MySort {
		private final MySortType type;
		private final SortOrder order;

		public MySort(MySortType type, SortOrder order) {
			this.type = type;
			this.order = order;
		}

		public MySortType getType() {
			return type;
		}

		public SortOrder getOrder() {
			return order;
		}
	}

	private enum MySortType {
		GENRE,
		TITLE,
		PAGE_COUNT
	}
}
