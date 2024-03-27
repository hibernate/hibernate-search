/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.valuebridge.param.string;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.time.Year;
import java.util.List;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ValueBridgeParamIT {
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
			Book book1 = new Book();
			book1.setTitle( "The Government Is Evil" );
			book1.setPublished( true );
			book1.getCensorshipAssessments().put( Year.of( 2014 ), false );
			book1.getCensorshipAssessments().put( Year.of( 2020 ), true );
			entityManager.persist( book1 );

			Book book2 = new Book();
			book2.setTitle( "The Government Is Not The Problem, Politicians Are" );
			book2.setPublished( false );
			book2.getCensorshipAssessments().put( Year.of( 2014 ), false );
			book2.getCensorshipAssessments().put( Year.of( 2020 ), false );
			entityManager.persist( book2 );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			List<Book> result = searchSession.search( Book.class )
					.where( f -> f.match().field( "published" )
							.matching( false ) )
					.fetchHits( 20 );

			assertThat( result ).hasSize( 1 );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			List<Book> result = searchSession.search( Book.class )
					.where( f -> f.match().field( "censorshipAssessments_allYears" )
							.matching( true ) )
					.fetchHits( 20 );

			assertThat( result ).hasSize( 1 );
		} );
	}

}
