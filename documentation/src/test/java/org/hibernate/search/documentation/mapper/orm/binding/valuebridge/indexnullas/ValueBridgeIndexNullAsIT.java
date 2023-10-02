/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.valuebridge.indexnullas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.List;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.documentation.testsupport.data.ISBN;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ValueBridgeIndexNullAsIT {
	@RegisterExtension
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@BeforeEach
	void setup() {
		entityManagerFactory = setupHelper.start().setup( Book.class );
	}

	@Test
	void smoke() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			Book book = new Book();
			book.setIsbn( null );
			entityManager.persist( book );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			List<Book> result = searchSession.search( Book.class )
					.where( f -> f.match().field( "isbn" )
							.matching( ISBN.parse( "000-0-00-000000-0" ) ) )
					.fetchHits( 20 );

			assertThat( result ).hasSize( 1 );
		} );
	}

	@Test
	void queryString() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			Book book = new Book();
			book.setIsbn( ISBN.parse( "978-0-13-468599-1" ) );
			entityManager.persist( book );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			//tag::string-query[]
			List<Book> result = searchSession.search( Book.class )
					.where( f -> f.queryString().field( "isbn" )
							.matching( "978-0-13-468599-1" ) ) // <1>
					.fetchHits( 20 );
			//end::string-query[]
			assertThat( result ).hasSize( 1 );
		} );
	}
}
