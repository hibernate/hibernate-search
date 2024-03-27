/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.search.sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.List;
import java.util.function.Consumer;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortedSetSortField;

class LuceneSortDslIT {

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
		initData();
	}

	@Test
	void fromLucene() {
		withinSearchSession( searchSession -> {
			// tag::lucene-fromLuceneSort[]
			List<Book> hits = searchSession.search( Book.class )
					.extension( LuceneExtension.get() )
					.where( f -> f.matchAll() )
					.sort( f -> f.fromLuceneSort(
							new Sort(
									new SortedSetSortField( "genre_sort", false ),
									new SortedSetSortField( "title_sort", false )
							)
					) )
					.fetchHits( 20 );
			// end::lucene-fromLuceneSort[]

			assertThat( hits ).extracting( Book::getId )
					.containsExactly( BOOK4_ID, BOOK1_ID, BOOK2_ID, BOOK3_ID );
		} );

		withinSearchSession( searchSession -> {
			// tag::lucene-fromLuceneSortField[]
			List<Book> hits = searchSession.search( Book.class )
					.extension( LuceneExtension.get() )
					.where( f -> f.matchAll() )
					.sort( f -> f.fromLuceneSortField(
							new SortedSetSortField( "title_sort", false )
					) )
					.fetchHits( 20 );
			// end::lucene-fromLuceneSortField[]
			assertThat( hits )
					.extracting( Book::getId )
					.containsExactly( BOOK1_ID, BOOK4_ID, BOOK2_ID, BOOK3_ID );
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

}
