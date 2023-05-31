/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

@TestForIssue(jiraKey = "HSEARCH-3927")
public class ProjectionConstructorBaseIT extends AbstractProjectionConstructorIT {

	@Rule
	public StandalonePojoMappingSetupHelper setupHelper = StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Test
	public void typeLevelAnnotation() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public Integer integer;
		}
		@ProjectionConstructor
		class MyProjection {
			public final String text;
			public final Integer integer;
			public MyProjection(String text, Integer integer) {
				this.text = text;
				this.integer = integer;
			}
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( MyProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjectionExecutionOnly(
				mapping, IndexedEntity.class, MyProjection.class,
				Arrays.asList(
						Arrays.asList( "result1", 1 ),
						Arrays.asList( "result2", 2 ),
						Arrays.asList( "result3", 3 )
				),
				Arrays.asList(
						new MyProjection( "result1", 1 ),
						new MyProjection( "result2", 2 ),
						new MyProjection( "result3", 3 )
				)
		);
	}

	@Test
	public void typeLevelAnnotation_multipleConstructors() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public Integer integer;
		}
		@ProjectionConstructor
		class MyProjection {
			public final String text;
			public final Integer integer;
			public MyProjection(String text, Integer integer) {
				this.text = text;
				this.integer = integer;
			}
			public MyProjection(String text, Integer integer, String somethingElse) {
				this.text = text;
				this.integer = integer;
			}
		}

		assertThatThrownBy( () -> setupHelper.start()
				.withAnnotatedTypes( MyProjection.class )
				.setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( MyProjection.class.getName() )
						.annotationContextAnyParameters( ProjectionConstructor.class )
						.failure( "No main constructor for type",
								MyProjection.class.getName(), "this type does not declare exactly one constructor" ) );
	}

	@Test
	public void constructorLevelAnnotation() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public Integer integer;
		}
		class MyProjection {
			public final String text;
			public final Integer integer;
			public MyProjection() {
				this.text = "foo";
				this.integer = 42;
			}
			@ProjectionConstructor
			public MyProjection(String text, Integer integer) {
				this.text = text;
				this.integer = integer;
			}
			public MyProjection(String text, Integer integer, String somethingElse) {
				this.text = text;
				this.integer = integer;
			}
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( MyProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjectionExecutionOnly(
				mapping, IndexedEntity.class, MyProjection.class,
				Arrays.asList(
						Arrays.asList( "result1", 1 ),
						Arrays.asList( "result2", 2 ),
						Arrays.asList( "result3", 3 )
				),
				Arrays.asList(
						new MyProjection( "result1", 1 ),
						new MyProjection( "result2", 2 ),
						new MyProjection( "result3", 3 )
				)
		);
	}

	@Test
	public void abstractType() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public Integer integer;
		}
		abstract class MyAbstractProjection {
			public final String text;
			public final Integer integer;
			@ProjectionConstructor
			public MyAbstractProjection(String text, Integer integer) {
				this.text = text;
				this.integer = integer;
			}
			public MyAbstractProjection(String text, Integer integer, String somethingElse) {
				this.text = text;
				this.integer = integer;
			}
		}

		assertThatThrownBy( () -> setupHelper.start()
				.withAnnotatedTypes( MyAbstractProjection.class )
				.setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( MyAbstractProjection.class.getName() )
						.constructorContext( ProjectionConstructorBaseIT.class, String.class, Integer.class )
						.failure( "Invalid declaring type for projection constructor",
								MyAbstractProjection.class.getName(), "is abstract",
								"Projection constructors can only be declared on concrete types" ) );
	}

	@Test
	public void entityAndProjection() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public Integer integer;

			public IndexedEntity() {
			}

			@ProjectionConstructor
			public IndexedEntity(String text, Integer integer) {
				this.text = text;
				this.integer = integer;
			}
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start().setup( IndexedEntity.class );

		testSuccessfulRootProjectionExecutionOnly(
				mapping, IndexedEntity.class, IndexedEntity.class,
				Arrays.asList(
						Arrays.asList( "result1", 1 ),
						Arrays.asList( "result2", 2 ),
						Arrays.asList( "result3", 3 )
				),
				Arrays.asList(
						new IndexedEntity( "result1", 1 ),
						new IndexedEntity( "result2", 2 ),
						new IndexedEntity( "result3", 3 )
				)
		);
	}

	@Test
	public void noArgConstructor() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public Integer integer;
		}
		class MyProjection {
			public final String text;
			public final Integer integer;
			@ProjectionConstructor
			public MyProjection() {
				this.text = "foo";
				this.integer = 42;
			}
			public MyProjection(String text, Integer integer) {
				this.text = text;
				this.integer = integer;
			}
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( MyProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjectionExecutionOnly(
				mapping, IndexedEntity.class, MyProjection.class,
				Arrays.asList(
						Arrays.asList(),
						Arrays.asList(),
						Arrays.asList()
				),
				Arrays.asList(
						new MyProjection(),
						new MyProjection(),
						new MyProjection()
				)
		);
	}

	@Test
	public void noProjectionConstructor() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public Integer integer;
		}
		class MyNonProjection {
			public final String text;
			public final Integer integer;
			public MyNonProjection() {
				this.text = "foo";
				this.integer = 42;
			}
			public MyNonProjection(String text, Integer integer) {
				this.text = text;
				this.integer = integer;
			}
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( MyNonProjection.class )
				.setup( IndexedEntity.class );

		try ( SearchSession session = mapping.createSession() ) {
			assertThatThrownBy( () -> session.search( IndexedEntity.class )
					.select( MyNonProjection.class ) )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll( "Invalid object class for projection",
							MyNonProjection.class.getName(),
							"Make sure that this class is mapped correctly, "
									+ "either through annotations (@ProjectionConstructor) or programmatic mapping" );
		}
	}

	// This can happen if the class is not included in any Jandex index on startup.
	@Test
	public void annotationNotProcessed() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public Integer integer;
		}
		class MyProjection {
			public final String text;
			public final Integer integer;
			public MyProjection() {
				this.text = "foo";
				this.integer = 42;
			}
			@ProjectionConstructor
			public MyProjection(String text, Integer integer) {
				this.text = text;
				this.integer = integer;
			}
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				// We're not processing annotations on MyProjection on purpose:
				// this simulates the class not being included in any Jandex index on startup.
				.setup( IndexedEntity.class );

		try ( SearchSession session = mapping.createSession() ) {
			assertThatThrownBy( () -> session.search( IndexedEntity.class )
					.select( MyProjection.class ) )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll( "Invalid object class for projection",
							MyProjection.class.getName(),
							"Make sure that this class is mapped correctly, "
									+ "either through annotations (@ProjectionConstructor) or programmatic mapping",
							"If it is, make sure the class is included in a Jandex index"
									+ " made available to Hibernate Search" );
		}
	}

	@Test
	public void inheritance_sameConstructorParameters_subclassOnlyWithProjectionConstructor() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public Integer integer;
		}
		class MyNonProjection {
			public final String text;
			public final Integer integer;
			public MyNonProjection() {
				this.text = "foo";
				this.integer = 42;
			}
			public MyNonProjection(String text, Integer integer) {
				this.text = text;
				this.integer = integer;
			}
			public MyNonProjection(String text, Integer integer, String somethingElse) {
				this.text = text;
				this.integer = integer;
			}
		}
		class MyProjectionSubclass extends MyNonProjection {
			public MyProjectionSubclass() {
				super();
			}
			@ProjectionConstructor
			public MyProjectionSubclass(String text, Integer integer) {
				super( text + "_fromSubclass", integer );
			}
			public MyProjectionSubclass(String text, Integer integer, String somethingElse) {
				super( text, integer, somethingElse );
			}
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( MyNonProjection.class, MyProjectionSubclass.class )
				.setup( IndexedEntity.class );

		try ( SearchSession session = mapping.createSession() ) {
			assertThatThrownBy( () -> session.search( IndexedEntity.class )
					.select( MyNonProjection.class ) )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll( "Invalid object class for projection",
							MyNonProjection.class.getName() );
		}

		testSuccessfulRootProjectionExecutionOnly(
				mapping, IndexedEntity.class, MyProjectionSubclass.class,
				Arrays.asList(
						Arrays.asList( "result1", 1 ),
						Arrays.asList( "result2", 2 ),
						Arrays.asList( "result3", 3 )
				),
				Arrays.asList(
						new MyProjectionSubclass( "result1", 1 ),
						new MyProjectionSubclass( "result2", 2 ),
						new MyProjectionSubclass( "result3", 3 )
				)
		);
	}

	@Test
	public void inheritance_sameConstructorParameters_superclassOnlyWithProjectionConstructor() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public Integer integer;
		}
		class MyProjection {
			public final String text;
			public final Integer integer;
			public MyProjection() {
				this.text = "foo";
				this.integer = 42;
			}
			@ProjectionConstructor
			public MyProjection(String text, Integer integer) {
				this.text = text;
				this.integer = integer;
			}
			public MyProjection(String text, Integer integer, String somethingElse) {
				this.text = text;
				this.integer = integer;
			}
		}
		class MyNonProjectionSubclass extends MyProjection {
			public MyNonProjectionSubclass() {
				super();
			}
			public MyNonProjectionSubclass(String text, Integer integer) {
				super( text + "_fromSubclass", integer );
			}
			public MyNonProjectionSubclass(String text, Integer integer, String somethingElse) {
				super( text, integer, somethingElse );
			}
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( MyProjection.class, MyNonProjectionSubclass.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjectionExecutionOnly(
				mapping, IndexedEntity.class, MyProjection.class,
				Arrays.asList(
						Arrays.asList( "result1", 1 ),
						Arrays.asList( "result2", 2 ),
						Arrays.asList( "result3", 3 )
				),
				Arrays.asList(
						new MyProjection( "result1", 1 ),
						new MyProjection( "result2", 2 ),
						new MyProjection( "result3", 3 )
				)
		);

		try ( SearchSession session = mapping.createSession() ) {
			assertThatThrownBy( () -> session.search( IndexedEntity.class )
					.select( MyNonProjectionSubclass.class ) )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll( "Invalid object class for projection",
							MyNonProjectionSubclass.class.getName() );
		}
	}

	@Test
	public void inheritance_sameConstructorParameters_bothClassesWithProjectionConstructor() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public Integer integer;
		}
		class MyProjection {
			public final String text;
			public final Integer integer;
			public MyProjection() {
				this.text = "foo";
				this.integer = 42;
			}
			@ProjectionConstructor
			public MyProjection(String text, Integer integer) {
				this.text = text;
				this.integer = integer;
			}
			public MyProjection(String text, Integer integer, String somethingElse) {
				this.text = text;
				this.integer = integer;
			}
		}
		class MyProjectionSubclass extends MyProjection {
			public MyProjectionSubclass() {
				super();
			}
			@ProjectionConstructor
			public MyProjectionSubclass(String text, Integer integer) {
				super( text + "_fromSubclass", integer );
			}
			public MyProjectionSubclass(String text, Integer integer, String somethingElse) {
				super( text, integer, somethingElse );
			}
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( MyProjection.class, MyProjectionSubclass.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjectionExecutionOnly(
				mapping, IndexedEntity.class, MyProjection.class,
				Arrays.asList(
						Arrays.asList( "result1", 1 ),
						Arrays.asList( "result2", 2 ),
						Arrays.asList( "result3", 3 )
				),
				Arrays.asList(
						new MyProjection( "result1", 1 ),
						new MyProjection( "result2", 2 ),
						new MyProjection( "result3", 3 )
				)
		);

		testSuccessfulRootProjectionExecutionOnly(
				mapping, IndexedEntity.class, MyProjectionSubclass.class,
				Arrays.asList(
						Arrays.asList( "result1", 1 ),
						Arrays.asList( "result2", 2 ),
						Arrays.asList( "result3", 3 )
				),
				Arrays.asList(
						new MyProjectionSubclass( "result1", 1 ),
						new MyProjectionSubclass( "result2", 2 ),
						new MyProjectionSubclass( "result3", 3 )
				)
		);
	}

	@Test
	public void cycle() {
		class Model {
			@Indexed(index = INDEX_NAME)
			class Level1 {
				@DocumentId
				public Integer id;
				@FullTextField
				public String text;
				@IndexedEmbedded(includeDepth = 10)
				public Level2 level2;
			}
			class Level2 {
				@DocumentId
				public Integer id;
				@FullTextField
				public String text;
				@IndexedEmbedded
				Level1 level1;
			}
			class ProjectionLevel1 {
				public final String text;
				public final ProjectionLevel2 level2;
				@ProjectionConstructor
				public ProjectionLevel1(String text, ProjectionLevel2 level2) {
					this.text = text;
					this.level2 = level2;
				}
			}
			class ProjectionLevel2 {
				public final String text;
				public final ProjectionLevel1 level1;
				@ProjectionConstructor
				public ProjectionLevel2(String text, ProjectionLevel1 level1) {
					this.text = text;
					this.level1 = level1;
				}
			}
		}

		assertThatThrownBy( () -> setupHelper.start()
				.withAnnotatedTypes( Model.ProjectionLevel1.class, Model.ProjectionLevel2.class )
				.setup( Model.Level1.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( Model.ProjectionLevel1.class.getName() )
						.constructorContext( Model.class, String.class, Model.ProjectionLevel2.class )
						.methodParameterContext( 2, "level2" )
						.typeContext( Model.ProjectionLevel2.class.getName() )
						.constructorContext( Model.class, String.class, Model.ProjectionLevel1.class )
						.methodParameterContext( 2, "level1" )
						.failure( "Infinite object projection recursion starting from projection constructor "
								+ Model.ProjectionLevel1.class.getName() + "(" + Model.class.getName() + ", "
								+ String.class.getName() + ", " + Model.ProjectionLevel2.class.getName() + ")"
								+ " and involving field path '.level2.level1'" ) );
	}

	@Test
	public void cycle_indirect() {
		class Model {
			@Indexed(index = INDEX_NAME)
			class Level1 {
				@DocumentId
				public Integer id;
				@FullTextField
				public String text;
				@IndexedEmbedded(includeDepth = 10)
				public Level2 level2;
			}
			class Level2 {
				@DocumentId
				public Integer id;
				@FullTextField
				public String text;
				@IndexedEmbedded
				Level3 level3;
			}
			class Level3 {
				@DocumentId
				public Integer id;
				@FullTextField
				public String text;
				@IndexedEmbedded
				Level1 level1;
			}
			class ProjectionLevel1 {
				public final String text;
				public final ProjectionLevel2 level2;
				@ProjectionConstructor
				public ProjectionLevel1(String text, ProjectionLevel2 level2) {
					this.text = text;
					this.level2 = level2;
				}
			}
			class ProjectionLevel2 {
				public final String text;
				public final ProjectionLevel3 level3;
				@ProjectionConstructor
				public ProjectionLevel2(String text, ProjectionLevel3 level3) {
					this.text = text;
					this.level3 = level3;
				}
			}
			class ProjectionLevel3 {
				public final String text;
				public final ProjectionLevel1 level1;
				@ProjectionConstructor
				public ProjectionLevel3(String text, ProjectionLevel1 level1) {
					this.text = text;
					this.level1 = level1;
				}
			}
		}

		assertThatThrownBy( () -> setupHelper.start()
				.withAnnotatedTypes( Model.ProjectionLevel1.class, Model.ProjectionLevel2.class )
				.setup( Model.Level1.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( Model.ProjectionLevel1.class.getName() )
						.constructorContext( Model.class, String.class, Model.ProjectionLevel2.class )
						.methodParameterContext( 2, "level2" )
						.typeContext( Model.ProjectionLevel2.class.getName() )
						.constructorContext( Model.class, String.class, Model.ProjectionLevel3.class )
						.methodParameterContext( 2, "level3" )
						.typeContext( Model.ProjectionLevel3.class.getName() )
						.constructorContext( Model.class, String.class, Model.ProjectionLevel1.class )
						.methodParameterContext( 2, "level1" )
						.failure( "Infinite object projection recursion starting from projection constructor "
								+ Model.ProjectionLevel1.class.getName() + "(" + Model.class.getName() + ", "
								+ String.class.getName() + ", " + Model.ProjectionLevel2.class.getName() + ")"
								+ " and involving field path '.level2.level3.level1'" ) );
	}

	@Test
	public void cycle_buried() {
		class Model {
			@Indexed(index = INDEX_NAME)
			class Level1 {
				@DocumentId
				public Integer id;
				@FullTextField
				public String text;
				@IndexedEmbedded(includeDepth = 10)
				public Level2 level2;
			}
			class Level2 {
				@DocumentId
				public Integer id;
				@FullTextField
				public String text;
				@IndexedEmbedded
				Level3 level3;
			}
			class Level3 {
				@DocumentId
				public Integer id;
				@FullTextField
				public String text;
				@IndexedEmbedded
				Level1 level1;
			}
			class ProjectionLevel1 {
				public final String text;
				public final ProjectionLevel2 level2;
				@ProjectionConstructor
				public ProjectionLevel1(String text, ProjectionLevel2 level2) {
					this.text = text;
					this.level2 = level2;
				}
			}
			class ProjectionLevel2 {
				public final String text;
				public final ProjectionLevel3 level3;
				@ProjectionConstructor
				public ProjectionLevel2(String text, ProjectionLevel3 level3) {
					this.text = text;
					this.level3 = level3;
				}
			}
			class ProjectionLevel3 {
				public final String text;
				public final ProjectionLevel2 level2;
				@ProjectionConstructor
				public ProjectionLevel3(String text, ProjectionLevel2 level2) {
					this.text = text;
					this.level2 = level2;
				}
			}
		}

		assertThatThrownBy( () -> setupHelper.start()
				.withAnnotatedTypes( Model.ProjectionLevel1.class, Model.ProjectionLevel2.class )
				.setup( Model.Level1.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( Model.ProjectionLevel1.class.getName() )
						.constructorContext( Model.class, String.class, Model.ProjectionLevel2.class )
						.methodParameterContext( 2, "level2" )
						.typeContext( Model.ProjectionLevel2.class.getName() )
						.constructorContext( Model.class, String.class, Model.ProjectionLevel3.class )
						.methodParameterContext( 2, "level3" )
						.typeContext( Model.ProjectionLevel3.class.getName() )
						.constructorContext( Model.class, String.class, Model.ProjectionLevel2.class )
						.methodParameterContext( 2, "level2" )
						.failure( "Infinite object projection recursion starting from projection constructor "
								+ Model.ProjectionLevel2.class.getName() + "(" + Model.class.getName() + ", "
								+ String.class.getName() + ", " + Model.ProjectionLevel3.class.getName() + ")"
								+ " and involving field path '.level3.level2'" ) );
	}

	// This checks that everything works correctly when a constructor projection
	// is applied to a non-root element (an object field).
	// This case is tricky because the constructor projection definition is relative
	// to the object field, while usually projection factories expect absolute paths
	// (if everything works correctly they don't in this case, though).
	@Test
	public void nonRoot() {
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
		class MyProjection {
			public final String text;
			public final Integer integer;
			@ProjectionConstructor
			public MyProjection(String text, Integer integer) {
				this.text = text;
				this.integer = integer;
			}
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( MyProjection.class )
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
					.select( f -> f.object( "contained" ).as( MyProjection.class ) )
					.where( f -> f.matchAll() )
					.fetchAllHits() )
					.usingRecursiveComparison()
					.isEqualTo( Arrays.asList(
							new MyProjection( "text1", 1 ),
							new MyProjection( "text2", 2 )
					) );
		}
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void incompatibleProjectionWithExtraPropertiesMissing() {
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

		@ProjectionConstructor
		class MyProjectionAuthor {
			public final String firstName;
			public final String lastName;
			// Extra property that is part of projection but is not present in the index.
			public final String email;

			MyProjectionAuthor(String firstName, String lastName, String email) {
				this.firstName = firstName;
				this.lastName = lastName;
				this.email = email;
			}
		}
		@ProjectionConstructor
		class MyProjectionBook {
			public final String title;
			public final List<MyProjectionAuthor> authors;

			MyProjectionBook(String title, List<MyProjectionAuthor> authors) {
				this.title = title;
				this.authors = authors;
			}
		}

		backendMock.expectAnySchema( Book.INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( MyProjectionBook.class )
				.setup( Book.class );

		try ( SearchSession session = mapping.createSession() ) {
			assertThatThrownBy( () -> session.search( Book.class )
					.select( MyProjectionBook.class )
					.where( f -> f.matchAll() )
					.fetchAllHits()
			).isInstanceOf( SearchException.class )
					.hasMessageContainingAll(
							"Could not apply projection constructor",
							"Unknown field 'authors.email'",
							"for parameter #2 in"
					).hasMessageFindingMatch(
							// constructor string as per `PojoConstructorModelFormatter`
							Pattern.quote( MyProjectionAuthor.class.getName() ) + "\\(.+\\)"
					).hasMessageFindingMatch(
							// constructor string as per `PojoConstructorModelFormatter`
							Pattern.quote( MyProjectionBook.class.getName() ) + "\\(.+\\*java\\.util\\.List\\*\\)"
					);
		}
	}

}
