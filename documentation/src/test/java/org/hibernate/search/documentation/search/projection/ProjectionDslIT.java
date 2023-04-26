/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.search.projection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchHitsAssert.assertThatHits;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.persistence.EntityManagerFactory;

import org.hibernate.Session;
import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.pojo.common.spi.PojoEntityReference;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.assertion.TestComparators;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;

public class ProjectionDslIT {

	private static final int ASIMOV_ID = 1;
	private static final int MARTINEZ_ID = 2;

	private static final String BOOK_INDEX_NAME = Book.class.getSimpleName();
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

			List<String> result = searchSession.search( Book.class ) // <2>
					.select( f -> f.field( "title", String.class ) ) // <3>
					.where( f -> f.matchAll() )
					.fetchHits( 20 ); // <4>
			// end::entryPoint-lambdas[]
			assertThat( result ).containsExactlyInAnyOrder(
					entityManager.getReference( Book.class, BOOK1_ID ).getTitle(),
					entityManager.getReference( Book.class, BOOK2_ID ).getTitle(),
					entityManager.getReference( Book.class, BOOK3_ID ).getTitle(),
					entityManager.getReference( Book.class, BOOK4_ID ).getTitle()
			);
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			// tag::entryPoint-objects[]
			SearchSession searchSession = /* ... */
					// end::entryPoint-objects[]
					Search.session( entityManager );
			// tag::entryPoint-objects[]

			SearchScope<Book> scope = searchSession.scope( Book.class );

			List<String> result = searchSession.search( scope )
					.select( scope.projection().field( "title", String.class )
							.toProjection() )
					.where( scope.predicate().matchAll().toPredicate() )
					.fetchHits( 20 );
			// end::entryPoint-objects[]
			assertThat( result ).containsExactlyInAnyOrder(
					entityManager.getReference( Book.class, BOOK1_ID ).getTitle(),
					entityManager.getReference( Book.class, BOOK2_ID ).getTitle(),
					entityManager.getReference( Book.class, BOOK3_ID ).getTitle(),
					entityManager.getReference( Book.class, BOOK4_ID ).getTitle()
			);
		} );
	}

	@Test
	public void documentReference() {
		withinSearchSession( searchSession -> {
			// tag::documentReference[]
			List<DocumentReference> hits = searchSession.search( Book.class )
					.select( f -> f.documentReference() )
					.where( f -> f.matchAll() )
					.fetchHits( 20 );
			// end::documentReference[]
			assertThatHits( hits ).hasDocRefHitsAnyOrder(
					BOOK_INDEX_NAME,
					String.valueOf( BOOK1_ID ),
					String.valueOf( BOOK2_ID ),
					String.valueOf( BOOK3_ID ),
					String.valueOf( BOOK4_ID )
			);
		} );
	}

	@Test
	public void reference() {
		withinSearchSession( searchSession -> {
			// tag::reference[]
			List<? extends EntityReference> hits = searchSession.search( Book.class )
					.select( f -> f.entityReference() )
					.where( f -> f.matchAll() )
					.fetchHits( 20 );
			// end::reference[]
			Assertions.<EntityReference>assertThat( hits ).containsExactlyInAnyOrder(
					PojoEntityReference.withDefaultName( Book.class, BOOK1_ID ),
					PojoEntityReference.withDefaultName( Book.class, BOOK2_ID ),
					PojoEntityReference.withDefaultName( Book.class, BOOK3_ID ),
					PojoEntityReference.withDefaultName( Book.class, BOOK4_ID )
			);
		} );
	}

	@Test
	public void id_object() {
		withinSearchSession( searchSession -> {
			// tag::id-object[]
			List<Object> hits = searchSession.search( Book.class )
					.select( f -> f.id() )
					.where( f -> f.matchAll() )
					.fetchHits( 20 );
			// end::id-object[]
			assertThat( hits ).containsExactlyInAnyOrder(
					BOOK1_ID, BOOK2_ID, BOOK3_ID, BOOK4_ID
			);
		} );
	}

	@Test
	public void id_int() {
		withinSearchSession( searchSession -> {
			// tag::id-int[]
			List<Integer> hits = searchSession.search( Book.class )
					.select( f -> f.id( Integer.class ) )
					.where( f -> f.matchAll() )
					.fetchHits( 20 );
			// end::id-int[]
			assertThat( hits ).containsExactlyInAnyOrder(
					BOOK1_ID, BOOK2_ID, BOOK3_ID, BOOK4_ID
			);
		} );
	}

	@Test
	public void entity() {
		withinSearchSession( searchSession -> {
			// tag::entity[]
			List<Book> hits = searchSession.search( Book.class )
					.select( f -> f.entity() )
					.where( f -> f.matchAll() )
					.fetchHits( 20 );
			// end::entity[]
			Session session = searchSession.toOrmSession();
			assertThat( hits ).containsExactlyInAnyOrder(
					session.getReference( Book.class, BOOK1_ID ),
					session.getReference( Book.class, BOOK2_ID ),
					session.getReference( Book.class, BOOK3_ID ),
					session.getReference( Book.class, BOOK4_ID )
			);
		} );
	}

	@Test
	public void field() {
		withinSearchSession( searchSession -> {
			// tag::field[]
			List<Genre> hits = searchSession.search( Book.class )
					.select( f -> f.field( "genre", Genre.class ) )
					.where( f -> f.matchAll() )
					.fetchHits( 20 );
			// end::field[]
			Session session = searchSession.toOrmSession();
			assertThat( hits ).containsExactlyInAnyOrder(
					session.getReference( Book.class, BOOK1_ID ).getGenre(),
					session.getReference( Book.class, BOOK2_ID ).getGenre(),
					session.getReference( Book.class, BOOK3_ID ).getGenre(),
					session.getReference( Book.class, BOOK4_ID ).getGenre()
			);
		} );

		withinSearchSession( searchSession -> {
			// tag::field-multiValued[]
			List<List<String>> hits = searchSession.search( Book.class )
					.select( f -> f.field( "authors.lastName", String.class ).multi() )
					.where( f -> f.matchAll() )
					.fetchHits( 20 );
			// end::field-multiValued[]
			Session session = searchSession.toOrmSession();
			assertThat( hits ).containsExactlyInAnyOrder(
					session.getReference( Book.class, BOOK1_ID ).getAuthors().stream()
							.map( Author::getLastName )
							.collect( Collectors.toList() ),
					session.getReference( Book.class, BOOK2_ID ).getAuthors().stream()
							.map( Author::getLastName )
							.collect( Collectors.toList() ),
					session.getReference( Book.class, BOOK3_ID ).getAuthors().stream()
							.map( Author::getLastName )
							.collect( Collectors.toList() ),
					session.getReference( Book.class, BOOK4_ID ).getAuthors().stream()
							.map( Author::getLastName )
							.collect( Collectors.toList() )
			);
		} );

		withinSearchSession( searchSession -> {
			// tag::field-noType[]
			List<Object> hits = searchSession.search( Book.class )
					.select( f -> f.field( "genre" ) )
					.where( f -> f.matchAll() )
					.fetchHits( 20 );
			// end::field-noType[]
			Session session = searchSession.toOrmSession();
			assertThat( hits ).containsExactlyInAnyOrder(
					session.getReference( Book.class, BOOK1_ID ).getGenre(),
					session.getReference( Book.class, BOOK2_ID ).getGenre(),
					session.getReference( Book.class, BOOK3_ID ).getGenre(),
					session.getReference( Book.class, BOOK4_ID ).getGenre()
			);
		} );

		withinSearchSession( searchSession -> {
			// tag::field-noProjectionConverter[]
			List<String> hits = searchSession.search( Book.class )
					.select( f -> f.field( "genre", String.class, ValueConvert.NO ) )
					.where( f -> f.matchAll() )
					.fetchHits( 20 );
			// end::field-noProjectionConverter[]
			Session session = searchSession.toOrmSession();
			assertThat( hits ).containsExactlyInAnyOrder(
					session.getReference( Book.class, BOOK1_ID ).getGenre().name(),
					session.getReference( Book.class, BOOK2_ID ).getGenre().name(),
					session.getReference( Book.class, BOOK3_ID ).getGenre().name(),
					session.getReference( Book.class, BOOK4_ID ).getGenre().name()
			);
		} );
	}

	@Test
	public void score() {
		withinSearchSession( searchSession -> {
			// tag::score[]
			List<Float> hits = searchSession.search( Book.class )
					.select( f -> f.score() )
					.where( f -> f.match().field( "title" )
							.matching( "robot dawn" ) )
					.fetchHits( 20 );
			// end::score[]
			assertThat( hits )
					.hasSize( 2 )
					.allSatisfy(
							score -> assertThat( score ).isGreaterThan( 0.0f )
					);
		} );
	}

	@Test
	public void distance() {
		withinSearchSession( searchSession -> {
			// tag::distance[]
			GeoPoint center = GeoPoint.of( 47.506060, 2.473916 );
			SearchResult<Double> result = searchSession.search( Author.class )
					.select( f -> f.distance( "placeOfBirth", center ) )
					.where( f -> f.matchAll() )
					.fetch( 20 );
			// end::distance[]
			assertThat( result.hits() )
					.hasSize( 2 )
					.allSatisfy(
							distance -> assertThat( distance ).isBetween( 1_000_000.0, 10_000_000.0 )
					);
		} );

		withinSearchSession( searchSession -> {
			// tag::distance-multiValued[]
			GeoPoint center = GeoPoint.of( 47.506060, 2.473916 );
			SearchResult<List<Double>> result = searchSession.search( Book.class )
					.select( f -> f.distance( "authors.placeOfBirth", center ).multi() )
					.where( f -> f.matchAll() )
					.fetch( 20 );
			// end::distance-multiValued[]
			assertThat( result.hits() )
					.hasSize( 4 )
					.allSatisfy(
							distances -> assertThat( distances ).isNotEmpty().allSatisfy(
									distance -> assertThat( distance ).isBetween( 1_000_000.0, 10_000_000.0 )
							)
					);
		} );

		withinSearchSession( searchSession -> {
			// tag::distance-unit[]
			GeoPoint center = GeoPoint.of( 47.506060, 2.473916 );
			SearchResult<Double> result = searchSession.search( Author.class )
					.select( f -> f.distance( "placeOfBirth", center )
							.unit( DistanceUnit.KILOMETERS ) )
					.where( f -> f.matchAll() )
					.fetch( 20 );
			// end::distance-unit[]
			assertThat( result.hits() )
					.hasSize( 2 )
					.allSatisfy(
							distance -> assertThat( distance ).isBetween( 1_000.0, 10_000.0 )
					);
		} );
	}

	@Test
	public void composite() {
		withinSearchSession( searchSession -> {
			// tag::composite-customObject[]
			List<MyPair<String, Genre>> hits = searchSession.search( Book.class )
					.select( f -> f.composite() // <1>
							.from( f.field( "title", String.class ), // <2>
									f.field( "genre", Genre.class ) ) // <3>
							.as( MyPair::new ) )// <4>
					.where( f -> f.matchAll() )
					.fetchHits( 20 ); // <5>
			// end::composite-customObject[]
			Session session = searchSession.toOrmSession();
			assertThat( hits ).containsExactlyInAnyOrder(
					new MyPair<>(
							session.getReference( Book.class, BOOK1_ID ).getTitle(),
							session.getReference( Book.class, BOOK1_ID ).getGenre()
					),
					new MyPair<>(
							session.getReference( Book.class, BOOK2_ID ).getTitle(),
							session.getReference( Book.class, BOOK2_ID ).getGenre()
					),
					new MyPair<>(
							session.getReference( Book.class, BOOK3_ID ).getTitle(),
							session.getReference( Book.class, BOOK3_ID ).getGenre()
					),
					new MyPair<>(
							session.getReference( Book.class, BOOK4_ID ).getTitle(),
							session.getReference( Book.class, BOOK4_ID ).getGenre()
					)
			);
		} );

		withinSearchSession( searchSession -> {
			// tag::composite-customObject-asList[]
			List<MyTuple4<String, Genre, Integer, String>> hits = searchSession.search( Book.class )
					.select( f -> f.composite() // <1>
							.from( f.field( "title", String.class ), // <2>
									f.field( "genre", Genre.class ), // <3>
									f.field( "pageCount", Integer.class ), // <4>
									f.field( "description", String.class ) ) // <5>
							.asList( list -> // <6>
								new MyTuple4<>( (String) list.get( 0 ), (Genre) list.get( 1 ),
										(Integer) list.get( 2 ), (String) list.get( 3 ) ) ) )
					.where( f -> f.matchAll() )
					.fetchHits( 20 ); // <7>
			// end::composite-customObject-asList[]
			Session session = searchSession.toOrmSession();
			assertThat( hits ).containsExactlyInAnyOrder(
					new MyTuple4<>(
							session.getReference( Book.class, BOOK1_ID ).getTitle(),
							session.getReference( Book.class, BOOK1_ID ).getGenre(),
							session.getReference( Book.class, BOOK1_ID ).getPageCount(),
							session.getReference( Book.class, BOOK1_ID ).getDescription()
					),
					new MyTuple4<>(
							session.getReference( Book.class, BOOK2_ID ).getTitle(),
							session.getReference( Book.class, BOOK2_ID ).getGenre(),
							session.getReference( Book.class, BOOK2_ID ).getPageCount(),
							session.getReference( Book.class, BOOK2_ID ).getDescription()
					),
					new MyTuple4<>(
							session.getReference( Book.class, BOOK3_ID ).getTitle(),
							session.getReference( Book.class, BOOK3_ID ).getGenre(),
							session.getReference( Book.class, BOOK3_ID ).getPageCount(),
							session.getReference( Book.class, BOOK3_ID ).getDescription()
					),
					new MyTuple4<>(
							session.getReference( Book.class, BOOK4_ID ).getTitle(),
							session.getReference( Book.class, BOOK4_ID ).getGenre(),
							session.getReference( Book.class, BOOK4_ID ).getPageCount(),
							session.getReference( Book.class, BOOK4_ID ).getDescription()
					)
			);
		} );

		withinSearchSession( searchSession -> {
			// tag::composite-list[]
			List<List<?>> hits = searchSession.search( Book.class )
					.select( f -> f.composite() // <1>
							.from( f.field( "title", String.class ), // <2>
									f.field( "genre", Genre.class ) ) // <3>
							.asList() ) // <4>
					.where( f -> f.matchAll() )
					.fetchHits( 20 ); // <5>
			// end::composite-list[]
			Session session = searchSession.toOrmSession();
			assertThat( hits ).containsExactlyInAnyOrder(
					Arrays.asList(
							session.getReference( Book.class, BOOK1_ID ).getTitle(),
							session.getReference( Book.class, BOOK1_ID ).getGenre()
					),
					Arrays.asList(
							session.getReference( Book.class, BOOK2_ID ).getTitle(),
							session.getReference( Book.class, BOOK2_ID ).getGenre()
					),
					Arrays.asList(
							session.getReference( Book.class, BOOK3_ID ).getTitle(),
							session.getReference( Book.class, BOOK3_ID ).getGenre()
					),
					Arrays.asList(
							session.getReference( Book.class, BOOK4_ID ).getTitle(),
							session.getReference( Book.class, BOOK4_ID ).getGenre()
					)
			);
		} );

		withinSearchSession( searchSession -> {
			// tag::composite-array[]
			List<Object[]> hits = searchSession.search( Book.class )
					.select( f -> f.composite() // <1>
							.from( f.field( "title", String.class ), // <2>
									f.field( "genre", Genre.class ) ) // <3>
							.asArray() ) // <4>
					.where( f -> f.matchAll() )
					.fetchHits( 20 ); // <5>
			// end::composite-array[]
			Session session = searchSession.toOrmSession();
			assertThat( hits ).containsExactlyInAnyOrder(
					new Object[] {
							session.getReference( Book.class, BOOK1_ID ).getTitle(),
							session.getReference( Book.class, BOOK1_ID ).getGenre()
					},
					new Object[] {
							session.getReference( Book.class, BOOK2_ID ).getTitle(),
							session.getReference( Book.class, BOOK2_ID ).getGenre()
					},
					new Object[] {
							session.getReference( Book.class, BOOK3_ID ).getTitle(),
							session.getReference( Book.class, BOOK3_ID ).getGenre()
					},
					new Object[] {
							session.getReference( Book.class, BOOK4_ID ).getTitle(),
							session.getReference( Book.class, BOOK4_ID ).getGenre()
					}
			);
		} );
	}

	@Test
	@SuppressWarnings("deprecation")
	public void composite_singleStep() {
		withinSearchSession( searchSession -> {
			// tag::composite-customObject-singlestep[]
			List<MyPair<String, Genre>> hits = searchSession.search( Book.class )
					.select( f -> f.composite( // <1>
							MyPair::new, // <2>
							f.field( "title", String.class ), // <3>
							f.field( "genre", Genre.class ) // <4>
					) )
					.where( f -> f.matchAll() )
					.fetchHits( 20 ); // <5>
			// end::composite-customObject-singlestep[]
			Session session = searchSession.toOrmSession();
			assertThat( hits ).containsExactlyInAnyOrder(
					new MyPair<>(
							session.getReference( Book.class, BOOK1_ID ).getTitle(),
							session.getReference( Book.class, BOOK1_ID ).getGenre()
					),
					new MyPair<>(
							session.getReference( Book.class, BOOK2_ID ).getTitle(),
							session.getReference( Book.class, BOOK2_ID ).getGenre()
					),
					new MyPair<>(
							session.getReference( Book.class, BOOK3_ID ).getTitle(),
							session.getReference( Book.class, BOOK3_ID ).getGenre()
					),
					new MyPair<>(
							session.getReference( Book.class, BOOK4_ID ).getTitle(),
							session.getReference( Book.class, BOOK4_ID ).getGenre()
					)
			);
		} );

		withinSearchSession( searchSession -> {
			// tag::composite-list-singlestep[]
			List<List<?>> hits = searchSession.search( Book.class )
					.select( f -> f.composite( // <1>
							f.field( "title", String.class ), // <2>
							f.field( "genre", Genre.class ) // <3>
					) )
					.where( f -> f.matchAll() )
					.fetchHits( 20 ); // <4>
			// end::composite-list-singlestep[]
			Session session = searchSession.toOrmSession();
			assertThat( hits ).containsExactlyInAnyOrder(
					Arrays.asList(
							session.getReference( Book.class, BOOK1_ID ).getTitle(),
							session.getReference( Book.class, BOOK1_ID ).getGenre()
					),
					Arrays.asList(
							session.getReference( Book.class, BOOK2_ID ).getTitle(),
							session.getReference( Book.class, BOOK2_ID ).getGenre()
					),
					Arrays.asList(
							session.getReference( Book.class, BOOK3_ID ).getTitle(),
							session.getReference( Book.class, BOOK3_ID ).getGenre()
					),
					Arrays.asList(
							session.getReference( Book.class, BOOK4_ID ).getTitle(),
							session.getReference( Book.class, BOOK4_ID ).getGenre()
					)
			);
		} );
	}

	@Test
	public void object() {
		withinSearchSession( searchSession -> {
			// tag::object-customObject[]
			List<List<MyAuthorName>> hits = searchSession.search( Book.class )
					.select( f -> f.object( "authors" ) // <1>
							.from( f.field( "authors.firstName", String.class ), // <2>
									f.field( "authors.lastName", String.class ) ) // <3>
							.as( MyAuthorName::new ) // <4>
							.multi() ) // <5>
					.where( f -> f.matchAll() )
					.fetchHits( 20 ); // <6>
			// end::object-customObject[]
			Session session = searchSession.toOrmSession();
			assertThat( hits ).usingRecursiveFieldByFieldElementComparator()
					.containsExactlyInAnyOrder(
					Collections.singletonList(
							new MyAuthorName(
									session.getReference( Book.class, BOOK1_ID ).getAuthors().get( 0 ).getFirstName(),
									session.getReference( Book.class, BOOK1_ID ).getAuthors().get( 0 ).getLastName()
							)
					),
					Collections.singletonList(
							new MyAuthorName(
									session.getReference( Book.class, BOOK2_ID ).getAuthors().get( 0 ).getFirstName(),
									session.getReference( Book.class, BOOK2_ID ).getAuthors().get( 0 ).getLastName()
							)
					),
					Collections.singletonList(
							new MyAuthorName(
									session.getReference( Book.class, BOOK3_ID ).getAuthors().get( 0 ).getFirstName(),
									session.getReference( Book.class, BOOK3_ID ).getAuthors().get( 0 ).getLastName()
							)
					),
					Collections.singletonList(
							new MyAuthorName(
									session.getReference( Book.class, BOOK4_ID ).getAuthors().get( 0 ).getFirstName(),
									session.getReference( Book.class, BOOK4_ID ).getAuthors().get( 0 ).getLastName()
							)
					)
			);
		} );

		withinSearchSession( searchSession -> {
			// tag::object-customObject-asList[]
			GeoPoint center = GeoPoint.of( 53.970000, 32.150000 );
			List<List<MyAuthorNameAndBirthDateAndPlaceOfBirthDistance>> hits = searchSession
					.search( Book.class )
					.select( f -> f.object( "authors" ) // <1>
							.from( f.field( "authors.firstName", String.class ), // <2>
									f.field( "authors.lastName", String.class ), // <3>
									f.field( "authors.birthDate", LocalDate.class ), // <4>
									f.distance( "authors.placeOfBirth", center ) // <5>
											.unit( DistanceUnit.KILOMETERS ) )
							.asList( list -> // <6>
									new MyAuthorNameAndBirthDateAndPlaceOfBirthDistance(
											(String) list.get( 0 ), (String) list.get( 1 ),
											(LocalDate) list.get( 2 ), (Double) list.get( 3 ) ) )
							.multi() ) // <7>
					.where( f -> f.matchAll() )
					.fetchHits( 20 ); // <8>
			// end::object-customObject-asList[]
			Session session = searchSession.toOrmSession();
			assertThat( hits )
					.usingRecursiveFieldByFieldElementComparator( RecursiveComparisonConfiguration.builder()
							.withComparatorForType( TestComparators.APPROX_KM_COMPARATOR, Double.class )
							.build() )
					.containsExactlyInAnyOrder(
							Collections.singletonList(
									new MyAuthorNameAndBirthDateAndPlaceOfBirthDistance(
											session.getReference( Book.class, BOOK1_ID ).getAuthors().get( 0 ).getFirstName(),
											session.getReference( Book.class, BOOK1_ID ).getAuthors().get( 0 ).getLastName(),
											session.getReference( Book.class, BOOK1_ID ).getAuthors().get( 0 ).getBirthDate(),
											0.888
									)
							),
							Collections.singletonList(
									new MyAuthorNameAndBirthDateAndPlaceOfBirthDistance(
											session.getReference( Book.class, BOOK2_ID ).getAuthors().get( 0 ).getFirstName(),
											session.getReference( Book.class, BOOK2_ID ).getAuthors().get( 0 ).getLastName(),
											session.getReference( Book.class, BOOK2_ID ).getAuthors().get( 0 ).getBirthDate(),
											0.888
									)
							),
							Collections.singletonList(
									new MyAuthorNameAndBirthDateAndPlaceOfBirthDistance(
											session.getReference( Book.class, BOOK3_ID ).getAuthors().get( 0 ).getFirstName(),
											session.getReference( Book.class, BOOK3_ID ).getAuthors().get( 0 ).getLastName(),
											session.getReference( Book.class, BOOK3_ID ).getAuthors().get( 0 ).getBirthDate(),
											0.888
									)
							),
							Collections.singletonList(
									new MyAuthorNameAndBirthDateAndPlaceOfBirthDistance(
											session.getReference( Book.class, BOOK4_ID ).getAuthors().get( 0 ).getFirstName(),
											session.getReference( Book.class, BOOK4_ID ).getAuthors().get( 0 ).getLastName(),
											session.getReference( Book.class, BOOK4_ID ).getAuthors().get( 0 ).getBirthDate(),
											9680.93
									)
							)
					);
		} );

		withinSearchSession( searchSession -> {
			// tag::object-customObject-asArray[]
			GeoPoint center = GeoPoint.of( 53.970000, 32.150000 );
			List<List<MyAuthorNameAndBirthDateAndPlaceOfBirthDistance>> hits = searchSession
					.search( Book.class )
					.select( f -> f.object( "authors" ) // <1>
							.from( f.field( "authors.firstName", String.class ), // <2>
									f.field( "authors.lastName", String.class ), // <3>
									f.field( "authors.birthDate", LocalDate.class ), // <4>
									f.distance( "authors.placeOfBirth", center ) // <5>
											.unit( DistanceUnit.KILOMETERS ) )
							.asArray( array -> // <6>
									new MyAuthorNameAndBirthDateAndPlaceOfBirthDistance(
											(String) array[0], (String) array[1],
											(LocalDate) array[2], (Double) array[3] ) )
							.multi() ) // <7>
					.where( f -> f.matchAll() )
					.fetchHits( 20 ); // <8>
			// end::object-customObject-asArray[]
			Session session = searchSession.toOrmSession();
			assertThat( hits )
					.usingRecursiveFieldByFieldElementComparator( RecursiveComparisonConfiguration.builder()
							.withComparatorForType( TestComparators.APPROX_KM_COMPARATOR, Double.class )
							.build() )
					.containsExactlyInAnyOrder(
							Collections.singletonList(
									new MyAuthorNameAndBirthDateAndPlaceOfBirthDistance(
											session.getReference( Book.class, BOOK1_ID ).getAuthors().get( 0 ).getFirstName(),
											session.getReference( Book.class, BOOK1_ID ).getAuthors().get( 0 ).getLastName(),
											session.getReference( Book.class, BOOK1_ID ).getAuthors().get( 0 ).getBirthDate(),
											0.888
									)
							),
							Collections.singletonList(
									new MyAuthorNameAndBirthDateAndPlaceOfBirthDistance(
											session.getReference( Book.class, BOOK2_ID ).getAuthors().get( 0 ).getFirstName(),
											session.getReference( Book.class, BOOK2_ID ).getAuthors().get( 0 ).getLastName(),
											session.getReference( Book.class, BOOK2_ID ).getAuthors().get( 0 ).getBirthDate(),
											0.888
									)
							),
							Collections.singletonList(
									new MyAuthorNameAndBirthDateAndPlaceOfBirthDistance(
											session.getReference( Book.class, BOOK3_ID ).getAuthors().get( 0 ).getFirstName(),
											session.getReference( Book.class, BOOK3_ID ).getAuthors().get( 0 ).getLastName(),
											session.getReference( Book.class, BOOK3_ID ).getAuthors().get( 0 ).getBirthDate(),
											0.888
									)
							),
							Collections.singletonList(
									new MyAuthorNameAndBirthDateAndPlaceOfBirthDistance(
											session.getReference( Book.class, BOOK4_ID ).getAuthors().get( 0 ).getFirstName(),
											session.getReference( Book.class, BOOK4_ID ).getAuthors().get( 0 ).getLastName(),
											session.getReference( Book.class, BOOK4_ID ).getAuthors().get( 0 ).getBirthDate(),
											9680.93
									)
							)
					);
		} );

		withinSearchSession( searchSession -> {
			// tag::object-list[]
			List<List<List<?>>> hits = searchSession.search( Book.class )
					.select( f -> f.object( "authors" ) // <1>
							.from( f.field( "authors.firstName", String.class ), // <2>
									f.field( "authors.lastName", String.class ) ) // <3>
							.asList() // <4>
							.multi() ) // <5>
					.where( f -> f.matchAll() )
					.fetchHits( 20 ); // <6>
			// end::object-list[]
			Session session = searchSession.toOrmSession();
			assertThat( hits ).containsExactlyInAnyOrder(
					Collections.singletonList(
							Arrays.asList(
									session.getReference( Book.class, BOOK1_ID ).getAuthors().get( 0 ).getFirstName(),
									session.getReference( Book.class, BOOK1_ID ).getAuthors().get( 0 ).getLastName()
							)
					),
					Collections.singletonList(
							Arrays.asList(
									session.getReference( Book.class, BOOK2_ID ).getAuthors().get( 0 ).getFirstName(),
									session.getReference( Book.class, BOOK2_ID ).getAuthors().get( 0 ).getLastName()
							)
					),
					Collections.singletonList(
							Arrays.asList(
									session.getReference( Book.class, BOOK3_ID ).getAuthors().get( 0 ).getFirstName(),
									session.getReference( Book.class, BOOK3_ID ).getAuthors().get( 0 ).getLastName()
							)
					),
					Collections.singletonList(
							Arrays.asList(
									session.getReference( Book.class, BOOK4_ID ).getAuthors().get( 0 ).getFirstName(),
									session.getReference( Book.class, BOOK4_ID ).getAuthors().get( 0 ).getLastName()
							)
					)
			);
		} );

		withinSearchSession( searchSession -> {
			// tag::object-array[]
			List<List<Object[]>> hits = searchSession.search( Book.class )
					.select( f -> f.object( "authors" ) // <1>
							.from( f.field( "authors.firstName", String.class ), // <2>
									f.field( "authors.lastName", String.class ) ) // <3>
							.asArray() // <4>
							.multi() ) // <5>
					.where( f -> f.matchAll() )
					.fetchHits( 20 ); // <6>
			// end::object-array[]
			Session session = searchSession.toOrmSession();
			assertThat( hits ).usingElementComparator( (left, right) -> {
				if ( left.size() != right.size() ) {
					return 1;
				}
				for ( int i = 0; i < left.size(); i++ ) {
					if ( !Objects.deepEquals( left.get( i ), right.get( i ) ) ) {
						return 1;
					}
				}
				return 0;
			} ).containsExactlyInAnyOrder(
					Collections.singletonList(
							new Object[] {
									session.getReference( Book.class, BOOK1_ID ).getAuthors().get( 0 ).getFirstName(),
									session.getReference( Book.class, BOOK1_ID ).getAuthors().get( 0 ).getLastName()
							}
					),
					Collections.singletonList(
							new Object[] {
									session.getReference( Book.class, BOOK2_ID ).getAuthors().get( 0 ).getFirstName(),
									session.getReference( Book.class, BOOK2_ID ).getAuthors().get( 0 ).getLastName()
							}
					),
					Collections.singletonList(
							new Object[] {
									session.getReference( Book.class, BOOK3_ID ).getAuthors().get( 0 ).getFirstName(),
									session.getReference( Book.class, BOOK3_ID ).getAuthors().get( 0 ).getLastName()
							}
					),
					Collections.singletonList(
							new Object[] {
									session.getReference( Book.class, BOOK4_ID ).getAuthors().get( 0 ).getFirstName(),
									session.getReference( Book.class, BOOK4_ID ).getAuthors().get( 0 ).getLastName()
							}
					)
			);
		} );
	}

	@Test
	public void constant() {
		withinSearchSession( searchSession -> {
			// tag::constant-incomposite[]
			Instant searchRequestTimestamp = Instant.now();
			List<MyPair<Integer, Instant>> hits = searchSession.search( Book.class )
					.select( f -> f.composite()
							.from( f.id( Integer.class ), f.constant( searchRequestTimestamp ) )
							.as( MyPair::new ) )
					.where( f -> f.matchAll() )
					.fetchHits( 20 );
			// end::constant-incomposite[]
			assertThat( hits ).containsExactlyInAnyOrder(
					new MyPair<>( BOOK1_ID, searchRequestTimestamp ),
					new MyPair<>( BOOK2_ID, searchRequestTimestamp ),
					new MyPair<>( BOOK3_ID, searchRequestTimestamp ),
					new MyPair<>( BOOK4_ID, searchRequestTimestamp )
			);
		} );
	}

	@Test
	public void highlight() {
		withinSearchSession( searchSession -> {
			// tag::highlight[]
			List<List<String>> hits = searchSession.search( Book.class )
					.select( f -> f.highlight( "title" ) )
					.where( f -> f.match().field( "title" ).matching( "detective" ) )
					.fetchHits( 20 );
			// end::highlight[]
			assertThat( hits ).containsExactlyInAnyOrder(
					Collections.singletonList( "The Automatic <em>Detective</em>" )
			);
		} );

		withinSearchSession( searchSession -> {
			// tag::highlight-multiValued[]
			List<List<String>> hits = searchSession.search( Book.class )
					.select( f -> f.highlight( "flattenedAuthors.lastName" ) )
					.where( f -> f.match().field( "flattenedAuthors.lastName" ).matching( "martinez" ) )
					.fetchHits( 20 );
			// end::highlight-multiValued[]
			assertThat( hits ).containsExactlyInAnyOrder(
					Collections.singletonList( "<em>Martinez</em>" )
			);
		} );
		withinSearchSession( searchSession -> {
			// tag::highlighter-default[]
			List<List<String>> hits = searchSession.search( Book.class )
					.select( f -> f.highlight( "title" ) ) // <1>
					.where( f -> f.match().field( "title" ).matching( "detective" ) )
					.highlighter( f -> f.unified().tag( "<b>", "</b>" ) ) // <2>
					.fetchHits( 20 );
			// end::highlighter-default[]
			assertThat( hits ).containsExactlyInAnyOrder(
					Collections.singletonList( "The Automatic <b>Detective</b>" )
			);
		} );

		withinSearchSession( searchSession -> {
			// tag::highlighter-named[]
			List<List<?>> hits = searchSession.search( Book.class )
					.select( f -> f.composite().from(
							f.highlight( "title" ),
							f.highlight( "description" ).highlighter( "description-highlighter" ) // <1>
					).asList() )
					.where( f -> f.match().field( "title" ).matching( "detective" ) )
					.highlighter( f -> f.unified().tag( "<b>", "</b>" ) ) // <2>
					.highlighter( "description-highlighter", f -> f.unified().tag( "<span>", "</span>" ) ) // <3>
					.fetchHits( 20 );
			// end::highlighter-named[]
			Session session = searchSession.toOrmSession();
			assertThat( hits ).containsExactlyInAnyOrder(
					Arrays.asList( Collections.singletonList( "The Automatic <b>Detective</b>" ), Collections.emptyList() )
			);
		} );
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
			isaacAsimov.setBirthDate( LocalDate.of( 1920, Month.JANUARY, 2 ) );
			isaacAsimov.setPlaceOfBirth( EmbeddableGeoPoint.of( 53.976177, 32.158627 ) );

			Author aLeeMartinez = new Author();
			aLeeMartinez.setId( MARTINEZ_ID );
			aLeeMartinez.setFirstName( "A. Lee" );
			aLeeMartinez.setLastName( "Martinez" );
			aLeeMartinez.setBirthDate( LocalDate.of( 1973, Month.JANUARY, 12 ) );
			aLeeMartinez.setPlaceOfBirth( EmbeddableGeoPoint.of( 31.814315, -106.475524 ) );

			Book book1 = new Book();
			book1.setId( BOOK1_ID );
			book1.setTitle( "I, Robot" );
			book1.setDescription( "A robot becomes self-aware." );
			book1.setPageCount( 250 );
			book1.setGenre( Genre.SCIENCE_FICTION );
			book1.addAuthor( isaacAsimov );

			Book book2 = new Book();
			book2.setId( BOOK2_ID );
			book2.setTitle( "The Caves of Steel" );
			book2.setDescription( "A robot helps investigate a murder on an extrasolar colony." );
			book2.setPageCount( 206 );
			book2.setGenre( Genre.SCIENCE_FICTION );
			book2.addAuthor( isaacAsimov );

			Book book3 = new Book();
			book3.setId( BOOK3_ID );
			book3.setTitle( "The Robots of Dawn" );
			book3.setDescription( "A crime story about the first \"roboticide\"." );
			book3.setPageCount( 435 );
			book3.setGenre( Genre.SCIENCE_FICTION );
			book3.addAuthor( isaacAsimov );

			Book book4 = new Book();
			book4.setId( BOOK4_ID );
			book4.setTitle( "The Automatic Detective" );
			book4.setDescription( "A robot cab driver turns PI after the disappearance of a neighboring family." );
			book4.setPageCount( 222 );
			book4.setGenre( Genre.CRIME_FICTION );
			book4.addAuthor( aLeeMartinez );
			entityManager.persist( isaacAsimov );
			entityManager.persist( aLeeMartinez );

			entityManager.persist( book1 );
			entityManager.persist( book2 );
			entityManager.persist( book3 );
			entityManager.persist( book4 );
		} );
	}

	private static class MyPair<T1, T2> {
		private final T1 first;
		private final T2 second;

		MyPair(T1 first, T2 second) {
			this.first = first;
			this.second = second;
		}

		@Override
		public boolean equals(Object obj) {
			if ( obj == null || !MyPair.class.equals( obj.getClass() ) ) {
				return false;
			}
			MyPair<?, ?> other = (MyPair<?, ?>) obj;
			return Objects.equals( first, other.first ) && Objects.equals( second, other.second );
		}

		@Override
		public int hashCode() {
			return Objects.hash( first, second );
		}
	}

	private static class MyTuple4<T1, T2, T3, T4> {
		private final T1 first;
		private final T2 second;
		private final T3 third;
		private final T4 fourth;

		MyTuple4(T1 first, T2 second, T3 third, T4 fourth) {
			this.first = first;
			this.second = second;
			this.third = third;
			this.fourth = fourth;
		}

		@Override
		public boolean equals(Object obj) {
			if ( obj == null || !MyTuple4.class.equals( obj.getClass() ) ) {
				return false;
			}
			MyTuple4<?, ?, ?, ?> other = (MyTuple4<?, ?, ?, ?>) obj;
			return Objects.equals( first, other.first ) && Objects.equals( second, other.second )
					&& Objects.equals( third, other.third ) && Objects.equals( fourth, other.fourth );
		}

		@Override
		public int hashCode() {
			return Objects.hash( first, second, third, fourth );
		}
	}

	private static class MyAuthorName {
		private final String firstName;
		private final String lastName;

		MyAuthorName(String firstName, String lastName) {
			this.firstName = firstName;
			this.lastName = lastName;
		}

		@Override
		public String toString() {
			return "MyAuthorName{" +
					"firstName='" + firstName + '\'' +
					", lastName='" + lastName + '\'' +
					'}';
		}
	}

	private static class MyAuthorNameAndBirthDateAndPlaceOfBirthDistance {
		private final String firstName;
		private final String lastName;
		private final LocalDate birthDate;
		private final Double placeOfBirthDistance;

		private MyAuthorNameAndBirthDateAndPlaceOfBirthDistance(String firstName, String lastName,
				LocalDate birthDate, Double placeOfBirthDistance) {
			this.firstName = firstName;
			this.lastName = lastName;
			this.birthDate = birthDate;
			this.placeOfBirthDistance = placeOfBirthDistance;
		}

		@Override
		public String toString() {
			return "MyAuthorNameAndBirthDateAndPlaceOfBirthDistance{" +
					"firstName='" + firstName + '\'' +
					", lastName='" + lastName + '\'' +
					", birthDate=" + birthDate +
					", placeOfBirthDistance=" + placeOfBirthDistance +
					'}';
		}
	}

}
