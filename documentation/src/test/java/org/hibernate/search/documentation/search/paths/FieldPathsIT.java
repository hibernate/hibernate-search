/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.search.paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.List;
import java.util.function.Consumer;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class FieldPathsIT {

	private static final int BOOK1_ID = 1;
	private static final int BOOK2_ID = 2;
	private static final int BOOK3_ID = 3;
	private static final int BOOK4_ID = 4;
	private static final int BOOK5_ID = 5;

	@Rule
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start().setup( Book.class, Author.class );
		initData();
	}

	@Test
	public void root() {
		withinSearchSession( searchSession -> {
			// tag::root[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.match().field( "title" ) // <1>
							.matching( "robot" ) )
					.fetchHits( 20 );
			// end::root[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK3_ID );
		} );
	}

	@Test
	public void nested_implicit() {
		withinSearchSession( searchSession -> {
			// tag::nested_implicit[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.match().field( "writers.firstName" ) // <1>
											.matching( "isaac" ) )
					.fetchHits( 20 );
			// end::nested_implicit[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID, BOOK3_ID );
		} );
	}

	@Test
	public void nested_explicit() {
		withinSearchSession( searchSession -> {
			// tag::nested_explicit[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.nested( "writers" )
							.add( f.match().field( "writers.firstName" ) // <1>
									.matching( "isaac" ) )
							.add( f.match().field( "writers.lastName" )
									.matching( "asimov" ) )
					)
					.fetchHits( 20 );
			// end::nested_explicit[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID, BOOK3_ID );
		} );
	}

	@Test
	public void withRoot() {
		withinSearchSession( searchSession -> {
			// tag::withRoot[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.or()
							.add( f.nested( "writers" )
									.add( matchFirstAndLastName( // <1>
											f.withRoot( "writers" ), // <2>
											"bob", "kane" ) ) )
							.add( f.nested( "artists" )
									.add( matchFirstAndLastName( // <3>
											f.withRoot( "artists" ), // <4>
											"bill", "finger" ) ) ) )
					.fetchHits( 20 );
			// end::withRoot[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK5_ID );
		} );
	}

	// tag::withRoot_method[]
	private SearchPredicate matchFirstAndLastName(SearchPredicateFactory f,
			String firstName, String lastName) {
		return f.and(
						f.match().field( "firstName" ) // <1>
								.matching( firstName ),
						f.match().field( "lastName" )
								.matching( lastName )
				)
				.toPredicate();
	}
	// end::withRoot_method[]

	private void withinSearchSession(Consumer<SearchSession> action) {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			action.accept( searchSession );
		} );
	}

	private void initData() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			Author isaacAsimov = new Author();
			isaacAsimov.setId( 1 );
			isaacAsimov.setFirstName( "Isaac" );
			isaacAsimov.setLastName( "Asimov" );

			Author aLeeMartinez = new Author();
			aLeeMartinez.setId( 2 );
			aLeeMartinez.setFirstName( "A. Lee" );
			aLeeMartinez.setLastName( "Martinez" );

			Author billFinger = new Author();
			billFinger.setId( 3 );
			billFinger.setFirstName( "Bill" );
			billFinger.setLastName( "Finger" );

			Author bobKane = new Author();
			bobKane.setId( 4 );
			bobKane.setFirstName( "Bob" );
			bobKane.setLastName( "Kane" );

			Book book1 = new Book();
			book1.setId( BOOK1_ID );
			book1.setTitle( "I, Robot" );
			book1.getWriters().add( isaacAsimov );
			isaacAsimov.getBooksAsWriter().add( book1 );

			Book book2 = new Book();
			book2.setId( BOOK2_ID );
			book2.setTitle( "The Caves of Steel" );
			book2.getWriters().add( isaacAsimov );
			isaacAsimov.getBooksAsWriter().add( book2 );

			Book book3 = new Book();
			book3.setId( BOOK3_ID );
			book3.setTitle( "The Robots of Dawn" );
			book3.getWriters().add( isaacAsimov );
			isaacAsimov.getBooksAsWriter().add( book3 );

			Book book4 = new Book();
			book4.setId( BOOK4_ID );
			book4.setTitle( "The Automatic Detective" );
			book4.getWriters().add( aLeeMartinez );
			aLeeMartinez.getBooksAsWriter().add( book4 );

			Book book5 = new Book();
			book5.setId( BOOK5_ID );
			book5.setTitle( "Batman" );
			book5.getWriters().add( bobKane );
			bobKane.getBooksAsWriter().add( book5 );
			book5.getArtists().add( billFinger );
			billFinger.getBooksAsArtist().add( book5 );

			entityManager.persist( isaacAsimov );
			entityManager.persist( aLeeMartinez );
			entityManager.persist( bobKane );
			entityManager.persist( billFinger );

			entityManager.persist( book1 );
			entityManager.persist( book2 );
			entityManager.persist( book3 );
			entityManager.persist( book4 );
			entityManager.persist( book5 );
		} );
	}

}
