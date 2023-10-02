/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.indexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.time.LocalDate;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.session.SearchSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

abstract class AbstractHibernateOrmMassIndexingIT {

	protected static final int NUMBER_OF_BOOKS = 1000;
	private static final int INIT_DATA_TRANSACTION_SIZE = 500;

	@RegisterExtension
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	protected EntityManagerFactory entityManagerFactory;

	@BeforeEach
	public void setup() {
		this.entityManagerFactory = setupHelper.start()
				.withProperty( HibernateOrmMapperSettings.INDEXING_LISTENERS_ENABLED, false )
				.setup( Book.class, Author.class );
		initData( entityManagerFactory );
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			assertThat( searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.fetchTotalHitCount() )
					.isZero();
			assertThat( searchSession.search( Author.class )
					.where( f -> f.matchAll() )
					.fetchTotalHitCount() )
					.isZero();
		} );
	}

	protected void initData(EntityManagerFactory entityManagerFactory) {
		with( entityManagerFactory ).runNoTransaction( entityManager -> {
			try {
				int i = 0;
				while ( i < NUMBER_OF_BOOKS ) {
					entityManager.getTransaction().begin();
					int end = Math.min( i + INIT_DATA_TRANSACTION_SIZE, NUMBER_OF_BOOKS );
					for ( ; i < end; ++i ) {
						Author author = newAuthor( i );

						Book book = newBook( i );
						book.setAuthor( author );
						author.getBooks().add( book );

						entityManager.persist( author );
						entityManager.persist( book );
					}
					entityManager.getTransaction().commit();
				}
			}
			catch (RuntimeException e) {
				entityManager.getTransaction().rollback();
				throw e;
			}
		} );
	}

	private Book newBook(int id) {
		Book book = new Book();
		book.setId( id );
		book.setTitle( "This is the title of book #" + id );
		book.setPublicationYear( 1450 + id );
		return book;
	}

	private Author newAuthor(int id) {
		Author author = new Author();
		author.setId( id );
		author.setFirstName( "John" + id );
		author.setLastName( "Smith" + id );
		author.setBirthDate( LocalDate.ofYearDay( 1450 + id, 33 ) );
		return author;
	}
}
