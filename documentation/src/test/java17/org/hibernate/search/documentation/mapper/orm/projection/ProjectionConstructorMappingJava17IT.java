/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.projection;


import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.mapper.pojo.search.definition.binding.builtin.IdProjectionBinder;
import org.hibernate.search.util.common.impl.CollectionHelper;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test that the simple mapping defined in Book works as expected.
 */
class ProjectionConstructorMappingJava17IT {

	private static final int ASIMOV_ID = 1;
	private static final int MARTINEZ_ID = 2;

	private static final int BOOK1_ID = 1;
	private static final int BOOK2_ID = 2;
	private static final int BOOK3_ID = 3;
	private static final int BOOK4_ID = 4;

	public static List<? extends Arguments> params() {
		return DocumentationSetupHelper.testParamsForBothAnnotationsAndProgrammatic(
				// Since we disable classpath scanning in tests for performance reasons,
				// we need to register annotated projection types explicitly.
				// This wouldn't be needed in a typical application.
				CollectionHelper.asSet( MyBookProjection.class, MyBookProjection.Author.class,
						MyAuthorProjectionClassMultiConstructor.class, MyAuthorProjectionRecordMultiConstructor.class
				),
				mapping -> {
					TypeMappingStep bookMapping = mapping.type( Book.class );
					bookMapping.indexed();
					bookMapping.property( "title" )
							.fullTextField()
							.analyzer( "english" ).projectable( Projectable.YES );
					bookMapping.property( "description" )
							.fullTextField()
							.analyzer( "english" ).projectable( Projectable.YES );
					bookMapping.property( "authors" )
							.indexedEmbedded()
							.structure( ObjectStructure.NESTED );
					TypeMappingStep authorMapping = mapping.type( Author.class );
					authorMapping.indexed();
					authorMapping.property( "firstName" )
							.fullTextField()
							.analyzer( "name" ).projectable( Projectable.YES );
					authorMapping.property( "lastName" )
							.fullTextField()
							.analyzer( "name" ).projectable( Projectable.YES );

					//tag::programmatic-mainConstructor[]
					TypeMappingStep myBookProjectionMapping = mapping.type( MyBookProjection.class );
					myBookProjectionMapping.mainConstructor()
							.projectionConstructor(); // <1>
					myBookProjectionMapping.mainConstructor().parameter( 0 )
							.projection( IdProjectionBinder.create() ); // <2>
					TypeMappingStep myAuthorProjectionMapping = mapping.type( MyBookProjection.Author.class );
					myAuthorProjectionMapping.mainConstructor()
							.projectionConstructor();
					//end::programmatic-mainConstructor[]
					//tag::programmatic-constructor[]
					mapping.type( MyAuthorProjectionClassMultiConstructor.class )
							.constructor( String.class, String.class )
							.projectionConstructor();
					//end::programmatic-constructor[]
					mapping.type( MyAuthorProjectionRecordMultiConstructor.class )
							.constructor( String.class, String.class )
							.projectionConstructor();
				}
		);
	}

	@RegisterExtension
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );
	private EntityManagerFactory entityManagerFactory;

	public void init(DocumentationSetupHelper.SetupVariant variant) {
		entityManagerFactory = setupHelper.start( variant )
				.setup( Book.class, Author.class );
		initData();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void simple(DocumentationSetupHelper.SetupVariant variant) {
		init( variant );
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			// tag::projection-mapped[]
			List<MyBookProjection> hits = searchSession.search( Book.class )
					.select( MyBookProjection.class ) // <1>
					.where( f -> f.matchAll() )
					.fetchHits( 20 ); // <2>
			// end::projection-mapped[]

			assertThat( hits ).containsExactlyInAnyOrderElementsOf(
					entityManager.createQuery( "select b from Book b", Book.class ).getResultList().stream()
							.map( book -> new MyBookProjection(
									book.getId(),
									book.getTitle(),
									book.getAuthors().stream()
											.map( author -> new MyBookProjection.Author(
													author.getFirstName(), author.getLastName() ) )
											.collect( Collectors.toList() )
							) )
							.collect( Collectors.toList() )
			);
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			// tag::projection-programmatic[]
			List<MyBookProjection> hits = searchSession.search( Book.class )
					.select( f -> f.composite()
							.from(
									f.id( Integer.class ),
									f.field( "title", String.class ),
									f.object( "authors" )
											.from(
													f.field( "authors.firstName", String.class ),
													f.field( "authors.lastName", String.class )
											)
											.as( MyBookProjection.Author::new )
											.multi()
							)
							.as( MyBookProjection::new ) )
					.where( f -> f.matchAll() )
					.fetchHits( 20 );
			// end::projection-programmatic[]

			assertThat( hits ).containsExactlyInAnyOrderElementsOf(
					entityManager.createQuery( "select b from Book b", Book.class ).getResultList().stream()
							.map( book -> new MyBookProjection(
									book.getId(),
									book.getTitle(),
									book.getAuthors().stream()
											.map( author -> new MyBookProjection.Author(
													author.getFirstName(), author.getLastName() ) )
											.collect( Collectors.toList() )
							) )
							.collect( Collectors.toList() )
			);
		} );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void multiConstructor_class(DocumentationSetupHelper.SetupVariant variant) {
		init( variant );
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			List<MyAuthorProjectionClassMultiConstructor> hits = searchSession.search( Author.class )
					.select( MyAuthorProjectionClassMultiConstructor.class )
					.where( f -> f.matchAll() )
					.fetchHits( 20 );

			assertThat( hits ).containsExactlyInAnyOrderElementsOf(
					entityManager.createQuery( "select a from Author a", Author.class ).getResultList().stream()
							.map( author -> new MyAuthorProjectionClassMultiConstructor(
									author.getFirstName(), author.getLastName()
							) )
							.collect( Collectors.toList() )
			);
		} );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void multiConstructor_record(DocumentationSetupHelper.SetupVariant variant) {
		init( variant );
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			List<MyAuthorProjectionRecordMultiConstructor> hits = searchSession.search( Author.class )
					.select( MyAuthorProjectionRecordMultiConstructor.class )
					.where( f -> f.matchAll() )
					.fetchHits( 20 );

			assertThat( hits ).containsExactlyInAnyOrderElementsOf(
					entityManager.createQuery( "select a from Author a", Author.class ).getResultList().stream()
							.map( author -> new MyAuthorProjectionRecordMultiConstructor(
									author.getFirstName(), author.getLastName()
							) )
							.collect( Collectors.toList() )
			);
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
			book1.getAuthors().add( isaacAsimov );
			isaacAsimov.getBooks().add( book1 );

			Book book2 = new Book();
			book2.setId( BOOK2_ID );
			book2.setTitle( "The Caves of Steel" );
			book2.setDescription( "A robot helps investigate a murder on an extrasolar colony." );
			book2.getAuthors().add( isaacAsimov );
			isaacAsimov.getBooks().add( book2 );

			Book book3 = new Book();
			book3.setId( BOOK3_ID );
			book3.setTitle( "The Robots of Dawn" );
			book3.setDescription( "A crime story about the first \"roboticide\"." );
			book3.getAuthors().add( isaacAsimov );
			isaacAsimov.getBooks().add( book3 );

			Book book4 = new Book();
			book4.setId( BOOK4_ID );
			book4.setTitle( "The Automatic Detective" );
			book4.setDescription( "A robot cab driver turns PI after the disappearance of a neighboring family." );
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
