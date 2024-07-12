/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.search.projection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.List;
import java.util.function.Consumer;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.backend.lucene.search.projection.dsl.DocumentTree;
import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Explanation;

class LuceneProjectionDslIT {

	private static final int ASIMOV_ID = 1;
	private static final int MARTINEZ_ID = 2;

	private static final String BOOK_INDEX_NAME = Book.class.getSimpleName();
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
	void document() {
		withinSearchSession( searchSession -> {
			// tag::lucene-document[]
			List<Document> hits = searchSession.search( Book.class )
					.extension( LuceneExtension.get() )
					.select( f -> f.document() )
					.where( f -> f.matchAll() )
					.fetchHits( 20 );
			// end::lucene-document[]
			assertThat( hits ).hasSize( 4 );
		} );
	}

	@Test
	void documentTree() {
		withinSearchSession( searchSession -> {
			// tag::lucene-document-tree[]
			List<DocumentTree> hits = searchSession.search( Book.class )
					.extension( LuceneExtension.get() )
					.select( f -> f.documentTree() )
					.where( f -> f.matchAll() )
					.fetchHits( 20 );
			// end::lucene-document-tree[]
			assertThat( hits ).hasSize( 4 );
		} );
	}

	@Test
	void explanation() {
		withinSearchSession( searchSession -> {
			// tag::lucene-explanation[]
			List<Explanation> hits = searchSession.search( Book.class )
					.extension( LuceneExtension.get() )
					.select( f -> f.explanation() )
					.where( f -> f.matchAll() )
					.fetchHits( 20 );
			// end::lucene-explanation[]
			assertThat( hits ).hasSize( 4 );
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
			book1.setGenre( Genre.SCIENCE_FICTION );
			book1.getAuthors().add( isaacAsimov );
			isaacAsimov.getBooks().add( book1 );

			Book book2 = new Book();
			book2.setId( BOOK2_ID );
			book2.setTitle( "The Caves of Steel" );
			book2.setGenre( Genre.SCIENCE_FICTION );
			book2.getAuthors().add( isaacAsimov );
			isaacAsimov.getBooks().add( book2 );

			Book book3 = new Book();
			book3.setId( BOOK3_ID );
			book3.setTitle( "The Robots of Dawn" );
			book3.setGenre( Genre.SCIENCE_FICTION );
			book3.getAuthors().add( isaacAsimov );
			isaacAsimov.getBooks().add( book3 );

			Book book4 = new Book();
			book4.setId( BOOK4_ID );
			book4.setTitle( "The Automatic Detective" );
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
