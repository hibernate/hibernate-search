/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@TestForIssue(jiraKey = "HSEARCH-3927")
class ProjectionConstructorInnerInferredIT extends AbstractProjectionConstructorIT {

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Test
	void value() {
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
						Arrays.asList( "result1", 11 ),
						Arrays.asList( "result2", 21 )
				),
				f -> f.composite()
						.from(
								dummyProjectionForEnclosingClassInstance( f ),
								f.field( "text", String.class ),
								f.field( "integer", Integer.class )
						)
						.asList(),
				Arrays.asList(
						new MyProjection( "result1", 11 ),
						new MyProjection( "result2", 21 )
				)
		);
	}

	@Test
	void value_multiValued_list() {
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
				f -> f.composite()
						.from(
								dummyProjectionForEnclosingClassInstance( f ),
								f.field( "text", String.class ).multi(),
								f.field( "integer", Integer.class ).multi()
						)
						.asList(),
				Arrays.asList(
						new MyProjection( Arrays.asList( "result1_1", "result1_2" ), Arrays.asList( 11, 12 ) ),
						new MyProjection( Arrays.asList( "result2_1" ), Arrays.asList( 21 ) ),
						new MyProjection( Collections.emptyList(), Collections.emptyList() ),
						new MyProjection( Arrays.asList( "result4_1" ), Arrays.asList( 41 ) )
				)
		);
	}

	@Test
	void value_multiValued_collection() {
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
				f -> f.composite()
						.from(
								dummyProjectionForEnclosingClassInstance( f ),
								f.field( "text", String.class ).multi(),
								f.field( "integer", Integer.class ).multi()
						)
						.asList(),
				Arrays.asList(
						new MyProjection( Arrays.asList( "result1_1", "result1_2" ), Arrays.asList( 11, 12 ) ),
						new MyProjection( Arrays.asList( "result2_1" ), Arrays.asList( 21 ) ),
						new MyProjection( Collections.emptyList(), Collections.emptyList() ),
						new MyProjection( Arrays.asList( "result4_1" ), Arrays.asList( 41 ) )
				)
		);
	}

	@Test
	void value_multiValued_iterable() {
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
				f -> f.composite()
						.from(
								dummyProjectionForEnclosingClassInstance( f ),
								f.field( "text", String.class ).multi(),
								f.field( "integer", Integer.class ).multi()
						)
						.asList(),
				Arrays.asList(
						new MyProjection( Arrays.asList( "result1_1", "result1_2" ), Arrays.asList( 11, 12 ) ),
						new MyProjection( Arrays.asList( "result2_1" ), Arrays.asList( 21 ) ),
						new MyProjection( Collections.emptyList(), Collections.emptyList() ),
						new MyProjection( Arrays.asList( "result4_1" ), Arrays.asList( 41 ) )
				)
		);
	}

	@Test
	void value_multiValued_set() {
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
						.constructorContext( ProjectionConstructorInnerInferredIT.class, Set.class, List.class )
						.methodParameterContext( 1, "text" )
						.failure( "Invalid parameter type for projection constructor",
								"java.util.Set<java.lang.String>",
								"When inferring the cardinality of inner projections from constructor parameters,"
										+ " multi-valued constructor parameters must be lists (java.util.List<...>)"
										+ " or list supertypes (java.lang.Iterable<...>, java.util.Collection<...>)" ) );
	}

	@Test
	void object() {
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
				f -> f.composite()
						.from(
								dummyProjectionForEnclosingClassInstance( f ),
								f.field( "text", String.class ),
								f.object( "contained" )
										.from(
												dummyProjectionForEnclosingClassInstance( f ),
												f.field( "contained.text", String.class ),
												f.field( "contained.integer", Integer.class )
										)
										.asList()
						)
						.asList(),
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
	void object_annotatedTypeDiscovery() {
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
				f -> f.composite()
						.from(
								dummyProjectionForEnclosingClassInstance( f ),
								f.field( "text", String.class ),
								f.object( "contained" )
										.from(
												dummyProjectionForEnclosingClassInstance( f ),
												f.field( "contained.text", String.class ),
												f.field( "contained.integer", Integer.class )
										)
										.asList()
						)
						.asList(),
				Arrays.asList(
						new MyProjection( "result1", new MyInnerProjection( "result1_1", 11 ) ),
						new MyProjection( "result2", new MyInnerProjection( null, null ) ),
						new MyProjection( "result3", null )
				)
		);
	}

	@Test
	void object_multiValued_list() {
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
				f -> f.composite()
						.from(
								dummyProjectionForEnclosingClassInstance( f ),
								f.field( "text", String.class ),
								f.object( "contained" )
										.from(
												dummyProjectionForEnclosingClassInstance( f ),
												f.field( "contained.text", String.class ),
												f.field( "contained.integer", Integer.class )
										)
										.asList()
										.multi()
						)
						.asList(),
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
	void object_multiValued_collection() {
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
				f -> f.composite()
						.from(
								dummyProjectionForEnclosingClassInstance( f ),
								f.field( "text", String.class ),
								f.object( "contained" )
										.from(
												dummyProjectionForEnclosingClassInstance( f ),
												f.field( "contained.text", String.class ),
												f.field( "contained.integer", Integer.class )
										)
										.asList()
										.multi()
						)
						.asList(),
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
	void object_multiValued_iterable() {
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
				f -> f.composite()
						.from(
								dummyProjectionForEnclosingClassInstance( f ),
								f.field( "text", String.class ),
								f.object( "contained" )
										.from(
												dummyProjectionForEnclosingClassInstance( f ),
												f.field( "contained.text", String.class ),
												f.field( "contained.integer", Integer.class )
										)
										.asList()
										.multi()
						)
						.asList(),
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
	void object_multiValued_set() {
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
						.constructorContext( ProjectionConstructorInnerInferredIT.class, String.class, Set.class )
						.methodParameterContext( 2, "contained" )
						.failure( "Invalid parameter type for projection constructor",
								"java.util.Set<" + MyInnerProjection.class.getName() + ">",
								"When inferring the cardinality of inner projections from constructor parameters,"
										+ " multi-valued constructor parameters must be lists (java.util.List<...>)"
										+ " or list supertypes (java.lang.Iterable<...>, java.util.Collection<...>)" ) );
	}

}
