/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FieldProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IdProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.extension.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@TestForIssue(jiraKey = "HSEARCH-3927")
class ProjectionConstructorBaseIT extends AbstractProjectionConstructorIT {

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Test
	void typeLevelAnnotation() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public Integer integer;
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( TypeLevelAnnotationMyProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjectionExecutionOnly(
				mapping, IndexedEntity.class, TypeLevelAnnotationMyProjection.class,
				Arrays.asList(
						Arrays.asList( "result1", 1 ),
						Arrays.asList( "result2", 2 ),
						Arrays.asList( "result3", 3 )
				),
				Arrays.asList(
						new TypeLevelAnnotationMyProjection( "result1", 1 ),
						new TypeLevelAnnotationMyProjection( "result2", 2 ),
						new TypeLevelAnnotationMyProjection( "result3", 3 )
				)
		);
	}

	@ProjectionConstructor
	static class TypeLevelAnnotationMyProjection {
		public final String text;
		public final Integer integer;

		public TypeLevelAnnotationMyProjection(String text, Integer integer) {
			this.text = text;
			this.integer = integer;
		}
	}

	@Test
	void typeLevelAnnotation_multipleConstructors() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public Integer integer;
		}

		assertThatThrownBy( () -> setupHelper.start()
				.withAnnotatedTypes( TypeLevelAnnotation_multipleConstructorsMyProjection.class )
				.setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( TypeLevelAnnotation_multipleConstructorsMyProjection.class.getName() )
						.annotationContextAnyParameters( ProjectionConstructor.class )
						.failure( "No main constructor for type",
								TypeLevelAnnotation_multipleConstructorsMyProjection.class.getName(),
								"this type does not declare exactly one constructor" ) );
	}

	@ProjectionConstructor
	static class TypeLevelAnnotation_multipleConstructorsMyProjection {
		public final String text;
		public final Integer integer;

		public TypeLevelAnnotation_multipleConstructorsMyProjection(String text, Integer integer) {
			this.text = text;
			this.integer = integer;
		}

		public TypeLevelAnnotation_multipleConstructorsMyProjection(String text, Integer integer, String somethingElse) {
			this.text = text;
			this.integer = integer;
		}
	}

	@Test
	void constructorLevelAnnotation() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public Integer integer;
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( ConstructorLevelAnnotationMyProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjectionExecutionOnly(
				mapping, IndexedEntity.class, ConstructorLevelAnnotationMyProjection.class,
				Arrays.asList(
						Arrays.asList( "result1", 1 ),
						Arrays.asList( "result2", 2 ),
						Arrays.asList( "result3", 3 )
				),
				Arrays.asList(
						new ConstructorLevelAnnotationMyProjection( "result1", 1 ),
						new ConstructorLevelAnnotationMyProjection( "result2", 2 ),
						new ConstructorLevelAnnotationMyProjection( "result3", 3 )
				)
		);
	}

	static class ConstructorLevelAnnotationMyProjection {
		public final String text;
		public final Integer integer;

		public ConstructorLevelAnnotationMyProjection() {
			this.text = "foo";
			this.integer = 42;
		}

		@ProjectionConstructor
		public ConstructorLevelAnnotationMyProjection(String text, Integer integer) {
			this.text = text;
			this.integer = integer;
		}

		public ConstructorLevelAnnotationMyProjection(String text, Integer integer, String somethingElse) {
			this.text = text;
			this.integer = integer;
		}
	}

	@Test
	void abstractType() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public Integer integer;
		}

		assertThatThrownBy( () -> setupHelper.start()
				.withAnnotatedTypes( AbstractTypeMyAbstractProjection.class )
				.setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( AbstractTypeMyAbstractProjection.class.getName() )
						.constructorContext( String.class, Integer.class )
						.failure( "Invalid declaring type for projection constructor",
								AbstractTypeMyAbstractProjection.class.getName(), "is abstract",
								"Projection constructors can only be declared on concrete types" ) );
	}

	abstract static class AbstractTypeMyAbstractProjection {
		public final String text;
		public final Integer integer;

		@ProjectionConstructor
		public AbstractTypeMyAbstractProjection(String text, Integer integer) {
			this.text = text;
			this.integer = integer;
		}

		public AbstractTypeMyAbstractProjection(String text, Integer integer, String somethingElse) {
			this.text = text;
			this.integer = integer;
		}
	}

	@Test
	void entityAndProjection() {
		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start().setup( EntityAndProjectionIndexedEntity.class );

		testSuccessfulRootProjectionExecutionOnly(
				mapping, EntityAndProjectionIndexedEntity.class, EntityAndProjectionIndexedEntity.class,
				Arrays.asList(
						Arrays.asList( "result1", 1 ),
						Arrays.asList( "result2", 2 ),
						Arrays.asList( "result3", 3 )
				),
				Arrays.asList(
						new EntityAndProjectionIndexedEntity( "result1", 1 ),
						new EntityAndProjectionIndexedEntity( "result2", 2 ),
						new EntityAndProjectionIndexedEntity( "result3", 3 )
				)
		);
	}

	@Indexed(index = INDEX_NAME)
	static class EntityAndProjectionIndexedEntity {
		@DocumentId
		public Integer id;
		@FullTextField
		public String text;
		@GenericField
		public Integer integer;

		public EntityAndProjectionIndexedEntity() {
		}

		@ProjectionConstructor
		public EntityAndProjectionIndexedEntity(String text, Integer integer) {
			this.text = text;
			this.integer = integer;
		}
	}

	@Test
	void noArgConstructor() {
		assumeTrue(
				Runtime.version().feature() < 25,
				"With JDK 25+ nonstatic (inner class) projections are not supported."
		);
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public Integer integer;
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( NoArgConstructorMyProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjectionExecutionOnly(
				mapping, IndexedEntity.class, NoArgConstructorMyProjection.class,
				Arrays.asList(
						Arrays.asList(),
						Arrays.asList(),
						Arrays.asList()
				),
				Arrays.asList(
						new NoArgConstructorMyProjection(),
						new NoArgConstructorMyProjection(),
						new NoArgConstructorMyProjection()
				)
		);
	}

	class NoArgConstructorMyProjection {
		public final String text;
		public final Integer integer;

		@ProjectionConstructor
		public NoArgConstructorMyProjection() {
			this.text = "foo";
			this.integer = 42;
		}

		public NoArgConstructorMyProjection(String text, Integer integer) {
			this.text = text;
			this.integer = integer;
		}
	}

	@Test
	void noProjectionConstructor() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public Integer integer;
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( NoProjectionConstructorMyNonProjection.class )
				.setup( IndexedEntity.class );

		try ( SearchSession session = mapping.createSession() ) {
			assertThatThrownBy( () -> session.search( IndexedEntity.class )
					.select( NoProjectionConstructorMyNonProjection.class ) )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll( "Invalid object class for projection",
							NoProjectionConstructorMyNonProjection.class.getName(),
							"Make sure that this class is mapped correctly, "
									+ "either through annotations (@ProjectionConstructor) or programmatic mapping" );
		}
	}

	static class NoProjectionConstructorMyNonProjection {
		public final String text;
		public final Integer integer;

		public NoProjectionConstructorMyNonProjection() {
			this.text = "foo";
			this.integer = 42;
		}

		public NoProjectionConstructorMyNonProjection(String text, Integer integer) {
			this.text = text;
			this.integer = integer;
		}
	}

	// This can happen if the class is not included in any Jandex index on startup.
	@Test
	void annotationNotProcessed() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public Integer integer;
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				// We're not processing annotations on MyProjection on purpose:
				// this simulates the class not being included in any Jandex index on startup.
				.setup( IndexedEntity.class );

		try ( SearchSession session = mapping.createSession() ) {
			assertThatThrownBy( () -> session.search( IndexedEntity.class )
					.select( AnnotationNotProcessedMyProjection.class ) )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll( "Invalid object class for projection",
							AnnotationNotProcessedMyProjection.class.getName(),
							"Make sure that this class is mapped correctly, "
									+ "either through annotations (@ProjectionConstructor) or programmatic mapping",
							"If it is, make sure the class is included in a Jandex index"
									+ " made available to Hibernate Search" );
		}
	}

	static class AnnotationNotProcessedMyProjection {
		public final String text;
		public final Integer integer;

		public AnnotationNotProcessedMyProjection() {
			this.text = "foo";
			this.integer = 42;
		}

		@ProjectionConstructor
		public AnnotationNotProcessedMyProjection(String text, Integer integer) {
			this.text = text;
			this.integer = integer;
		}
	}

	@Test
	void inheritance_sameConstructorParameters_subclassOnlyWithProjectionConstructor() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public Integer integer;
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes(
						Inheritance_sameConstructorParameters_subclassOnlyWithProjectionConstructorMyNonProjection.class,
						Inheritance_sameConstructorParameters_subclassOnlyWithProjectionConstructorMyProjectionSubclass.class )
				.setup( IndexedEntity.class );

		try ( SearchSession session = mapping.createSession() ) {
			assertThatThrownBy( () -> session.search( IndexedEntity.class )
					.select( Inheritance_sameConstructorParameters_subclassOnlyWithProjectionConstructorMyNonProjection.class ) )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll( "Invalid object class for projection",
							Inheritance_sameConstructorParameters_subclassOnlyWithProjectionConstructorMyNonProjection.class
									.getName() );
		}

		testSuccessfulRootProjectionExecutionOnly(
				mapping, IndexedEntity.class,
				Inheritance_sameConstructorParameters_subclassOnlyWithProjectionConstructorMyProjectionSubclass.class,
				Arrays.asList(
						Arrays.asList( "result1", 1 ),
						Arrays.asList( "result2", 2 ),
						Arrays.asList( "result3", 3 )
				),
				Arrays.asList(
						new Inheritance_sameConstructorParameters_subclassOnlyWithProjectionConstructorMyProjectionSubclass(
								"result1", 1 ),
						new Inheritance_sameConstructorParameters_subclassOnlyWithProjectionConstructorMyProjectionSubclass(
								"result2", 2 ),
						new Inheritance_sameConstructorParameters_subclassOnlyWithProjectionConstructorMyProjectionSubclass(
								"result3", 3 )
				)
		);
	}

	static class Inheritance_sameConstructorParameters_subclassOnlyWithProjectionConstructorMyNonProjection {
		public final String text;
		public final Integer integer;

		public Inheritance_sameConstructorParameters_subclassOnlyWithProjectionConstructorMyNonProjection() {
			this.text = "foo";
			this.integer = 42;
		}

		public Inheritance_sameConstructorParameters_subclassOnlyWithProjectionConstructorMyNonProjection(String text,
				Integer integer) {
			this.text = text;
			this.integer = integer;
		}

		public Inheritance_sameConstructorParameters_subclassOnlyWithProjectionConstructorMyNonProjection(String text,
				Integer integer, String somethingElse) {
			this.text = text;
			this.integer = integer;
		}
	}

	static class Inheritance_sameConstructorParameters_subclassOnlyWithProjectionConstructorMyProjectionSubclass
			extends Inheritance_sameConstructorParameters_subclassOnlyWithProjectionConstructorMyNonProjection {
		public Inheritance_sameConstructorParameters_subclassOnlyWithProjectionConstructorMyProjectionSubclass() {
			super();
		}

		@ProjectionConstructor
		public Inheritance_sameConstructorParameters_subclassOnlyWithProjectionConstructorMyProjectionSubclass(String text,
				Integer integer) {
			super( text + "_fromSubclass", integer );
		}

		public Inheritance_sameConstructorParameters_subclassOnlyWithProjectionConstructorMyProjectionSubclass(String text,
				Integer integer, String somethingElse) {
			super( text, integer, somethingElse );
		}
	}

	@Test
	void inheritance_sameConstructorParameters_superclassOnlyWithProjectionConstructor() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public Integer integer;
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes(
						Inheritance_sameConstructorParameters_superclassOnlyWithProjectionConstructorMyProjection.class,
						Inheritance_sameConstructorParameters_superclassOnlyWithProjectionConstructorMyNonProjectionSubclass.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjectionExecutionOnly(
				mapping, IndexedEntity.class,
				Inheritance_sameConstructorParameters_superclassOnlyWithProjectionConstructorMyProjection.class,
				Arrays.asList(
						Arrays.asList( "result1", 1 ),
						Arrays.asList( "result2", 2 ),
						Arrays.asList( "result3", 3 )
				),
				Arrays.asList(
						new Inheritance_sameConstructorParameters_superclassOnlyWithProjectionConstructorMyProjection(
								"result1", 1 ),
						new Inheritance_sameConstructorParameters_superclassOnlyWithProjectionConstructorMyProjection(
								"result2", 2 ),
						new Inheritance_sameConstructorParameters_superclassOnlyWithProjectionConstructorMyProjection(
								"result3", 3 )
				)
		);

		try ( SearchSession session = mapping.createSession() ) {
			assertThatThrownBy( () -> session.search( IndexedEntity.class )
					.select( Inheritance_sameConstructorParameters_superclassOnlyWithProjectionConstructorMyNonProjectionSubclass.class ) )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll( "Invalid object class for projection",
							Inheritance_sameConstructorParameters_superclassOnlyWithProjectionConstructorMyNonProjectionSubclass.class
									.getName() );
		}
	}

	static class Inheritance_sameConstructorParameters_superclassOnlyWithProjectionConstructorMyProjection {
		public final String text;
		public final Integer integer;

		public Inheritance_sameConstructorParameters_superclassOnlyWithProjectionConstructorMyProjection() {
			this.text = "foo";
			this.integer = 42;
		}

		@ProjectionConstructor
		public Inheritance_sameConstructorParameters_superclassOnlyWithProjectionConstructorMyProjection(String text,
				Integer integer) {
			this.text = text;
			this.integer = integer;
		}

		public Inheritance_sameConstructorParameters_superclassOnlyWithProjectionConstructorMyProjection(String text,
				Integer integer, String somethingElse) {
			this.text = text;
			this.integer = integer;
		}
	}

	static class Inheritance_sameConstructorParameters_superclassOnlyWithProjectionConstructorMyNonProjectionSubclass
			extends Inheritance_sameConstructorParameters_superclassOnlyWithProjectionConstructorMyProjection {
		public Inheritance_sameConstructorParameters_superclassOnlyWithProjectionConstructorMyNonProjectionSubclass() {
			super();
		}

		public Inheritance_sameConstructorParameters_superclassOnlyWithProjectionConstructorMyNonProjectionSubclass(String text,
				Integer integer) {
			super( text + "_fromSubclass", integer );
		}

		public Inheritance_sameConstructorParameters_superclassOnlyWithProjectionConstructorMyNonProjectionSubclass(String text,
				Integer integer, String somethingElse) {
			super( text, integer, somethingElse );
		}
	}

	@Test
	void inheritance_sameConstructorParameters_bothClassesWithProjectionConstructor() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public Integer integer;
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes(
						Inheritance_sameConstructorParameters_bothClassesWithProjectionConstructorMyProjection.class,
						Inheritance_sameConstructorParameters_bothClassesWithProjectionConstructorMyProjectionSubclass.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjectionExecutionOnly(
				mapping, IndexedEntity.class,
				Inheritance_sameConstructorParameters_bothClassesWithProjectionConstructorMyProjection.class,
				Arrays.asList(
						Arrays.asList( "result1", 1 ),
						Arrays.asList( "result2", 2 ),
						Arrays.asList( "result3", 3 )
				),
				Arrays.asList(
						new Inheritance_sameConstructorParameters_bothClassesWithProjectionConstructorMyProjection( "result1",
								1 ),
						new Inheritance_sameConstructorParameters_bothClassesWithProjectionConstructorMyProjection( "result2",
								2 ),
						new Inheritance_sameConstructorParameters_bothClassesWithProjectionConstructorMyProjection( "result3",
								3 )
				)
		);

		testSuccessfulRootProjectionExecutionOnly(
				mapping, IndexedEntity.class,
				Inheritance_sameConstructorParameters_bothClassesWithProjectionConstructorMyProjectionSubclass.class,
				Arrays.asList(
						Arrays.asList( "result1", 1 ),
						Arrays.asList( "result2", 2 ),
						Arrays.asList( "result3", 3 )
				),
				Arrays.asList(
						new Inheritance_sameConstructorParameters_bothClassesWithProjectionConstructorMyProjectionSubclass(
								"result1", 1 ),
						new Inheritance_sameConstructorParameters_bothClassesWithProjectionConstructorMyProjectionSubclass(
								"result2", 2 ),
						new Inheritance_sameConstructorParameters_bothClassesWithProjectionConstructorMyProjectionSubclass(
								"result3", 3 )
				)
		);
	}

	static class Inheritance_sameConstructorParameters_bothClassesWithProjectionConstructorMyProjection {
		public final String text;
		public final Integer integer;

		public Inheritance_sameConstructorParameters_bothClassesWithProjectionConstructorMyProjection() {
			this.text = "foo";
			this.integer = 42;
		}

		@ProjectionConstructor
		public Inheritance_sameConstructorParameters_bothClassesWithProjectionConstructorMyProjection(String text,
				Integer integer) {
			this.text = text;
			this.integer = integer;
		}

		public Inheritance_sameConstructorParameters_bothClassesWithProjectionConstructorMyProjection(String text,
				Integer integer, String somethingElse) {
			this.text = text;
			this.integer = integer;
		}
	}

	static class Inheritance_sameConstructorParameters_bothClassesWithProjectionConstructorMyProjectionSubclass
			extends Inheritance_sameConstructorParameters_bothClassesWithProjectionConstructorMyProjection {
		public Inheritance_sameConstructorParameters_bothClassesWithProjectionConstructorMyProjectionSubclass() {
			super();
		}

		@ProjectionConstructor
		public Inheritance_sameConstructorParameters_bothClassesWithProjectionConstructorMyProjectionSubclass(String text,
				Integer integer) {
			super( text + "_fromSubclass", integer );
		}

		public Inheritance_sameConstructorParameters_bothClassesWithProjectionConstructorMyProjectionSubclass(String text,
				Integer integer, String somethingElse) {
			super( text, integer, somethingElse );
		}
	}

	// This checks that everything works correctly when a constructor projection
	// is applied to a non-root element (an object field).
	// This case is tricky because the constructor projection definition is relative
	// to the object field, while usually projection factories expect absolute paths
	// (if everything works correctly they don't in this case, though).
	@Test
	void nonRoot() {
		class Contained {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public Integer integer;
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@IndexedEmbedded
			public Contained contained;
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( NonRootMyProjection.class )
				.setup( IndexedEntity.class );

		try ( SearchSession session = mapping.createSession() ) {
			backendMock.expectSearchProjection(
					INDEX_NAME,
					StubSearchWorkBehavior.of(
							2,
							Arrays.asList( "text1", 1 ),
							Arrays.asList( "text2", 2 )
					)
			);

			assertThat( session.search( IndexedEntity.class )
					.select( f -> f.object( "contained" ).as( NonRootMyProjection.class ) )
					.where( f -> f.matchAll() )
					.fetchAllHits() )
					.usingRecursiveComparison()
					.isEqualTo( Arrays.asList(
							new NonRootMyProjection( "text1", 1 ),
							new NonRootMyProjection( "text2", 2 )
					) );
		}
		backendMock.verifyExpectationsMet();
	}

	static class NonRootMyProjection {
		public final String text;
		public final Integer integer;

		@ProjectionConstructor
		public NonRootMyProjection(String text, Integer integer) {
			this.text = text;
			this.integer = integer;
		}
	}

	@Test
	void incompatibleProjectionWithExtraPropertiesMissing() {
		class Author {
			private Integer id;

			@FullTextField(analyzer = "name", projectable = Projectable.YES)
			private String firstName;

			@FullTextField(analyzer = "name", projectable = Projectable.YES)
			private String lastName;

			public Author() {
			}

			public Author(Integer id, String firstName, String lastName) {
				this.id = id;
				this.firstName = firstName;
				this.lastName = lastName;
			}
		}

		@Indexed(index = Book.INDEX_NAME)
		class Book {
			static final String INDEX_NAME = "Book";
			@DocumentId
			public Integer id;
			@FullTextField
			public String title;
			@IndexedEmbedded(structure = ObjectStructure.NESTED)
			public List<Author> authors;

			public Book() {
			}

			public Book(Integer id, String title, List<Author> authors) {
				this.id = id;
				this.title = title;
				this.authors = authors;
			}

		}

		backendMock.expectAnySchema( Book.INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( IncompatibleProjectionWithExtraPropertiesMissingMyProjectionBook.class )
				.setup( Book.class );

		try ( SearchSession session = mapping.createSession() ) {
			assertThatThrownBy( () -> session.search( Book.class )
					.select( IncompatibleProjectionWithExtraPropertiesMissingMyProjectionBook.class )
					.where( f -> f.matchAll() )
					.fetchAllHits()
			).isInstanceOf( SearchException.class )
					.hasMessageContainingAll(
							"Could not apply projection constructor",
							"Unknown field 'authors.email'",
							"for parameter #1 in"
					).hasMessageFindingMatch(
							// constructor string as per `PojoConstructorModelFormatter`
							Pattern.quote( IncompatibleProjectionWithExtraPropertiesMissingMyProjectionAuthor.class.getName() )
									+ "\\(.+\\)"
					).hasMessageFindingMatch(
							// constructor string as per `PojoConstructorModelFormatter`
							Pattern.quote( IncompatibleProjectionWithExtraPropertiesMissingMyProjectionBook.class.getName() )
									+ "\\(.+\\*java\\.util\\.List\\*\\)"
					);
		}
	}

	@ProjectionConstructor
	static class IncompatibleProjectionWithExtraPropertiesMissingMyProjectionAuthor {
		public final String firstName;
		public final String lastName;
		// Extra property that is part of projection but is not present in the index.
		public final String email;

		IncompatibleProjectionWithExtraPropertiesMissingMyProjectionAuthor(String firstName, String lastName, String email) {
			this.firstName = firstName;
			this.lastName = lastName;
			this.email = email;
		}
	}

	@ProjectionConstructor
	static class IncompatibleProjectionWithExtraPropertiesMissingMyProjectionBook {
		public final String title;
		public final List<IncompatibleProjectionWithExtraPropertiesMissingMyProjectionAuthor> authors;

		IncompatibleProjectionWithExtraPropertiesMissingMyProjectionBook(String title,
				List<IncompatibleProjectionWithExtraPropertiesMissingMyProjectionAuthor> authors) {
			this.title = title;
			this.authors = authors;
		}
	}

	@Test
	void multipleParameterProjectionAnnotations() {
		class Model {
			@Indexed(index = INDEX_NAME)
			class IndexedEntity {
				@DocumentId
				@GenericField
				public Integer id;
			}

			static class MyProjection {
				public final Integer id;

				@ProjectionConstructor
				public MyProjection(@IdProjection @FieldProjection Integer id) {
					this.id = id;
				}
			}
		}

		assertThatThrownBy( () -> setupHelper.start()
				.withAnnotatedTypes( Model.MyProjection.class )
				.setup( Model.IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( Model.MyProjection.class.getName() )
						.constructorContext( Integer.class )
						.methodParameterContext( 0, "id" )
						.failure(
								"Multiple projections are mapped for this parameter",
								"At most one projection is allowed for each parameter" ) );
	}

}
