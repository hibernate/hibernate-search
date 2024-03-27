/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.HighlightProjection;
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

@TestForIssue(jiraKey = "HSEARCH-4574")
class ProjectionConstructorHighlightProjectionIT extends AbstractProjectionConstructorIT {

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
			public final List<String> text;

			@ProjectionConstructor
			public MyProjection(@HighlightProjection List<String> text) {
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
						Arrays.asList( Arrays.asList( "result1_term1", "result1_term2" ) ),
						Arrays.asList( Arrays.asList( "result1_term1" ) )
				),
				f -> f.composite()
						.from(
								dummyProjectionForEnclosingClassInstance( f ),
								f.highlight( "text" )
						)
						.asList(),
				Arrays.asList(
						new MyProjection( Arrays.asList( "result1_term1", "result1_term2" ) ),
						new MyProjection( Arrays.asList( "result1_term1" ) )
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
			public final List<String> text;

			@ProjectionConstructor
			public MyProjection(@HighlightProjection(path = "myText") List<String> text) {
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
						Arrays.asList( Arrays.asList( "result1_term1", "result1_term2" ) ),
						Arrays.asList( Arrays.asList( "result1_term1" ) )
				),
				f -> f.composite()
						.from(
								dummyProjectionForEnclosingClassInstance( f ),
								f.highlight( "myText" )
						)
						.asList(),
				Arrays.asList(
						new MyProjection( Arrays.asList( "result1_term1", "result1_term2" ) ),
						new MyProjection( Arrays.asList( "result1_term1" ) )
				)
		);
	}

	@Test
	void highlighter() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
		}
		class MyProjection {
			public final List<String> text;

			@ProjectionConstructor
			public MyProjection(@HighlightProjection(highlighter = "foo") List<String> text) {
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
						Arrays.asList( Arrays.asList( "result1_term1", "result1_term2" ) ),
						Arrays.asList( Arrays.asList( "result1_term1" ) )
				),
				f -> f.composite()
						.from(
								dummyProjectionForEnclosingClassInstance( f ),
								f.highlight( "text" )
										.highlighter( "foo" )
						)
						.asList(),
				Arrays.asList(
						new MyProjection( Arrays.asList( "result1_term1", "result1_term2" ) ),
						new MyProjection( Arrays.asList( "result1_term1" ) )
				)
		);
	}

	@Test
	void superType() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
		}
		class MyProjection {
			public final List<CharSequence> text;

			@ProjectionConstructor
			public MyProjection(@HighlightProjection List<CharSequence> text) {
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
						Arrays.asList( Arrays.asList( "result1_term1", "result1_term2" ) ),
						Arrays.asList( Arrays.asList( "result1_term1" ) )
				),
				f -> f.composite()
						.from(
								dummyProjectionForEnclosingClassInstance( f ),
								f.highlight( "text" )
						)
						.asList(),
				Arrays.asList(
						new MyProjection( Arrays.asList( "result1_term1", "result1_term2" ) ),
						new MyProjection( Arrays.asList( "result1_term1" ) )
				)
		);
	}

	@Test
	void singleValued() {
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
			public MyProjection(@HighlightProjection String text) {
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
						Arrays.asList( "result1_term1" ),
						Arrays.asList( "result1_term1" )
				),
				f -> f.composite()
						.from(
								dummyProjectionForEnclosingClassInstance( f ),
								f.highlight( "text" ).single()
						)
						.asList(),
				Arrays.asList(
						new MyProjection( "result1_term1" ),
						new MyProjection( "result1_term1" )
				)
		);
	}

	// Technically this is not supported on the backend side,
	// but we'll test it just to be sure the mapper side works.
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
			public final List<String> text;

			@ProjectionConstructor
			public MyInnerProjection(@HighlightProjection List<String> text) {
				this.text = text;
			}
		}
		class MyProjection {
			public final List<String> text;
			public final MyInnerProjection contained;

			@ProjectionConstructor
			public MyProjection(@HighlightProjection List<String> text, MyInnerProjection contained) {
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
						Arrays.asList( Arrays.asList( "result1_term1", "result1_term2" ),
								Arrays.asList( Arrays.asList( "result1_term2", "result1_term3" ) ) ),
						Arrays.asList( Arrays.asList( "result2_term1" ), Arrays.asList( (Object) null ) ),
						Arrays.asList( Arrays.asList( "result3_term1" ), null )
				),
				f -> f.composite()
						.from(
								dummyProjectionForEnclosingClassInstance( f ),
								f.highlight( "text" ),
								f.object( "contained" )
										.from(
												dummyProjectionForEnclosingClassInstance( f ),
												f.highlight( "contained.text" )
										)
										.asList()
						)
						.asList(),
				Arrays.asList(
						new MyProjection( Arrays.asList( "result1_term1", "result1_term2" ),
								new MyInnerProjection( Arrays.asList( "result1_term2", "result1_term3" ) ) ),
						new MyProjection( Arrays.asList( "result2_term1" ), new MyInnerProjection( null ) ),
						new MyProjection( Arrays.asList( "result3_term1" ), null )
				)
		);
	}

	@Test
	void collection() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
		}
		class MyProjection {
			public final Collection<String> text;

			@ProjectionConstructor
			public MyProjection(@HighlightProjection Collection<String> text) {
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
						Arrays.asList( Arrays.asList( "result1_term1", "result1_term2" ) ),
						Arrays.asList( Arrays.asList( "result1_term1" ) )
				),
				f -> f.composite()
						.from(
								dummyProjectionForEnclosingClassInstance( f ),
								f.highlight( "text" )
						)
						.asList(),
				Arrays.asList(
						new MyProjection( Arrays.asList( "result1_term1", "result1_term2" ) ),
						new MyProjection( Arrays.asList( "result1_term1" ) )
				)
		);
	}

	@Test
	void iterable() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
		}
		class MyProjection {
			public final Iterable<String> text;

			@ProjectionConstructor
			public MyProjection(@HighlightProjection Iterable<String> text) {
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
						Arrays.asList( Arrays.asList( "result1_term1", "result1_term2" ) ),
						Arrays.asList( Arrays.asList( "result1_term1" ) )
				),
				f -> f.composite()
						.from(
								dummyProjectionForEnclosingClassInstance( f ),
								f.highlight( "text" )
						)
						.asList(),
				Arrays.asList(
						new MyProjection( Arrays.asList( "result1_term1", "result1_term2" ) ),
						new MyProjection( Arrays.asList( "result1_term1" ) )
				)
		);
	}

	@Test
	void set() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
		}
		class MyProjection {
			public final Set<String> text;

			@ProjectionConstructor
			public MyProjection(@HighlightProjection Set<String> text) {
				this.text = text;
			}
		}

		assertThatThrownBy( () -> setupHelper.start()
				.withAnnotatedTypes( MyProjection.class )
				.setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( MyProjection.class.getName() )
						.constructorContext( ProjectionConstructorHighlightProjectionIT.class, Set.class )
						.methodParameterContext( 1, "text" )
						.failure( "Invalid parameter type for projection constructor: java.util.Set<java.lang.String>",
								"When inferring the cardinality of inner projections from constructor parameters, multi-valued constructor parameters must be lists (java.util.List<...>) or list supertypes (java.lang.Iterable<...>, java.util.Collection<...>)" ) );
	}

	@Test
	void invalidElementType() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
		}
		class MyProjection {
			public final List<Integer> text;

			@ProjectionConstructor
			public MyProjection(@HighlightProjection List<Integer> text) {
				this.text = text;
			}
		}

		assertThatThrownBy( () -> setupHelper.start()
				.withAnnotatedTypes( MyProjection.class )
				.setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( MyProjection.class.getName() )
						.constructorContext( ProjectionConstructorHighlightProjectionIT.class, List.class )
						.methodParameterContext( 1, "text" )
						.failure( "Invalid multi-valued projection definition for constructor parameter type",
								"'java.util.List<java.lang.Integer>'",
								"This projection results in values of type 'java.lang.String'" ) );
	}

}
