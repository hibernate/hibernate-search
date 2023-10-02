/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.indexedembedded.filteredassociation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class FilteredAssociationIT {

	public static List<? extends Arguments> params() {
		return DocumentationSetupHelper.testParamsForBothAnnotationsAndProgrammatic(
				mapping -> {
				// @formatter:off
					//tag::programmatic[]
					TypeMappingStep bookMapping = mapping.type( Book.class );
					bookMapping.indexed();
					bookMapping.property( "title" )
							.fullTextField().analyzer( "english" );
					bookMapping.property( "editionsNotRetired" )
							.indexedEmbedded()
							.associationInverseSide( PojoModelPath.parse( "book" ) )
							.indexingDependency()
									.derivedFrom( PojoModelPath.parse( "editions.status" ) );
					TypeMappingStep bookEditionMapping = mapping.type( BookEdition.class );
					bookEditionMapping.property( "label" )
							.fullTextField().analyzer( "english" );
					//end::programmatic[]
					// @formatter:on
				} );
	}

	@RegisterExtension
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );
	private EntityManagerFactory entityManagerFactory;

	public void init(DocumentationSetupHelper.SetupVariant variant) {
		entityManagerFactory = setupHelper.start( variant )
				.setup( Book.class, BookEdition.class );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void reindexing(DocumentationSetupHelper.SetupVariant variant) {
		init( variant );
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			Book book = new Book();
			book.setId( 0 );
			BookEdition edition1 = new BookEdition();
			edition1.setId( 1 );
			edition1.setLabel( "Mass Market Paperback, 12th Edition" );
			edition1.setStatus( BookEdition.Status.PUBLISHING );
			BookEdition edition2 = new BookEdition();
			edition2.setId( 2 );
			edition2.setLabel( "Kindle Edition" );
			edition2.setStatus( BookEdition.Status.PUBLISHING );

			book.getEditions().add( edition1 );
			edition1.setBook( book );
			book.getEditions().add( edition2 );
			edition2.setBook( book );

			entityManager.persist( book );
			entityManager.persist( edition1 );
			entityManager.persist( edition2 );
		} );
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			assertThat( searchByEditionLabel( entityManager, "paperback" ) )
					.hasSize( 1 );
			assertThat( searchByEditionLabel( entityManager, "kindle" ) )
					.hasSize( 1 );
			assertThat( searchByEditionLabel( entityManager, "extended" ) )
					.hasSize( 0 );
			assertThat( searchByEditionLabel( entityManager, "kobo" ) )
					.hasSize( 0 );
		} );

		// Verify that changing the edition text leads to reindexing
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			BookEdition edition1 = entityManager.find( BookEdition.class, 1 );
			edition1.setLabel( "Kindle Extended Edition" );
		} );
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			assertThat( searchByEditionLabel( entityManager, "extended" ) )
					.hasSize( 1 );
		} );

		// Verify that changing the edition status leads to reindexing
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			BookEdition edition2 = entityManager.find( BookEdition.class, 2 );
			edition2.setStatus( BookEdition.Status.RETIRED );
		} );
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			assertThat( searchByEditionLabel( entityManager, "paperback" ) )
					.hasSize( 0 );
			assertThat( searchByEditionLabel( entityManager, "kindle" ) )
					.hasSize( 1 );
		} );

		// Verify that adding an edition leads to reindexing
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			Book book = entityManager.find( Book.class, 0 );
			BookEdition edition3 = new BookEdition();
			edition3.setId( 3 );
			edition3.setLabel( "Kobo edition" );
			edition3.setStatus( BookEdition.Status.PUBLISHING );

			book.getEditions().add( edition3 );
			edition3.setBook( book );

			entityManager.persist( edition3 );
		} );
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			assertThat( searchByEditionLabel( entityManager, "kindle" ) )
					.hasSize( 1 );
			assertThat( searchByEditionLabel( entityManager, "kobo" ) )
					.hasSize( 1 );
		} );
	}

	private static List<Book> searchByEditionLabel(EntityManager entityManager, String label) {
		return Search.session( entityManager ).search( Book.class )
				.where( f -> f.match().field( "editionsNotRetired.label" ).matching( label ) )
				.fetchHits( 20 );
	}

}
