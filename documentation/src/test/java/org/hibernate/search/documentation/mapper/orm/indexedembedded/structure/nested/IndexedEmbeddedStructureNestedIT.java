/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.indexedembedded.structure.nested;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.List;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class IndexedEmbeddedStructureNestedIT {

	public static List<? extends Arguments> params() {
		return DocumentationSetupHelper.testParamsForBothAnnotationsAndProgrammatic(
				mapping -> {
					TypeMappingStep bookMapping = mapping.type( Book.class );
					bookMapping.indexed();
					bookMapping.property( "title" )
							.fullTextField().analyzer( "english" );
					bookMapping.property( "authors" )
							.indexedEmbedded().structure( ObjectStructure.NESTED );
					TypeMappingStep authorMapping = mapping.type( Author.class );
					authorMapping.property( "firstName" )
							.fullTextField().analyzer( "name" );
					authorMapping.property( "lastName" )
							.fullTextField().analyzer( "name" );
				} );
	}

	@RegisterExtension
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );
	private EntityManagerFactory entityManagerFactory;

	public void init(DocumentationSetupHelper.SetupVariant variant) {
		entityManagerFactory = setupHelper.start( variant )
				.setup( Book.class, Author.class );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void smoke(DocumentationSetupHelper.SetupVariant variant) {
		init( variant );
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			Book book = new Book();
			book.setId( 1 );
			book.setTitle( "Leviathan Wakes" );

			Author author1 = new Author();
			author1.setId( 1 );
			author1.setFirstName( "Daniel" );
			author1.setLastName( "Abraham" );
			book.getAuthors().add( author1 );
			author1.getBooks().add( book );

			Author author2 = new Author();
			author2.setId( 2 );
			author2.setFirstName( "Ty" );
			author2.setLastName( "Frank" );
			book.getAuthors().add( author2 );
			author2.getBooks().add( book );

			entityManager.persist( author1 );
			entityManager.persist( author2 );
			entityManager.persist( book );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			// tag::include[]
			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.nested( "authors" ) // <1>
							.add( f.match().field( "authors.firstName" ).matching( "Ty" ) ) // <2>
							.add( f.match().field( "authors.lastName" ).matching( "Abraham" ) ) ) // <2>
					.fetchHits( 20 );

			assertThat( hits ).isEmpty(); // <3>
			// end::include[]
		} );
	}

}
