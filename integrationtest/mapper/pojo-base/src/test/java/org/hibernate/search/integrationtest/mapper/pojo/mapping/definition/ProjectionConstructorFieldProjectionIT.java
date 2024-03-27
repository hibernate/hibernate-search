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

import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FieldProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@TestForIssue(jiraKey = "HSEARCH-4574")
class ProjectionConstructorFieldProjectionIT extends AbstractProjectionConstructorIT {

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Test
	void noArg() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
		}
		class MyProjection {
			public final String text;

			@ProjectionConstructor
			public MyProjection(@FieldProjection String text) {
				this.text = text;
			}
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( MyProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, MyProjection.class,
				Arrays.asList(
						Arrays.asList( "result1" ),
						Arrays.asList( "result2" )
				),
				f -> f.composite()
						.from(
								dummyProjectionForEnclosingClassInstance( f ),
								f.field( "text", String.class )
						)
						.asList(),
				Arrays.asList(
						new MyProjection( "result1" ),
						new MyProjection( "result2" )
				)
		);
	}

	@Test
	void path() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField(name = "myText")
			public String text;
		}
		class MyProjection {
			public final String text;

			@ProjectionConstructor
			public MyProjection(@FieldProjection(path = "myText") String text) {
				this.text = text;
			}
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( MyProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, MyProjection.class,
				Arrays.asList(
						Arrays.asList( "result1" ),
						Arrays.asList( "result2" )
				),
				f -> f.composite()
						.from(
								dummyProjectionForEnclosingClassInstance( f ),
								f.field( "myText", String.class )
						)
						.asList(),
				Arrays.asList(
						new MyProjection( "result1" ),
						new MyProjection( "result2" )
				)
		);
	}

	@Test
	void valueConvert() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@GenericField
			public MyEnum myEnum;
		}
		class MyProjection {
			public final String myEnum;

			@ProjectionConstructor
			public MyProjection(@FieldProjection(convert = ValueConvert.NO) String myEnum) {
				this.myEnum = myEnum;
			}
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( MyProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, MyProjection.class,
				Arrays.asList(
						Arrays.asList( "result1" ),
						Arrays.asList( "result2" )
				),
				f -> f.composite()
						.from(
								dummyProjectionForEnclosingClassInstance( f ),
								f.field( "myEnum", String.class, ValueConvert.NO )
						)
						.asList(),
				Arrays.asList(
						new MyProjection( "result1" ),
						new MyProjection( "result2" )
				)
		);
	}

	enum MyEnum {
		FOO, BAR;
	}

	@Test
	void supertype() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
		}
		class MyProjection {
			public final Object text;

			@ProjectionConstructor
			public MyProjection(@FieldProjection Object text) {
				this.text = text;
			}
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( MyProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, MyProjection.class,
				Arrays.asList(
						Arrays.asList( "result1" ),
						Arrays.asList( "result2" )
				),
				f -> f.composite()
						.from(
								dummyProjectionForEnclosingClassInstance( f ),
								f.field( "text", Object.class )
						)
						.asList(),
				Arrays.asList(
						new MyProjection( "result1" ),
						new MyProjection( "result2" )
				)
		);
	}

	@Test
	void primitiveType() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@GenericField
			public int integer;
		}
		class MyProjection {
			public final int integer;

			@ProjectionConstructor
			public MyProjection(@FieldProjection int integer) {
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
						Arrays.asList( 1 ),
						Arrays.asList( 2 )
				),
				f -> f.composite()
						.from(
								dummyProjectionForEnclosingClassInstance( f ),
								f.field( "integer", Integer.class )
						)
						.asList(),
				Arrays.asList(
						new MyProjection( 1 ),
						new MyProjection( 2 )
				)
		);
	}

	@Test
	void inObjectField() {
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

			@ProjectionConstructor
			public MyInnerProjection(@FieldProjection String text) {
				this.text = text;
			}
		}
		class MyProjection {
			public final String text;
			public final MyInnerProjection contained;

			@ProjectionConstructor
			public MyProjection(@FieldProjection String text, MyInnerProjection contained) {
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
						Arrays.asList( "result1", Arrays.asList( "result1_1" ) ),
						Arrays.asList( "result2", Arrays.asList( (Object) null ) ),
						Arrays.asList( "result3", null )
				),
				f -> f.composite()
						.from(
								dummyProjectionForEnclosingClassInstance( f ),
								f.field( "text", String.class ),
								f.object( "contained" )
										.from(
												dummyProjectionForEnclosingClassInstance( f ),
												f.field( "contained.text", String.class )
										)
										.asList()
						)
						.asList(),
				Arrays.asList(
						new MyProjection( "result1", new MyInnerProjection( "result1_1" ) ),
						new MyProjection( "result2", new MyInnerProjection( null ) ),
						new MyProjection( "result3", null )
				)
		);
	}

	@Test
	void inObjectField_filteredOut() {
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
			public MyInnerProjection(@FieldProjection String text, @FieldProjection Integer integer) {
				this.text = text;
				this.integer = integer;
			}
		}
		class MyProjection {
			public final String text;
			public final MyInnerProjection contained;

			@ProjectionConstructor
			public MyProjection(@FieldProjection String text,
					@ObjectProjection(includePaths = { "text" }) MyInnerProjection contained) {
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
						Arrays.asList( "result1", Arrays.asList( "result1_1", null ) ),
						Arrays.asList( "result2", null )
				),
				f -> f.composite()
						.from(
								dummyProjectionForEnclosingClassInstance( f ),
								f.field( "text", String.class ),
								f.object( "contained" )
										.from(
												dummyProjectionForEnclosingClassInstance( f ),
												f.field( "contained.text", String.class ),
												// "contained.integer" got filtered out due to @ObjectProjection filters
												f.constant( null )
										)
										.asList()
						)
						.asList(),
				Arrays.asList(
						new MyProjection( "result1", new MyInnerProjection( "result1_1", null ) ),
						new MyProjection( "result2", null )
				)
		);
	}

	@Test
	void inObjectField_multiValued_filteredOut() {
		class Contained {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public List<Integer> integer;
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
			public final List<Integer> integer;

			@ProjectionConstructor
			public MyInnerProjection(@FieldProjection String text, @FieldProjection List<Integer> integer) {
				this.text = text;
				this.integer = integer;
			}
		}
		class MyProjection {
			public final String text;
			public final MyInnerProjection contained;

			@ProjectionConstructor
			public MyProjection(@FieldProjection String text,
					@ObjectProjection(includePaths = { "text" }) MyInnerProjection contained) {
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
						Arrays.asList( "result1", Arrays.asList( "result1_1", null ) ),
						Arrays.asList( "result2", null )
				),
				f -> f.composite()
						.from(
								dummyProjectionForEnclosingClassInstance( f ),
								f.field( "text", String.class ),
								f.object( "contained" )
										.from(
												dummyProjectionForEnclosingClassInstance( f ),
												f.field( "contained.text", String.class ),
												// "contained.integer" got filtered out due to @ObjectProjection filters
												f.constant( Collections.emptyList() )
										)
										.asList()
						)
						.asList(),
				Arrays.asList(
						new MyProjection( "result1", new MyInnerProjection( "result1_1", Collections.emptyList() ) ),
						new MyProjection( "result2", null )
				)
		);
	}

	@Test
	void multiValued_list() {
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
			public MyProjection(@FieldProjection List<String> text, @FieldProjection List<Integer> integer) {
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
	void multiValued_collection() {
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
			public MyProjection(@FieldProjection Collection<String> text, @FieldProjection Collection<Integer> integer) {
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
	void multiValued_iterable() {
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
			public MyProjection(@FieldProjection Iterable<String> text, @FieldProjection Iterable<Integer> integer) {
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
	void multiValued_set() {
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
			public MyProjection(@FieldProjection Set<String> text, @FieldProjection List<Integer> integer) {
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
						.constructorContext( ProjectionConstructorFieldProjectionIT.class, Set.class, List.class )
						.methodParameterContext( 1, "text" )
						.failure( "Invalid parameter type for projection constructor",
								"java.util.Set<java.lang.String>",
								"When inferring the cardinality of inner projections from constructor parameters,"
										+ " multi-valued constructor parameters must be lists (java.util.List<...>)"
										+ " or list supertypes (java.lang.Iterable<...>, java.util.Collection<...>)" ) );
	}

}
