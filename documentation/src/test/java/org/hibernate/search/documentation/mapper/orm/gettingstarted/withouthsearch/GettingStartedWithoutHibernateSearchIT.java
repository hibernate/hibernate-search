/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.gettingstarted.withouthsearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.documentation.testsupport.TestConfiguration.databaseConnectionProperties;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.TypedQuery;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GettingStartedWithoutHibernateSearchIT {

	private EntityManagerFactory entityManagerFactory;

	@BeforeEach
	void setup() {
		entityManagerFactory = Persistence.createEntityManagerFactory( "GettingStartedWithoutHibernateSearchIT",
				databaseConnectionProperties() );
	}

	@AfterEach
	void cleanup() {
		if ( entityManagerFactory != null ) {
			entityManagerFactory.close();
		}
	}

	@Test
	void test() {
		AtomicReference<Integer> bookIdHolder = new AtomicReference<>();

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			Author author = new Author();
			author.setName( "John Doe" );

			Book book = new Book();
			book.setTitle( "Refactoring: Improving the Design of Existing Code" );
			book.setIsbn( "978-0-58-600835-5" );
			book.setPageCount( 200 );
			book.getAuthors().add( author );
			author.getBooks().add( book );

			entityManager.persist( author );
			entityManager.persist( book );

			bookIdHolder.set( book.getId() );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			TypedQuery<Book> query = entityManager.createQuery( "select b from Book b where title = ?1", Book.class );
			query.setParameter( 1, "Refactoring: Improving the Design of Existing Code" );

			List<Book> result = query.getResultList();

			assertThat( result ).extracting( "id" )
					.containsExactlyInAnyOrder( bookIdHolder.get() );
		} );
	}

}
