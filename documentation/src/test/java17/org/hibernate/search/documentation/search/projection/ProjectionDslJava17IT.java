/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.search.projection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.Session;
import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.common.spi.PojoEntityReference;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.mapper.pojo.search.definition.binding.builtin.CompositeProjectionBinder;
import org.hibernate.search.mapper.pojo.search.definition.binding.builtin.DocumentReferenceProjectionBinder;
import org.hibernate.search.mapper.pojo.search.definition.binding.builtin.EntityProjectionBinder;
import org.hibernate.search.mapper.pojo.search.definition.binding.builtin.EntityReferenceProjectionBinder;
import org.hibernate.search.mapper.pojo.search.definition.binding.builtin.FieldProjectionBinder;
import org.hibernate.search.mapper.pojo.search.definition.binding.builtin.HighlightProjectionBinder;
import org.hibernate.search.mapper.pojo.search.definition.binding.builtin.IdProjectionBinder;
import org.hibernate.search.mapper.pojo.search.definition.binding.builtin.ObjectProjectionBinder;
import org.hibernate.search.mapper.pojo.search.definition.binding.builtin.ScoreProjectionBinder;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.impl.integrationtest.common.NormalizationUtils;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ProjectionDslJava17IT {

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
				CollectionHelper.asSet( MyBookProjection.class, MyBookProjection.Author.class, MyAuthorProjection.class,
						MyBookIdAndTitleProjection.class, MyBookTitleAndAuthorNamesProjection.class,
						MyBookTitleAndAuthorsProjection.class,
						MyBookIdAndTitleProjection.class, MyBookTitleAndAuthorNamesProjection.class,
						MyBookScoreAndTitleProjection.class,
						MyBookDocRefAndTitleProjection.class,
						MyBookEntityAndTitleProjection.class,
						MyBookEntityRefAndTitleProjection.class,
						MyBookMiscInfoAndTitleProjection.class, MyBookMiscInfoAndTitleProjection.MiscInfo.class,
						MyBookTitleAndHighlightedDescriptionProjection.class,
						MyBookHighlightedTitleProjection.class ),
				mapping -> {
					var bookMapping = mapping.type( Book.class );
					bookMapping.indexed();
					bookMapping.property( "title" )
							.fullTextField()
							.analyzer( "english" ).projectable( Projectable.YES );
					bookMapping.property( "description" )
							.fullTextField()
							.analyzer( "english" ).projectable( Projectable.YES );
					bookMapping.property( "genre" )
							.keywordField().projectable( Projectable.YES );
					bookMapping.property( "pageCount" )
							.genericField().projectable( Projectable.YES );
					bookMapping.property( "authors" )
							.indexedEmbedded()
							.structure( ObjectStructure.NESTED );
					bookMapping.property( "mainAuthor" )
							.indexedEmbedded()
							.structure( ObjectStructure.NESTED )
							.indexingDependency().derivedFrom( PojoModelPath.ofValue( "authors" ) )
							.associationInverseSide( PojoModelPath.ofValue( "books" ) );
					var authorMapping = mapping.type( Author.class );
					authorMapping.indexed();
					authorMapping.property( "firstName" )
							.fullTextField()
							.analyzer( "name" ).projectable( Projectable.YES );
					authorMapping.property( "lastName" )
							.fullTextField()
							.analyzer( "name" ).projectable( Projectable.YES );

					var myBookProjectionMapping = mapping.type( MyBookProjection.class );
					myBookProjectionMapping.mainConstructor()
							.projectionConstructor();
					myBookProjectionMapping.mainConstructor().parameter( 0 )
							.projection( IdProjectionBinder.create() );
					var myBookAuthorProjectionMapping = mapping.type( MyBookProjection.Author.class );
					myBookAuthorProjectionMapping.mainConstructor()
							.projectionConstructor();
					var myAuthorProjectionMapping = mapping.type( MyAuthorProjection.class );
					myAuthorProjectionMapping.mainConstructor()
							.projectionConstructor();

					//tag::programmatic-id-projection[]
					TypeMappingStep myBookIdAndTitleProjectionMapping =
							mapping.type( MyBookIdAndTitleProjection.class );
					myBookIdAndTitleProjectionMapping.mainConstructor()
							.projectionConstructor();
					myBookIdAndTitleProjectionMapping.mainConstructor().parameter( 0 )
							.projection( IdProjectionBinder.create() );
					//end::programmatic-id-projection[]

					//tag::programmatic-field-projection[]
					TypeMappingStep myBookTitleAndAuthorNamesProjectionMapping =
							mapping.type( MyBookTitleAndAuthorNamesProjection.class );
					myBookTitleAndAuthorNamesProjectionMapping.mainConstructor()
							.projectionConstructor();
					myBookTitleAndAuthorNamesProjectionMapping.mainConstructor().parameter( 0 )
							.projection( FieldProjectionBinder.create() );
					myBookTitleAndAuthorNamesProjectionMapping.mainConstructor().parameter( 1 )
							.projection( FieldProjectionBinder.create( "authors.lastName" ) );
					//end::programmatic-field-projection[]

					//tag::programmatic-score-projection[]
					TypeMappingStep myBookScoreAndTitleProjection =
							mapping.type( MyBookScoreAndTitleProjection.class );
					myBookScoreAndTitleProjection.mainConstructor()
							.projectionConstructor();
					myBookScoreAndTitleProjection.mainConstructor().parameter( 0 )
							.projection( ScoreProjectionBinder.create() );
					//end::programmatic-score-projection[]

					//tag::programmatic-document-reference-projection[]
					TypeMappingStep myBookDocRefAndTitleProjection =
							mapping.type( MyBookDocRefAndTitleProjection.class );
					myBookDocRefAndTitleProjection.mainConstructor()
							.projectionConstructor();
					myBookDocRefAndTitleProjection.mainConstructor().parameter( 0 )
							.projection( DocumentReferenceProjectionBinder.create() );
					//end::programmatic-document-reference-projection[]

					//tag::programmatic-entity-projection[]
					TypeMappingStep myBookEntityAndTitleProjection =
							mapping.type( MyBookEntityAndTitleProjection.class );
					myBookEntityAndTitleProjection.mainConstructor()
							.projectionConstructor();
					myBookEntityAndTitleProjection.mainConstructor().parameter( 0 )
							.projection( EntityProjectionBinder.create() );
					//end::programmatic-entity-projection[]

					//tag::programmatic-entity-reference-projection[]
					TypeMappingStep myBookEntityRefAndTitleProjection =
							mapping.type( MyBookEntityRefAndTitleProjection.class );
					myBookEntityRefAndTitleProjection.mainConstructor()
							.projectionConstructor();
					myBookEntityRefAndTitleProjection.mainConstructor().parameter( 0 )
							.projection( EntityReferenceProjectionBinder.create() );
					//end::programmatic-entity-reference-projection[]

					//tag::programmatic-object-projection[]
					TypeMappingStep myBookTitleAndAuthorsProjection =
							mapping.type( MyBookTitleAndAuthorsProjection.class );
					myBookTitleAndAuthorsProjection.mainConstructor()
							.projectionConstructor();
					myBookTitleAndAuthorsProjection.mainConstructor().parameter( 0 )
							.projection( ObjectProjectionBinder.create() );
					myBookTitleAndAuthorsProjection.mainConstructor().parameter( 1 )
							.projection( ObjectProjectionBinder.create( "mainAuthor" ) );
					//end::programmatic-object-projection[]

					//tag::programmatic-composite-projection[]
					TypeMappingStep myBookMiscInfoAndTitleProjection =
							mapping.type( MyBookMiscInfoAndTitleProjection.class );
					myBookMiscInfoAndTitleProjection.mainConstructor()
							.projectionConstructor();
					myBookMiscInfoAndTitleProjection.mainConstructor().parameter( 0 )
							.projection( CompositeProjectionBinder.create() );
					TypeMappingStep miscInfoProjection =
							mapping.type( MyBookMiscInfoAndTitleProjection.MiscInfo.class );
					miscInfoProjection.mainConstructor().projectionConstructor();
					//end::programmatic-composite-projection[]

					//tag::programmatic-highlight-projection[]
					TypeMappingStep myBookIdAndHighlightedTitleProjection =
							mapping.type( MyBookTitleAndHighlightedDescriptionProjection.class );
					myBookIdAndHighlightedTitleProjection.mainConstructor()
							.projectionConstructor();
					myBookIdAndHighlightedTitleProjection.mainConstructor().parameter( 0 )
							.projection( HighlightProjectionBinder.create() );
					//end::programmatic-highlight-projection[]

					TypeMappingStep myBookHighlightedTitleProjection =
							mapping.type( MyBookHighlightedTitleProjection.class );
					myBookHighlightedTitleProjection.mainConstructor()
							.projectionConstructor();
					myBookHighlightedTitleProjection.mainConstructor().parameter( 0 )
							.projection( HighlightProjectionBinder.create() );
				}
		);
	}

	@RegisterExtension
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	public void init(DocumentationSetupHelper.SetupVariant variant) {
		entityManagerFactory = setupHelper.start( variant )
				.setup( Book.class, Author.class, EmbeddableGeoPoint.class );
		initData();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void entryPoint_mapped_record(DocumentationSetupHelper.SetupVariant variant) {
		init( variant );
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			// tag::entryPoint-mapped-record[]
			List<MyBookProjection> hits = searchSession.search( Book.class )
					.select( MyBookProjection.class )// <1>
					.where( f -> f.matchAll() )
					.fetchHits( 20 ); // <2>
			// end::entryPoint-mapped-record[]
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
	void composite_mapped_record(DocumentationSetupHelper.SetupVariant variant) {
		init( variant );
		withinSearchSession( searchSession -> {
			// tag::composite-mapped-record[]
			List<MyBookProjection> hits = searchSession.search( Book.class )
					.select( f -> f.composite() // <1>
							.as( MyBookProjection.class ) )// <2>
					.where( f -> f.matchAll() )
					.fetchHits( 20 ); // <3>
			// end::composite-mapped-record[]
			Session session = searchSession.toOrmSession();
			assertThat( hits ).containsExactlyInAnyOrderElementsOf(
					session.createQuery( "select b from Book b", Book.class ).list().stream()
							.map( book -> new MyBookProjection(
									book.getId(),
									book.getTitle(),
									book.getAuthors().stream()
											.map( author -> new MyBookProjection.Author( author.getFirstName(),
													author.getLastName() ) )
											.collect( Collectors.toList() )
							) )
							.collect( Collectors.toList() )
			);
		} );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void object_mapped_record(DocumentationSetupHelper.SetupVariant variant) {
		init( variant );
		withinSearchSession( searchSession -> {
			// tag::object-mapped-record[]
			List<List<MyAuthorProjection>> hits = searchSession.search( Book.class )
					.select( f -> f.object( "authors" ) // <1>
							.as( MyAuthorProjection.class ) // <2>
							.multi() ) // <3>
					.where( f -> f.matchAll() )
					.fetchHits( 20 ); // <4>
			// end::object-mapped-record[]
			Session session = searchSession.toOrmSession();
			assertThat( hits ).containsExactlyInAnyOrderElementsOf(
					session.createQuery( "select b from Book b", Book.class ).list().stream()
							.map( book -> book.getAuthors().stream()
									.map( author -> new MyAuthorProjection( author.getFirstName(), author.getLastName() ) )
									.collect( Collectors.toList() ) )
							.collect( Collectors.toList() )
			);
		} );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void projectionConstructor_id(DocumentationSetupHelper.SetupVariant variant) {
		init( variant );
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			// tag::projection-constructor-id[]
			List<MyBookIdAndTitleProjection> hits = searchSession.search( Book.class )
					.select( MyBookIdAndTitleProjection.class )// <1>
					.where( f -> f.matchAll() )
					.fetchHits( 20 ); // <2>
			// end::projection-constructor-id[]
			assertThat( hits ).containsExactlyInAnyOrderElementsOf(
					entityManager.createQuery( "select b from Book b", Book.class ).getResultList().stream()
							.map( book -> new MyBookIdAndTitleProjection(
									book.getId(),
									book.getTitle()
							) )
							.collect( Collectors.toList() )
			);
		} );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void projectionConstructor_field(DocumentationSetupHelper.SetupVariant variant) {
		init( variant );
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			// tag::projection-constructor-field[]
			List<MyBookTitleAndAuthorNamesProjection> hits = searchSession.search( Book.class )
					.select( MyBookTitleAndAuthorNamesProjection.class )// <1>
					.where( f -> f.matchAll() )
					.fetchHits( 20 ); // <2>
			// end::projection-constructor-field[]
			assertThat( hits ).containsExactlyInAnyOrderElementsOf(
					entityManager.createQuery( "select b from Book b", Book.class ).getResultList().stream()
							.map( book -> new MyBookTitleAndAuthorNamesProjection(
									book.getTitle(),
									book.getAuthors().stream()
											.map( Author::getLastName )
											.collect( Collectors.toList() )
							) )
							.collect( Collectors.toList() )
			);
		} );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void projectionConstructor_score(DocumentationSetupHelper.SetupVariant variant) {
		init( variant );
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			// tag::projection-constructor-score[]
			List<MyBookScoreAndTitleProjection> hits = searchSession.search( Book.class )
					.select( MyBookScoreAndTitleProjection.class )// <1>
					.where( f -> f.matchAll() )
					.fetchHits( 20 ); // <2>
			// end::projection-constructor-score[]
			assertThat( hits )
					// Ignore scores, we can't guess their value
					.usingRecursiveFieldByFieldElementComparatorIgnoringFields( "score" )
					.containsExactlyInAnyOrderElementsOf(
							entityManager.createQuery( "select b from Book b", Book.class ).getResultList().stream()
									.map( book -> new MyBookScoreAndTitleProjection(
											0f,
											book.getTitle()
									) )
									.collect( Collectors.toList() )
					);
		} );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void projectionConstructor_documentReference(DocumentationSetupHelper.SetupVariant variant) {
		init( variant );
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			// tag::projection-constructor-document-reference[]
			List<MyBookDocRefAndTitleProjection> hits = searchSession.search( Book.class )
					.select( MyBookDocRefAndTitleProjection.class )// <1>
					.where( f -> f.matchAll() )
					.fetchHits( 20 ); // <2>
			// end::projection-constructor-document-reference[]
			assertThat( hits )
					.usingRecursiveFieldByFieldElementComparator()
					.containsExactlyInAnyOrderElementsOf(
							entityManager.createQuery( "select b from Book b", Book.class ).getResultList().stream()
									.map( book -> new MyBookDocRefAndTitleProjection(
											NormalizationUtils.reference( Book.NAME, book.getId().toString() ),
											book.getTitle()
									) )
									.collect( Collectors.toList() )
					);
		} );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void projectionConstructor_entity(DocumentationSetupHelper.SetupVariant variant) {
		init( variant );
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			// tag::projection-constructor-entity[]
			List<MyBookEntityAndTitleProjection> hits = searchSession.search( Book.class )
					.select( MyBookEntityAndTitleProjection.class )// <1>
					.where( f -> f.matchAll() )
					.fetchHits( 20 ); // <2>
			// end::projection-constructor-entity[]
			assertThat( hits )
					.usingRecursiveFieldByFieldElementComparator()
					.containsExactlyInAnyOrderElementsOf(
							entityManager.createQuery( "select b from Book b", Book.class ).getResultList().stream()
									.map( book -> new MyBookEntityAndTitleProjection(
											book,
											book.getTitle()
									) )
									.collect( Collectors.toList() )
					);
		} );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void projectionConstructor_entityReference(DocumentationSetupHelper.SetupVariant variant) {
		init( variant );
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			// tag::projection-constructor-entity-reference[]
			List<MyBookEntityRefAndTitleProjection> hits = searchSession.search( Book.class )
					.select( MyBookEntityRefAndTitleProjection.class )// <1>
					.where( f -> f.matchAll() )
					.fetchHits( 20 ); // <2>
			// end::projection-constructor-entity-reference[]
			assertThat( hits )
					.containsExactlyInAnyOrderElementsOf(
							entityManager.createQuery( "select b from Book b", Book.class ).getResultList().stream()
									.map( book -> new MyBookEntityRefAndTitleProjection(
											PojoEntityReference.withName( Book.class, Book.NAME, book.getId() ),
											book.getTitle()
									) )
									.collect( Collectors.toList() )
					);
		} );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void projectionConstructor_object(DocumentationSetupHelper.SetupVariant variant) {
		init( variant );
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			// tag::projection-constructor-object[]
			List<MyBookTitleAndAuthorsProjection> hits = searchSession.search( Book.class )
					.select( MyBookTitleAndAuthorsProjection.class )// <1>
					.where( f -> f.matchAll() )
					.fetchHits( 20 ); // <2>
			// end::projection-constructor-object[]
			assertThat( hits ).containsExactlyInAnyOrderElementsOf(
					entityManager.createQuery( "select b from Book b", Book.class ).getResultList().stream()
							.map( book -> new MyBookTitleAndAuthorsProjection(
									book.getAuthors().stream()
											.map( a -> new MyAuthorProjection( a.getFirstName(), a.getLastName() ) )
											.collect( Collectors.toList() ),
									new MyAuthorProjection( book.getMainAuthor().getFirstName(),
											book.getMainAuthor().getLastName() ),
									book.getTitle()
							) )
							.collect( Collectors.toList() )
			);
		} );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void projectionConstructor_composite(DocumentationSetupHelper.SetupVariant variant) {
		init( variant );
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			// tag::projection-constructor-composite[]
			List<MyBookMiscInfoAndTitleProjection> hits = searchSession.search( Book.class )
					.select( MyBookMiscInfoAndTitleProjection.class )// <1>
					.where( f -> f.matchAll() )
					.fetchHits( 20 ); // <2>
			// end::projection-constructor-composite[]
			assertThat( hits ).containsExactlyInAnyOrderElementsOf(
					entityManager.createQuery( "select b from Book b", Book.class ).getResultList().stream()
							.map( book -> new MyBookMiscInfoAndTitleProjection(
									new MyBookMiscInfoAndTitleProjection.MiscInfo( book.getGenre(), book.getPageCount() ),
									book.getTitle()
							) )
							.collect( Collectors.toList() )
			);
		} );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void projectionConstructor_highlight(DocumentationSetupHelper.SetupVariant variant) {
		init( variant );
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			// tag::projection-constructor-highlight[]
			List<MyBookTitleAndHighlightedDescriptionProjection> hits = searchSession.search( Book.class )
					.select( MyBookTitleAndHighlightedDescriptionProjection.class )// <1>
					.where( f -> f.match().field( "description" ).matching( "self-aware" ) )
					.fetchHits( 20 ); // <2>
			// end::projection-constructor-highlight[]
			assertThat( hits ).containsExactlyInAnyOrder(
					new MyBookTitleAndHighlightedDescriptionProjection(
							List.of( "A robot becomes <em>self</em>-<em>aware</em>." ),
							"I, Robot"
					)
			);
		} );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void projectionConstructor_highlightSingle(DocumentationSetupHelper.SetupVariant variant) {
		init( variant );
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			// tag::projection-constructor-highlight-single[]
			List<MyBookHighlightedTitleProjection> hits = searchSession.search( Book.class )
					.select( MyBookHighlightedTitleProjection.class )// <1>
					.where( f -> f.match().field( "title" ).matching( "robot" ) )
					.highlighter( f -> f.unified().numberOfFragments( 1 ) ) // <2>
					.fetchHits( 20 ); // <3>
			// end::projection-constructor-highlight-single[]
			assertThat( hits ).containsExactlyInAnyOrder(
					new MyBookHighlightedTitleProjection(
							"I, <em>Robot</em>",
							"A robot becomes self-aware."
					),
					new MyBookHighlightedTitleProjection(
							"The <em>Robots</em> of Dawn",
							"A crime story about the first \"roboticide\"."
					)
			);
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
			isaacAsimov.setBirthDate( LocalDate.of( 1920, Month.JANUARY, 2 ) );
			isaacAsimov.setPlaceOfBirth( EmbeddableGeoPoint.of( 53.976177, 32.158627 ) );

			Author aLeeMartinez = new Author();
			aLeeMartinez.setId( MARTINEZ_ID );
			aLeeMartinez.setFirstName( "A. Lee" );
			aLeeMartinez.setLastName( "Martinez" );
			aLeeMartinez.setBirthDate( LocalDate.of( 1973, Month.JANUARY, 12 ) );
			aLeeMartinez.setPlaceOfBirth( EmbeddableGeoPoint.of( 31.814315, -106.475524 ) );

			Book book1 = new Book();
			book1.setId( BOOK1_ID );
			book1.setTitle( "I, Robot" );
			book1.setDescription( "A robot becomes self-aware." );
			book1.setPageCount( 250 );
			book1.setGenre( Genre.SCIENCE_FICTION );
			book1.getAuthors().add( isaacAsimov );
			isaacAsimov.getBooks().add( book1 );

			Book book2 = new Book();
			book2.setId( BOOK2_ID );
			book2.setTitle( "The Caves of Steel" );
			book2.setDescription( "A robot helps investigate a murder on an extrasolar colony." );
			book2.setPageCount( 206 );
			book2.setGenre( Genre.SCIENCE_FICTION );
			book2.getAuthors().add( isaacAsimov );
			isaacAsimov.getBooks().add( book2 );

			Book book3 = new Book();
			book3.setId( BOOK3_ID );
			book3.setTitle( "The Robots of Dawn" );
			book3.setDescription( "A crime story about the first \"roboticide\"." );
			book3.setPageCount( 435 );
			book3.setGenre( Genre.SCIENCE_FICTION );
			book3.getAuthors().add( isaacAsimov );
			isaacAsimov.getBooks().add( book3 );

			Book book4 = new Book();
			book4.setId( BOOK4_ID );
			book4.setTitle( "The Automatic Detective" );
			book4.setDescription( "A robot cab driver turns PI after the disappearance of a neighboring family." );
			book4.setPageCount( 222 );
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
