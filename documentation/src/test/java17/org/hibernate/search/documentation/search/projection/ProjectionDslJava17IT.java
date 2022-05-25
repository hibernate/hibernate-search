/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.search.projection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.persistence.EntityManagerFactory;

import org.hibernate.Session;
import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.common.impl.CollectionHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ProjectionDslJava17IT {

	private static final int ASIMOV_ID = 1;
	private static final int MARTINEZ_ID = 2;

	private static final int BOOK1_ID = 1;
	private static final int BOOK2_ID = 2;
	private static final int BOOK3_ID = 3;
	private static final int BOOK4_ID = 4;

	@Rule
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend(
			BackendConfigurations.simple(), true,
			// Since we disable classpath scanning in tests for performance reasons,
			// we need to register annotated projection types explicitly.
			// This wouldn't be needed in a typical application.
			context -> context.annotationMapping().add( CollectionHelper.asSet(
					MyBookProjection.class, MyBookProjection.Author.class, MyAuthorProjection.class ) ) );

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start().setup( Book.class, Author.class, EmbeddableGeoPoint.class );
		initData();
	}

	@Test
	public void entryPoint_mapped_record() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			// tag::entryPoint-mapped-record[]
			SearchSession searchSession = Search.session( entityManager );

			List<MyBookProjection> hits = searchSession.search( Book.class )
					.select( MyBookProjection.class )// <1>
					.where( f -> f.matchAll() )
					.fetchHits( 20 ); // <2>
			// end::entryPoint-mapped-record[]
			assertThat( hits ).containsExactlyInAnyOrderElementsOf(
					entityManager.createQuery( "select b from Book b", Book.class ).getResultList().stream()
							.map( book -> new MyBookProjection(
									book.getTitle(),
									book.getAuthors().stream()
											.map( author -> new MyBookProjection.Author(
													author.getFirstName(), author.getLastName() ) )
											.collect( Collectors.toList() )
							) )
							.collect( Collectors.toList() )
			);
		} );
	}

	@Test
	public void composite_mapped_record() {
		withinSearchSession( searchSession -> {
			// tag::composite-mapped-record[]
			List<MyBookProjection> hits = searchSession.search( Book.class )
					.select( f -> f.composite() // <1>
							.as( MyBookProjection.class ) )// <2>
					.where( f -> f.matchAll() )
					.fetchHits( 20 ); // <3>
			// end::composite-mapped-record[]
			Session session = searchSession.toOrmSession();
			assertThat( hits ).containsExactlyInAnyOrderElementsOf(
					session.createQuery( "select b from Book b", Book.class ).list().stream()
							.map( book -> new MyBookProjection(
									book.getTitle(),
									book.getAuthors().stream()
											.map( author -> new MyBookProjection.Author( author.getFirstName(), author.getLastName() ) )
											.collect( Collectors.toList() )
							) )
							.collect( Collectors.toList() )
			);
		} );
	}

	@Test
	public void object_mapped_record() {
		withinSearchSession( searchSession -> {
			// tag::object-mapped-record[]
			List<List<MyAuthorProjection>> hits = searchSession.search( Book.class )
					.select( f -> f.object( "authors" ) // <1>
							.as( MyAuthorProjection.class ) // <2>
							.multi() ) // <3>
					.where( f -> f.matchAll() )
					.fetchHits( 20 ); // <4>
			// end::object-mapped-record[]
			Session session = searchSession.toOrmSession();
			assertThat( hits ).containsExactlyInAnyOrderElementsOf(
					session.createQuery( "select b from Book b", Book.class ).list().stream()
							.map( book -> book.getAuthors().stream()
									.map( author -> new MyAuthorProjection( author.getFirstName(), author.getLastName() ) )
									.collect( Collectors.toList() ) )
							.collect( Collectors.toList() )
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
			book1.getAuthors().add( isaacAsimov );
			isaacAsimov.getBooks().add( book1 );

			Book book2 = new Book();
			book2.setId( BOOK2_ID );
			book2.setTitle( "The Caves of Steel" );
			book2.setDescription( "A robot helps investigate a murder on an extrasolar colony." );
			book2.setPageCount( 206 );
			book2.setGenre( Genre.SCIENCE_FICTION );
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

}
