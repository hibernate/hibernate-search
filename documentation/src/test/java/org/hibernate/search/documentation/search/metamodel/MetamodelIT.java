/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.search.metamodel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.search.predicate.EmbeddableGeoPoint;
import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.hibernate.search.mapper.orm.session.SearchSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class MetamodelIT {

	private static final int ASIMOV_ID = 1;
	private static final int MARTINEZ_ID = 2;

	private static final int BOOK1_ID = 1;
	private static final int BOOK2_ID = 2;
	private static final int BOOK3_ID = 3;
	private static final int BOOK4_ID = 4;

	@RegisterExtension
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@BeforeEach
	void setup() {
		entityManagerFactory = setupHelper.start().setup( Book.class, Author.class, EmbeddableGeoPoint.class );

		DocumentationSetupHelper.SetupContext setupContext = setupHelper.start();
		entityManagerFactory = setupContext.setup( Book.class, Author.class, EmbeddableGeoPoint.class );
		initData();
	}

	@Test
	void entryPoint() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			// tag::entryPoint[]
			SearchSession searchSession = /* ... */ // <1>
					// end::entryPoint[]
					Search.session( entityManager );
			// tag::entryPoint[]
			SearchScope<Book__, Book> scope = Book__.INDEX.scope( searchSession ); // <2>

			List<Book> hits = searchSession.search( scope )
					.where( f -> f.match()
							.field( Book__.INDEX.title ).field( Book__.INDEX.description ) // <3>
							.matching( "robot" ) )
					.fetchHits( 20 );
			// end::entryPoint[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK1_ID, BOOK2_ID, BOOK3_ID, BOOK4_ID );
		} );
	}

	@Test
	void compileCheck() {
		withinSearchSession( searchSession -> {
			SearchScope<Book__, Book> scope = Book__.INDEX.scope( searchSession );
			// tag::compileCheck-pass[]
			List<String> titles = searchSession.search( scope )
					.select( f -> f.field( Book__.INDEX.title ) ) // <1>
					.where( f -> f.matchAll() )
					.fetchHits( 20 );
			// end::compileCheck-pass[]
			assertThat( titles )
					.hasSize( 4 );
		} );
	}

	@Test
	void valueModel() {
		withinSearchSession( searchSession -> {
			SearchScope<Book__, Book> scope = Book__.INDEX.scope( searchSession );
			// tag::valueModel[]
			List<Book> hits = searchSession.search( scope )
					.where( f -> f.match()
							.field( Book__.INDEX.genre.string() ) // <1>
							.matching( "CRIME_FICTION" ) ) // <2>
					.fetchHits( 20 );
			// end::valueModel[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactlyInAnyOrder( BOOK4_ID );
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

			Author aLeeMartinez = new Author();
			aLeeMartinez.setId( MARTINEZ_ID );
			aLeeMartinez.setFirstName( "A. Lee" );
			aLeeMartinez.setLastName( "Martinez" );

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

	private static float[] floats(int dimension, float value) {
		float[] bytes = new float[dimension];
		Arrays.fill( bytes, value );
		return bytes;
	}
}
