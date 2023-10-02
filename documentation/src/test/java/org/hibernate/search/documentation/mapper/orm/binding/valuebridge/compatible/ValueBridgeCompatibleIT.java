/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.valuebridge.compatible;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.Arrays;
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

class ValueBridgeCompatibleIT {
	@RegisterExtension
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@BeforeEach
	void setup() {
		entityManagerFactory = setupHelper.start().setup( Book1.class, Book2.class );
	}

	@Test
	void smoke() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			Book1 book1 = new Book1();
			book1.setIsbn( ISBN.parse( "978-0-58-600835-5" ) );
			entityManager.persist( book1 );

			Book2 book2 = new Book2();
			book2.setIsbn( ISBN.parse( "978-8-37-129015-2" ) );
			entityManager.persist( book2 );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			List<Object> result = searchSession.search( Arrays.asList( Book1.class, Book2.class ) )
					.where( f -> f.match().field( "isbn" )
							.matching( ISBN.parse( "978-0-58-600835-5" ) ) )
					.fetchHits( 20 );

			assertThat( result ).hasSize( 1 )
					.allSatisfy( b -> assertThat( b ).isInstanceOf( Book1.class ) );
		} );
	}

}
