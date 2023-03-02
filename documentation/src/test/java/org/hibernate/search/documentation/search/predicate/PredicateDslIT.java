/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.search.predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import javax.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.engine.search.common.BooleanOperator;
import org.hibernate.search.engine.search.predicate.dsl.RegexpQueryFlag;
import org.hibernate.search.engine.search.predicate.dsl.SimpleQueryFlag;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoBoundingBox;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.engine.spatial.GeoPolygon;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.common.data.RangeBoundInclusion;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class PredicateDslIT {

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
					.where( f -> f.match().field( "title" ) // <3>
							.matching( "robot" ) )
					.fetchHits( 20 ); // <4>
			// end::entryPoint-lambdas[]
			assertThat( result )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK3_ID );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			// tag::entryPoint-objects[]
			SearchSession searchSession = /* ... */
					// end::entryPoint-objects[]
					Search.session( entityManager );
			// tag::entryPoint-objects[]

			SearchScope<Book> scope = searchSession.scope( Book.class );

			List<Book> result = searchSession.search( scope )
					.where( scope.predicate().match().field( "title" )
							.matching( "robot" )
							.toPredicate() )
					.fetchHits( 20 );
			// end::entryPoint-objects[]
			assertThat( result )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK3_ID );
		} );
	}

	@Test
	public void matchAll() {
		withinSearchSession( searchSession -> {
			// tag::matchAll[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.fetchHits( 20 );
			// end::matchAll[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID, BOOK3_ID, BOOK4_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::matchAll-except[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.matchAll()
							.except( f.match().field( "title" )
									.matching( "robot" ) )
					)
					.fetchHits( 20 );
			// end::matchAll-except[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK2_ID, BOOK4_ID );
		} );
	}

	@Test
	public void matchNone() {
		withinSearchSession( searchSession -> {
			// tag::matchNone[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.matchNone() )
					.fetchHits( 20 );
			// end::matchNone[]
			assertThat( hits )
					.extracting( Book::getId )
					.isEmpty();
		} );
	}

	@Test
	public void id() {
		// DO NOT USE THE BOOKX_ID CONSTANTS INSIDE TAGS BELOW:
		// we don't want the constants to appear in the documentation.

		withinSearchSession( searchSession -> {
			// tag::id[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.id().matching( 1 ) )
					.fetchHits( 20 );
			// end::id[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::id-matchingAny[]
			List<Integer> ids = new ArrayList<>();
			ids.add( 1 );
			ids.add( 2 );
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.id().matchingAny( ids ) )
					.fetchHits( 20 );
			// end::id-matchingAny[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID );
		} );
	}

	@Test
	public void and() {
		withinSearchSession( searchSession -> {
			// tag::and[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.and(
									f.match().field( "title" )
											.matching( "robot" ), // <1>
									f.match().field( "description" )
											.matching( "crime" ) // <2>
							)
					)
					.fetchHits( 20 ); // <3>
			// end::and[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK3_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::and-dynamicParameters-root[]
			MySearchParameters searchParameters = getSearchParameters(); // <1>
			List<Book> hits = searchSession.search( Book.class )
					.where( (f, root) -> { // <2>
						root.add( f.matchAll() ); // <3>
						if ( searchParameters.getGenreFilter() != null ) { // <4>
							root.add( f.match().field( "genre" )
									.matching( searchParameters.getGenreFilter() ) );
						}
						if ( searchParameters.getFullTextFilter() != null ) {
							root.add( f.match().fields( "title", "description" )
									.matching( searchParameters.getFullTextFilter() ) );
						}
						if ( searchParameters.getPageCountMaxFilter() != null ) {
							root.add( f.range().field( "pageCount" )
									.atMost( searchParameters.getPageCountMaxFilter() ) );
						}
					} )
					.fetchHits( 20 );
			// end::and-dynamicParameters-root[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::and-dynamicParameters-with[]
			MySearchParameters searchParameters = getSearchParameters(); // <1>
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.and().with( and -> { // <2>
						and.add( f.matchAll() ); // <3>
						if ( searchParameters.getGenreFilter() != null ) { // <4>
							and.add( f.match().field( "genre" )
									.matching( searchParameters.getGenreFilter() ) );
						}
						if ( searchParameters.getFullTextFilter() != null ) {
							and.add( f.match().fields( "title", "description" )
									.matching( searchParameters.getFullTextFilter() ) );
						}
						if ( searchParameters.getPageCountMaxFilter() != null ) {
							and.add( f.range().field( "pageCount" )
									.atMost( searchParameters.getPageCountMaxFilter() ) );
						}
					} ) )
					.fetchHits( 20 );
			// end::and-dynamicParameters-with[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID );
		} );

	}

	@Test
	public void or() {
		withinSearchSession( searchSession -> {
			// tag::or[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.or(
									f.match().field( "title" )
											.matching( "robot" ), // <1>
									f.match().field( "description" )
											.matching( "investigation" ) // <2>
							)
					)
					.fetchHits( 20 ); // <3>
			// end::or[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID, BOOK3_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::or-dynamicParameters-root[]
			MySearchParameters searchParameters = getSearchParameters(); // <1>
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.or().with( or -> { // <2>
						if ( !searchParameters.getAuthorFilters().isEmpty() ) {
							for ( String authorFilter : searchParameters.getAuthorFilters() ) { // <3>
								or.add( f.match().fields( "authors.firstName", "authors.lastName" )
										.matching( authorFilter ) );
							}
						}
					} ) )
					.fetchHits( 20 );
			// end::or-dynamicParameters-root[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID, BOOK3_ID );
		} );
	}

	@Test
	public void bool() {
		withinSearchSession( searchSession -> {
			// tag::bool-mustNot[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.bool()
							.must( f.match().field( "title" )
									.matching( "robot" ) ) // <1>
							.mustNot( f.match().field( "description" )
									.matching( "investigation" ) ) // <2>
					)
					.fetchHits( 20 ); // <3>
			// end::bool-mustNot[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK3_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::bool-filter[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.bool() // <1>
							.should( f.bool() // <2>
									.filter( f.match().field( "genre" )
											.matching( Genre.SCIENCE_FICTION ) ) // <3>
									.must( f.match().fields( "description" )
											.matching( "crime" ) ) // <4>
							)
							.should( f.bool() // <5>
									.filter( f.match().field( "genre" )
											.matching( Genre.CRIME_FICTION ) ) // <6>
									.must( f.match().fields( "description" )
											.matching( "robot" ) ) // <7>
							)
					)
					.fetchHits( 20 ); // <8>
			// end::bool-filter[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK3_ID, BOOK4_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::bool-mustAndShould[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.bool()
							.must( f.match().field( "title" )
									.matching( "robot" ) ) // <1>
							.should( f.match().field( "description" )
									.matching( "crime" ) ) // <2>
							.should( f.match().field( "description" )
									.matching( "investigation" ) ) // <3>
					)
					.fetchHits( 20 ); // <4>
			// end::bool-mustAndShould[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK3_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::bool-minimumShouldMatchNumber[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.bool()
							.minimumShouldMatchNumber( 2 ) // <1>
							.should( f.match().field( "description" )
									.matching( "robot" ) ) // <2>
							.should( f.match().field( "description" )
									.matching( "investigation" ) ) // <3>
							.should( f.match().field( "description" )
									.matching( "disappearance" ) ) // <4>
					)
					.fetchHits( 20 ); // <5>
			// end::bool-minimumShouldMatchNumber[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK2_ID, BOOK4_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::bool-dynamicParameters-with[]
			MySearchParameters searchParameters = getSearchParameters(); // <1>
			List<Book> hits = searchSession.search( Book.class )
					.where( (f, root) -> { // <2>
						root.add( f.matchAll() );
						if ( searchParameters.getGenreFilter() != null ) {
							root.add( f.match().field( "genre" )
									.matching( searchParameters.getGenreFilter() ) );
						}
						if ( !searchParameters.getAuthorFilters().isEmpty() ) {
							root.add( f.bool().with( b -> { // <3>
								for ( String authorFilter : searchParameters.getAuthorFilters() ) { // <4>
									b.should( f.match().fields( "authors.firstName", "authors.lastName" )
											.matching( authorFilter ) );
								}
							} ) );
						}
					} )
					.fetchHits( 20 );
			// end::bool-dynamicParameters-with[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID, BOOK3_ID );
		} );
	}

	@Test
	@SuppressWarnings("deprecation")
	public void bool_deprecated() {
		withinSearchSession( searchSession -> {
			// tag::bool-dynamicParameters-deprecated[]
			MySearchParameters searchParameters = getSearchParameters(); // <1>
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.bool( b -> { // <2>
						b.must( f.matchAll() ); // <3>
						if ( searchParameters.getGenreFilter() != null ) { // <4>
							b.must( f.match().field( "genre" )
									.matching( searchParameters.getGenreFilter() ) );
						}
						if ( searchParameters.getFullTextFilter() != null ) {
							b.must( f.match().fields( "title", "description" )
									.matching( searchParameters.getFullTextFilter() ) );
						}
						if ( searchParameters.getPageCountMaxFilter() != null ) {
							b.must( f.range().field( "pageCount" )
									.atMost( searchParameters.getPageCountMaxFilter() ) );
						}
					} ) )
					.fetchHits( 20 );
			// end::bool-dynamicParameters-deprecated[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID );
		} );
	}

	@Test
	public void match() {
		withinSearchSession( searchSession -> {
			// tag::match[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.match().field( "title" )
							.matching( "robot" ) )
					.fetchHits( 20 );
			// end::match[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK3_ID );
		} );
	}

	@Test
	public void match_analysis() {
		withinSearchSession( searchSession -> {
			// tag::match-normalized[]
			List<Author> hits = searchSession.search( Author.class )
					.where( f -> f.match().field( "lastName" )
							.matching( "ASIMOV" ) )// <1>
					.fetchHits( 20 );

			assertThat( hits ).extracting( Author::getLastName )
					.contains( "Asimov" );// <2>
			// end::match-normalized[]
		} );

		withinSearchSession( searchSession -> {
			// tag::match-multipleTerms[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.match().field( "title" )
							.matching( "ROBOT Dawn" ) ) // <1>
					.fetchHits( 20 );

			assertThat( hits ).extracting( Book::getTitle )
					.contains( "The Robots of Dawn", "I, Robot" ); // <2>
			// end::match-multipleTerms[]
		} );
	}

	@Test
	public void match_fuzzy() {
		withinSearchSession( searchSession -> {
			// tag::match-fuzzy[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.match()
							.field( "title" )
							.matching( "robto" )
							.fuzzy() )
					.fetchHits( 20 );
			// end::match-fuzzy[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK3_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::match-fuzzy-maxEditDistance[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.match()
							.field( "title" )
							.matching( "robto" )
							.fuzzy( 1 ) )
					.fetchHits( 20 );
			// end::match-fuzzy-maxEditDistance[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK3_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::match-fuzzy-exactPrefixLength[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.match()
							.field( "title" )
							.matching( "robto" )
							.fuzzy( 1, 3 ) )
					.fetchHits( 20 );
			// end::match-fuzzy-exactPrefixLength[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK3_ID );
		} );
	}

	@Test
	public void multipleFields() {
		withinSearchSession( searchSession -> {
			// tag::multipleFields-fieldOrField[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.match()
							.field( "title" ).field( "description" )
							.matching( "robot" ) )
					.fetchHits( 20 );
			// end::multipleFields-fieldOrField[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID, BOOK3_ID, BOOK4_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::multipleFields-fields[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.match()
							.fields( "title", "description" )
							.matching( "robot" ) )
					.fetchHits( 20 );
			// end::multipleFields-fields[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID, BOOK3_ID, BOOK4_ID );
		} );
	}

	@Test
	public void overrideAnalysis() {
		withinSearchSession( searchSession -> {
			// tag::overrideAnalysis-analyzer[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.match()
							.field( "title_autocomplete" )
							.matching( "robo" )
							.analyzer( "autocomplete_query" ) )
					.fetchHits( 20 );
			// end::overrideAnalysis-analyzer[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK3_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::overrideAnalysis-skipAnalysis[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.match()
							.field( "title" )
							.matching( "robot" )
							.skipAnalysis() )
					.fetchHits( 20 );
			// end::overrideAnalysis-skipAnalysis[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK3_ID );
		} );
	}

	@Test
	public void score() {
		withinSearchSession( searchSession -> {
			// tag::score-constantScore[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.and(
							f.match()
									.field( "genre" )
									.matching( Genre.SCIENCE_FICTION )
									.constantScore(),
							f.match()
									.field( "title" )
									.matching( "robot" )
									.boost( 2.0f )
					) )
					.fetchHits( 20 );
			// end::score-constantScore[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK3_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::score-boost[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.and(
							f.match()
									.field( "title" )
									.matching( "robot" )
									.boost( 2.0f ),
							f.match()
									.field( "description" )
									.matching( "self-aware" )
					) )
					.fetchHits( 20 );
			// end::score-boost[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::score-boost-multipleFields[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.match()
							.field( "title" ).boost( 2.0f )
							.field( "description" )
							.matching( "robot" ) )
					.fetchHits( 20 );
			// end::score-boost-multipleFields[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID, BOOK3_ID, BOOK4_ID );
		} );
	}

	@Test
	public void range() {
		withinSearchSession( searchSession -> {
			// tag::range-between[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.range().field( "pageCount" )
							.between( 210, 250 ) )
					.fetchHits( 20 );
			// end::range-between[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK4_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::range-atLeast[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.range().field( "pageCount" )
							.atLeast( 400 ) )
					.fetchHits( 20 );
			// end::range-atLeast[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK3_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::range-greaterThan[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.range().field( "pageCount" )
							.greaterThan( 400 ) )
					.fetchHits( 20 );
			// end::range-greaterThan[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK3_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::range-atMost[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.range().field( "pageCount" )
							.atMost( 400 ) )
					.fetchHits( 20 );
			// end::range-atMost[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID, BOOK4_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::range-lessThan[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.range().field( "pageCount" )
							.lessThan( 400 ) )
					.fetchHits( 20 );
			// end::range-lessThan[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID, BOOK4_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::range-between-advanced[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.range().field( "pageCount" )
							.between(
									200, RangeBoundInclusion.EXCLUDED,
									250, RangeBoundInclusion.EXCLUDED
							) )
					.fetchHits( 20 );
			// end::range-between-advanced[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK2_ID, BOOK4_ID );
		} );
	}

	@Test
	public void phrase() {
		withinSearchSession( searchSession -> {
			// tag::phrase[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.phrase().field( "title" )
							.matching( "robots of dawn" ) )
					.fetchHits( 20 );
			// end::phrase[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK3_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::phrase-slop[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.phrase().field( "title" )
							.matching( "dawn robot" )
							.slop( 3 ) )
					.fetchHits( 20 );
			// end::phrase-slop[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK3_ID );
		} );
	}

	@Test
	public void simpleQueryString() {
		withinSearchSession( searchSession -> {
			// tag::simpleQueryString-boolean[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.simpleQueryString().field( "description" )
							.matching( "robots + (crime | investigation | disappearance)" ) )
					.fetchHits( 20 );
			// end::simpleQueryString-boolean[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK2_ID, BOOK4_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::simpleQueryString-not[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.simpleQueryString().field( "description" )
							.matching( "robots + -investigation" ) )
					.fetchHits( 20 );
			// end::simpleQueryString-not[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK4_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::simpleQueryString-defaultOperator-and[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.simpleQueryString().field( "description" )
							.matching( "robots investigation" )
							.defaultOperator( BooleanOperator.AND ) )
					.fetchHits( 20 );
			// end::simpleQueryString-defaultOperator-and[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK2_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::simpleQueryString-prefix[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.simpleQueryString().field( "description" )
							.matching( "rob*" ) )
					.fetchHits( 20 );
			// end::simpleQueryString-prefix[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID, BOOK3_ID, BOOK4_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::simpleQueryString-fuzzy[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.simpleQueryString().field( "description" )
							.matching( "robto~2" ) )
					.fetchHits( 20 );
			// end::simpleQueryString-fuzzy[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID, BOOK4_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::simpleQueryString-phrase[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.simpleQueryString().field( "title" )
							.matching( "\"robots of dawn\"" ) )
					.fetchHits( 20 );
			// end::simpleQueryString-phrase[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK3_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::simpleQueryString-phrase-slop[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.simpleQueryString().field( "title" )
							.matching( "\"dawn robot\"~3" ) )
					.fetchHits( 20 );
			// end::simpleQueryString-phrase-slop[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK3_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::simpleQueryString-flags[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.simpleQueryString().field( "title" )
							.matching( "I want a **robot**" )
							.flags( SimpleQueryFlag.AND, SimpleQueryFlag.OR, SimpleQueryFlag.NOT ) )
					.fetchHits( 20 );
			// end::simpleQueryString-flags[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK3_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::simpleQueryString-flags-none[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.simpleQueryString().field( "title" )
							.matching( "**robot**" )
							.flags( Collections.emptySet() ) )
					.fetchHits( 20 );
			// end::simpleQueryString-flags-none[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK3_ID );
		} );
	}

	@Test
	public void exists() {
		withinSearchSession( searchSession -> {
			// tag::exists[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.exists().field( "comment" ) )
					.fetchHits( 20 );
			// end::exists[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK2_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::exists-object[]
			List<Author> hits = searchSession.search( Author.class )
					.where( f -> f.exists().field( "placeOfBirth" ) )
					.fetchHits( 20 );
			// end::exists-object[]
			assertThat( hits )
					.extracting( Author::getId )
					.containsExactlyInAnyOrder( ASIMOV_ID, MARTINEZ_ID );
		} );
	}

	@Test
	public void wildcard() {
		withinSearchSession( searchSession -> {
			// tag::wildcard[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.wildcard().field( "description" )
							.matching( "rob*t" ) )
					.fetchHits( 20 );
			// end::wildcard[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID, BOOK4_ID );
		} );
	}

	@Test
	public void regexp() {
		withinSearchSession( searchSession -> {
			// tag::regexp[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.regexp().field( "description" )
							.matching( "r.*t" ) )
					.fetchHits( 20 );
			// end::regexp[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID, BOOK4_ID );

			// tag::regexp-flags[]
			hits = searchSession.search( Book.class )
					.where( f -> f.regexp().field( "description" )
							.matching( "r@t" )
							.flags( RegexpQueryFlag.ANY_STRING, RegexpQueryFlag.COMPLEMENT )
					)
					.fetchHits( 20 );
			// end::regexp-flags[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID, BOOK4_ID );
		} );
	}

	@Test
	public void terms_any() {
		withinSearchSession( searchSession -> {
			// tag::terms-any[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.terms().field( "genre" )
							.matchingAny( Genre.CRIME_FICTION, Genre.SCIENCE_FICTION ) )
					.fetchHits( 20 );
			// end::terms-any[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID, BOOK3_ID, BOOK4_ID );
		} );
	}

	@Test
	public void terms_all() {
		withinSearchSession( searchSession -> {
			// tag::terms-all[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.terms().field( "description" )
							.matchingAll( "robot", "cab" ) )
					.fetchHits( 20 );
			// end::terms-all[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK4_ID );
		} );
	}

	@Test
	public void nested() {
		withinSearchSession( searchSession -> {
			// tag::nested[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.nested( "authors" ) // <1>
							.add( f.match().field( "authors.firstName" )
									.matching( "isaac" ) ) // <2>
							.add( f.match().field( "authors.lastName" )
									.matching( "asimov" ) ) ) // <3>
					.fetchHits( 20 ); // <4>
			// end::nested[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID, BOOK3_ID );
		} );
	}

	@Test
	public void nested_implicit() {
		withinSearchSession( searchSession -> {
			// tag::nested-implicit-form[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.and()
							.add( f.match().field( "authors.firstName" ) // <1>
									.matching( "isaac" ) ) // <2>
							.add( f.match().field( "authors.lastName" )
									.matching( "asimov" ) ) ) // <3>
					.fetchHits( 20 ); // <4>
			// end::nested-implicit-form[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID, BOOK3_ID );
		} );
	}

	@Test
	@SuppressWarnings("deprecation")
	public void nested_deprecated() {
		withinSearchSession( searchSession -> {
			// tag::nested-deprecated[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.nested().objectField( "authors" ) // <1>
							.nest( f.and()
									.add( f.match().field( "authors.firstName" )
											.matching( "isaac" ) ) // <2>
									.add( f.match().field( "authors.lastName" )
											.matching( "asimov" ) ) ) ) // <3>
					.fetchHits( 20 ); // <4>
			// end::nested-deprecated[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID, BOOK3_ID );
		} );
	}

	@Test
	public void within() {
		withinSearchSession( searchSession -> {
			// tag::within-circle[]
			GeoPoint center = GeoPoint.of( 53.970000, 32.150000 );
			List<Author> hits = searchSession.search( Author.class )
					.where( f -> f.spatial().within().field( "placeOfBirth.coordinates" )
							.circle( center, 50, DistanceUnit.KILOMETERS ) )
					.fetchHits( 20 );
			// end::within-circle[]
			assertThat( hits )
					.extracting( Author::getId )
					.containsExactlyInAnyOrder( ASIMOV_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::within-circle-doubles[]
			List<Author> hits = searchSession.search( Author.class )
					.where( f -> f.spatial().within().field( "placeOfBirth.coordinates" )
							.circle( 53.970000, 32.150000, 50, DistanceUnit.KILOMETERS ) )
					.fetchHits( 20 );
			// end::within-circle-doubles[]
			assertThat( hits )
					.extracting( Author::getId )
					.containsExactlyInAnyOrder( ASIMOV_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::within-box[]
			GeoBoundingBox box = GeoBoundingBox.of(
					53.99, 32.13,
					53.95, 32.17
			);
			List<Author> hits = searchSession.search( Author.class )
					.where( f -> f.spatial().within().field( "placeOfBirth.coordinates" )
							.boundingBox( box ) )
					.fetchHits( 20 );
			// end::within-box[]
			assertThat( hits )
					.extracting( Author::getId )
					.containsExactlyInAnyOrder( ASIMOV_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::within-box-doubles[]
			List<Author> hits = searchSession.search( Author.class )
					.where( f -> f.spatial().within().field( "placeOfBirth.coordinates" )
							.boundingBox( 53.99, 32.13,
									53.95, 32.17 ) )
					.fetchHits( 20 );
			// end::within-box-doubles[]
			assertThat( hits )
					.extracting( Author::getId )
					.containsExactlyInAnyOrder( ASIMOV_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::within-polygon[]
			GeoPolygon polygon = GeoPolygon.of(
					GeoPoint.of( 53.976177, 32.138627 ),
					GeoPoint.of( 53.986177, 32.148627 ),
					GeoPoint.of( 53.979177, 32.168627 ),
					GeoPoint.of( 53.876177, 32.159627 ),
					GeoPoint.of( 53.956177, 32.155627 ),
					GeoPoint.of( 53.976177, 32.138627 )
			);
			List<Author> hits = searchSession.search( Author.class )
					.where( f -> f.spatial().within().field( "placeOfBirth.coordinates" )
							.polygon( polygon ) )
					.fetchHits( 20 );
			// end::within-polygon[]
			assertThat( hits )
					.extracting( Author::getId )
					.containsExactlyInAnyOrder( ASIMOV_ID );
		} );
	}

	@Test
	public void not() {
		withinSearchSession( searchSession -> {
			// tag::not[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.not(
							f.match()
									.field( "genre" )
									.matching( Genre.SCIENCE_FICTION )
					) )
					.fetchHits( 20 );
			// end::not[]
			assertThat( hits )
					.extracting( Book::getGenre )
					.isNotEmpty()
					.doesNotContain( Genre.SCIENCE_FICTION );
		} );
	}

	private MySearchParameters getSearchParameters() {
		return new MySearchParameters() {
			@Override
			public Genre getGenreFilter() {
				return Genre.SCIENCE_FICTION;
			}

			@Override
			public String getFullTextFilter() {
				return "robot";
			}

			@Override
			public Integer getPageCountMaxFilter() {
				return 400;
			}

			@Override
			public List<String> getAuthorFilters() {
				return Collections.singletonList( "asimov" );
			}
		};
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

			Address address1 = new Address();
			address1.setCountry( "Russia" );
			address1.setCity( "Petrovichi" );
			address1.setCoordinates( EmbeddableGeoPoint.of( 53.976177, 32.158627 ) );
			isaacAsimov.setPlaceOfBirth( address1 );

			Author aLeeMartinez = new Author();
			aLeeMartinez.setId( MARTINEZ_ID );
			aLeeMartinez.setFirstName( "A. Lee" );
			aLeeMartinez.setLastName( "Martinez" );

			Address address2 = new Address();
			address2.setCountry( "United States of America" );
			address2.setCity( "El Paso" );
			address2.setCoordinates( EmbeddableGeoPoint.of( 31.814315, -106.475524 ) );
			aLeeMartinez.setPlaceOfBirth( address2 );

			Book book1 = new Book();
			book1.setId( BOOK1_ID );
			book1.setTitle( "I, Robot" );
			book1.setDescription( "A robot becomes self-aware." );
			book1.setPageCount( 250 );
			book1.setGenre( Genre.SCIENCE_FICTION );
			book1.getAuthors().add( isaacAsimov );
			isaacAsimov.getBooks().add( book1 );

			Book book2 = new Book();
			book2.setId( BOOK2_ID );
			book2.setTitle( "The Caves of Steel" );
			book2.setDescription( "A robot helps investigate a murder on an extrasolar colony." );
			book2.setPageCount( 206 );
			book2.setGenre( Genre.SCIENCE_FICTION );
			book2.setComment( "Really liked this one!" );
			book2.getAuthors().add( isaacAsimov );
			isaacAsimov.getBooks().add( book2 );

			Book book3 = new Book();
			book3.setId( BOOK3_ID );
			book3.setTitle( "The Robots of Dawn" );
			book3.setDescription( "A crime story about the first \"roboticide\"." );
			book3.setPageCount( 435 );
			book3.setGenre( Genre.SCIENCE_FICTION );
			book3.getAuthors().add( isaacAsimov );
			isaacAsimov.getBooks().add( book3 );

			Book book4 = new Book();
			book4.setId( BOOK4_ID );
			book4.setTitle( "The Automatic Detective" );
			book4.setDescription( "A robot cab driver turns PI after the disappearance of a neighboring family." );
			book4.setPageCount( 222 );
			book4.setGenre( Genre.CRIME_FICTION );
			book4.getAuthors().add( aLeeMartinez );
			aLeeMartinez.getBooks().add( book3 );

			entityManager.persist( isaacAsimov );
			entityManager.persist( aLeeMartinez );

			entityManager.persist( book1 );
			entityManager.persist( book2 );
			entityManager.persist( book3 );
			entityManager.persist( book4 );
		} );
	}

	private interface MySearchParameters {
		Genre getGenreFilter();
		String getFullTextFilter();
		Integer getPageCountMaxFilter();
		List<String> getAuthorFilters();
	}
}
