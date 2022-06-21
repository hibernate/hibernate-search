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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.StandalonePojoMappingSetupHelper;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

@TestForIssue(jiraKey = "HSEARCH-3927")
public class ProjectionConstructorBaseIT {

	private static final String INDEX_NAME = "index_name";

	@Rule
	public BackendMock backendMock = new BackendMock();

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

		testSuccessfulRootProjection(
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

		testSuccessfulRootProjection(
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

		testSuccessfulRootProjection(
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

		testSuccessfulRootProjection(
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
	public void inferredInner_value_multiValued_list() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public Collection<String> text;
			@GenericField
			public Set<Integer> integer;
		}
		class MyProjection {
			public final List<String> text;
			public final List<Integer> integer;
			@ProjectionConstructor
			public MyProjection(List<String> text, List<Integer> integer) {
				this.text = text;
				this.integer = integer;
			}
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( MyProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, MyProjection.class,
				Arrays.asList(
						Arrays.asList( Arrays.asList( "result1_1", "result1_2" ), Arrays.asList( 11, 12 ) ),
						Arrays.asList( Arrays.asList( "result2_1" ), Arrays.asList( 21 ) ),
						Arrays.asList( Collections.emptyList(), Collections.emptyList() ),
						Arrays.asList( Arrays.asList( "result4_1" ), Arrays.asList( 41 ) )
				),
				Arrays.asList(
						new MyProjection( Arrays.asList( "result1_1", "result1_2" ), Arrays.asList( 11, 12 ) ),
						new MyProjection( Arrays.asList( "result2_1" ), Arrays.asList( 21 ) ),
						new MyProjection( Collections.emptyList(), Collections.emptyList() ),
						new MyProjection( Arrays.asList( "result4_1" ), Arrays.asList( 41 ) )
				)
		);
	}

	@Test
	public void inferredInner_value_multiValued_collection() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public List<String> text;
			@GenericField
			public Set<Integer> integer;
		}
		class MyProjection {
			public final Collection<String> text;
			public final Collection<Integer> integer;
			@ProjectionConstructor
			public MyProjection(Collection<String> text, Collection<Integer> integer) {
				this.text = text;
				this.integer = integer;
			}
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( MyProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, MyProjection.class,
				Arrays.asList(
						Arrays.asList( Arrays.asList( "result1_1", "result1_2" ), Arrays.asList( 11, 12 ) ),
						Arrays.asList( Arrays.asList( "result2_1" ), Arrays.asList( 21 ) ),
						Arrays.asList( Collections.emptyList(), Collections.emptyList() ),
						Arrays.asList( Arrays.asList( "result4_1" ), Arrays.asList( 41 ) )
				),
				Arrays.asList(
						new MyProjection( Arrays.asList( "result1_1", "result1_2" ), Arrays.asList( 11, 12 ) ),
						new MyProjection( Arrays.asList( "result2_1" ), Arrays.asList( 21 ) ),
						new MyProjection( Collections.emptyList(), Collections.emptyList() ),
						new MyProjection( Arrays.asList( "result4_1" ), Arrays.asList( 41 ) )
				)
		);
	}

	@Test
	public void inferredInner_value_multiValued_iterable() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public List<String> text;
			@GenericField
			public Set<Integer> integer;
		}
		class MyProjection {
			public final Iterable<String> text;
			public final Iterable<Integer> integer;
			@ProjectionConstructor
			public MyProjection(Iterable<String> text, Iterable<Integer> integer) {
				this.text = text;
				this.integer = integer;
			}
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( MyProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, MyProjection.class,
				Arrays.asList(
						Arrays.asList( Arrays.asList( "result1_1", "result1_2" ), Arrays.asList( 11, 12 ) ),
						Arrays.asList( Arrays.asList( "result2_1" ), Arrays.asList( 21 ) ),
						Arrays.asList( Collections.emptyList(), Collections.emptyList() ),
						Arrays.asList( Arrays.asList( "result4_1" ), Arrays.asList( 41 ) )
				),
				Arrays.asList(
						new MyProjection( Arrays.asList( "result1_1", "result1_2" ), Arrays.asList( 11, 12 ) ),
						new MyProjection( Arrays.asList( "result2_1" ), Arrays.asList( 21 ) ),
						new MyProjection( Collections.emptyList(), Collections.emptyList() ),
						new MyProjection( Arrays.asList( "result4_1" ), Arrays.asList( 41 ) )
				)
		);
	}

	@Test
	public void inferredInner_value_multiValued_set() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public Collection<String> text;
			@GenericField
			public Set<Integer> integer;
		}
		class MyProjection {
			public final Set<String> text;
			public final List<Integer> integer;
			@ProjectionConstructor
			public MyProjection(Set<String> text, List<Integer> integer) {
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
						.constructorContext( ProjectionConstructorBaseIT.class, Set.class, List.class )
						.methodParameterContext( 1, "text" )
						.failure( "Invalid parameter type for projection constructor",
								"java.util.Set<java.lang.String>",
								"When inferring inner projections from constructor parameters,"
										+ " multi-valued constructor parameters must be lists (java.util.List<...>)"
										+ " or list supertypes (java.lang.Iterable<...>, java.util.Collection<...>)" ) );
	}

	@Test
	public void inferredInner_object() {
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
			@FullTextField
			public String text;
			@IndexedEmbedded
			public Contained contained;
		}
		class MyInnerProjection {
			public final String text;
			public final Integer integer;
			@ProjectionConstructor
			public MyInnerProjection(String text, Integer integer) {
				this.text = text;
				this.integer = integer;
			}
		}
		class MyProjection {
			public final String text;
			public final MyInnerProjection contained;
			@ProjectionConstructor
			public MyProjection(String text, MyInnerProjection contained) {
				this.text = text;
				this.contained = contained;
			}
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( MyProjection.class, MyInnerProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, MyProjection.class,
				Arrays.asList(
						Arrays.asList( "result1", Arrays.asList( "result1_1", 11 ) ),
						Arrays.asList( "result2", Arrays.asList( null, null ) ),
						Arrays.asList( "result3", null )
				),
				Arrays.asList(
						new MyProjection( "result1", new MyInnerProjection( "result1_1", 11 ) ),
						new MyProjection( "result2", new MyInnerProjection( null, null ) ),
						new MyProjection( "result3", null )
				)
		);
	}

	// If an inner projection type is not included in any Jandex index on startup,
	// Hibernate Search can still get on its feet thanks to annotated type discovery.
	@Test
	public void inferredInner_object_annotatedTypeDiscovery() {
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
			@FullTextField
			public String text;
			@IndexedEmbedded
			public Contained contained;
		}
		class MyInnerProjection {
			public final String text;
			public final Integer integer;
			@ProjectionConstructor
			public MyInnerProjection(String text, Integer integer) {
				this.text = text;
				this.integer = integer;
			}
		}
		class MyProjection {
			public final String text;
			public final MyInnerProjection contained;
			@ProjectionConstructor
			public MyProjection(String text, MyInnerProjection contained) {
				this.text = text;
				this.contained = contained;
			}
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				// We're not processing annotations on MyInnerProjection on purpose:
				// this simulates the class not being included in any Jandex index on startup.
				.withAnnotatedTypes( MyProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, MyProjection.class,
				Arrays.asList(
						Arrays.asList( "result1", Arrays.asList( "result1_1", 11 ) ),
						Arrays.asList( "result2", Arrays.asList( null, null ) ),
						Arrays.asList( "result3", null )
				),
				Arrays.asList(
						new MyProjection( "result1", new MyInnerProjection( "result1_1", 11 ) ),
						new MyProjection( "result2", new MyInnerProjection( null, null ) ),
						new MyProjection( "result3", null )
				)
		);
	}

	@Test
	public void inferredInner_object_multiValued_list() {
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
			@FullTextField
			public String text;
			@IndexedEmbedded
			public Collection<Contained> contained;
		}
		class MyInnerProjection {
			public final String text;
			public final Integer integer;
			@ProjectionConstructor
			public MyInnerProjection(String text, Integer integer) {
				this.text = text;
				this.integer = integer;
			}
		}
		class MyProjection {
			public final String text;
			public final List<MyInnerProjection> contained;
			@ProjectionConstructor
			public MyProjection(String text, List<MyInnerProjection> contained) {
				this.text = text;
				this.contained = contained;
			}
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( MyProjection.class, MyInnerProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, MyProjection.class,
				Arrays.asList(
						Arrays.asList( "result1", Arrays.asList(
								Arrays.asList( "result1_1", 11 ),
								Arrays.asList( "result1_2", 12 )
						) ),
						Arrays.asList( "result2", Arrays.asList(
								Arrays.asList( "result2_1", 21 )
						) ),
						Arrays.asList( "result3", Collections.emptyList() ),
						Arrays.asList( "result4", Arrays.asList(
								Arrays.asList( "result4_1", 41 )
						) )
				),
				Arrays.asList(
						new MyProjection( "result1", Arrays.asList(
								new MyInnerProjection( "result1_1", 11 ),
								new MyInnerProjection( "result1_2", 12 )
						) ),
						new MyProjection( "result2", Arrays.asList(
								new MyInnerProjection( "result2_1", 21 )
						) ),
						new MyProjection( "result3", Collections.emptyList() ),
						new MyProjection( "result4", Arrays.asList(
								new MyInnerProjection( "result4_1", 41 )
						) )
				)
		);
	}

	@Test
	public void inferredInner_object_multiValued_collection() {
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
			@FullTextField
			public String text;
			@IndexedEmbedded
			public List<Contained> contained;
		}
		class MyInnerProjection {
			public final String text;
			public final Integer integer;
			@ProjectionConstructor
			public MyInnerProjection(String text, Integer integer) {
				this.text = text;
				this.integer = integer;
			}
		}
		class MyProjection {
			public final String text;
			public final Collection<MyInnerProjection> contained;
			@ProjectionConstructor
			public MyProjection(String text, Collection<MyInnerProjection> contained) {
				this.text = text;
				this.contained = contained;
			}
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( MyProjection.class, MyInnerProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, MyProjection.class,
				Arrays.asList(
						Arrays.asList( "result1", Arrays.asList(
								Arrays.asList( "result1_1", 11 ),
								Arrays.asList( "result1_2", 12 )
						) ),
						Arrays.asList( "result2", Arrays.asList(
								Arrays.asList( "result2_1", 21 )
						) ),
						Arrays.asList( "result3", Collections.emptyList() ),
						Arrays.asList( "result4", Arrays.asList(
								Arrays.asList( "result4_1", 41 )
						) )
				),
				Arrays.asList(
						new MyProjection( "result1", Arrays.asList(
								new MyInnerProjection( "result1_1", 11 ),
								new MyInnerProjection( "result1_2", 12 )
						) ),
						new MyProjection( "result2", Arrays.asList(
								new MyInnerProjection( "result2_1", 21 )
						) ),
						new MyProjection( "result3", Collections.emptyList() ),
						new MyProjection( "result4", Arrays.asList(
								new MyInnerProjection( "result4_1", 41 )
						) )
				)
		);
	}

	@Test
	public void inferredInner_object_multiValued_iterable() {
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
			@FullTextField
			public String text;
			@IndexedEmbedded
			public List<Contained> contained;
		}
		class MyInnerProjection {
			public final String text;
			public final Integer integer;
			@ProjectionConstructor
			public MyInnerProjection(String text, Integer integer) {
				this.text = text;
				this.integer = integer;
			}
		}
		class MyProjection {
			public final String text;
			public final Iterable<MyInnerProjection> contained;
			@ProjectionConstructor
			public MyProjection(String text, Iterable<MyInnerProjection> contained) {
				this.text = text;
				this.contained = contained;
			}
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( MyProjection.class, MyInnerProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, MyProjection.class,
				Arrays.asList(
						Arrays.asList( "result1", Arrays.asList(
								Arrays.asList( "result1_1", 11 ),
								Arrays.asList( "result1_2", 12 )
						) ),
						Arrays.asList( "result2", Arrays.asList(
								Arrays.asList( "result2_1", 21 )
						) ),
						Arrays.asList( "result3", Collections.emptyList() ),
						Arrays.asList( "result4", Arrays.asList(
								Arrays.asList( "result4_1", 41 )
						) )
				),
				Arrays.asList(
						new MyProjection( "result1", Arrays.asList(
								new MyInnerProjection( "result1_1", 11 ),
								new MyInnerProjection( "result1_2", 12 )
						) ),
						new MyProjection( "result2", Arrays.asList(
								new MyInnerProjection( "result2_1", 21 )
						) ),
						new MyProjection( "result3", Collections.emptyList() ),
						new MyProjection( "result4", Arrays.asList(
								new MyInnerProjection( "result4_1", 41 )
						) )
				)
		);
	}

	@Test
	public void inferredInner_object_multiValued_set() {
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
			@FullTextField
			public String text;
			@IndexedEmbedded
			public List<Contained> contained;
		}
		class MyInnerProjection {
			public final String text;
			public final Integer integer;
			@ProjectionConstructor
			public MyInnerProjection(String text, Integer integer) {
				this.text = text;
				this.integer = integer;
			}
		}
		class MyProjection {
			public final String text;
			public final Set<MyInnerProjection> contained;
			@ProjectionConstructor
			public MyProjection(String text, Set<MyInnerProjection> contained) {
				this.text = text;
				this.contained = contained;
			}
		}

		assertThatThrownBy( () -> setupHelper.start()
				.withAnnotatedTypes( MyProjection.class )
				.setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( MyProjection.class.getName() )
						.constructorContext( ProjectionConstructorBaseIT.class, String.class, Set.class )
						.methodParameterContext( 2, "contained" )
						.failure( "Invalid parameter type for projection constructor",
								"java.util.Set<" + MyInnerProjection.class.getName() + ">",
								"When inferring inner projections from constructor parameters,"
										+ " multi-valued constructor parameters must be lists (java.util.List<...>)"
										+ " or list supertypes (java.lang.Iterable<...>, java.util.Collection<...>)" ) );
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

		testSuccessfulRootProjection(
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

		testSuccessfulRootProjection(
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

		testSuccessfulRootProjection(
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

		testSuccessfulRootProjection(
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
						.typeContext( Model.ProjectionLevel1.class.getName() )
						.constructorContext( Model.class, String.class, Model.ProjectionLevel2.class )
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
						.typeContext( Model.ProjectionLevel1.class.getName() )
						.constructorContext( Model.class, String.class, Model.ProjectionLevel2.class )
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
						.typeContext( Model.ProjectionLevel2.class.getName() )
						.constructorContext( Model.class, String.class, Model.ProjectionLevel3.class )
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

	private <P> void testSuccessfulRootProjection(SearchMapping mapping, Class<?> indexedType, Class<P> projectionType,
			List<?> rawProjectionResults, List<P> expectedProjectionResults) {
		try ( SearchSession session = mapping.createSession() ) {
			backendMock.expectSearchProjection(
					INDEX_NAME,
					StubSearchWorkBehavior.of(
							rawProjectionResults.size(),
							rawProjectionResults
					)
			);

			assertThat( session.search( indexedType )
					.select( projectionType )
					.where( f -> f.matchAll() )
					.fetchAllHits() )
					.usingRecursiveComparison()
					.isEqualTo( expectedProjectionResults );
		}
		backendMock.verifyExpectationsMet();
	}

}
