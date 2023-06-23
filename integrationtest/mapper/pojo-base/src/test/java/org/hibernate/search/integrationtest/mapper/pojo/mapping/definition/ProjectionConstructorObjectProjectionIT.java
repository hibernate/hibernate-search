/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

@TestForIssue(jiraKey = "HSEARCH-4574")
public class ProjectionConstructorObjectProjectionIT extends AbstractProjectionConstructorIT {

	@Rule
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock )
					// We don't care about reindexing here and don't want to configure association inverse sides
					.disableAssociationReindexing();

	@Test
	public void noArg() {
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
			public MyInnerProjection(String text) {
				this.text = text;
			}
		}
		class MyProjection {
			public final String text;
			public final MyInnerProjection contained;

			@ProjectionConstructor
			public MyProjection(String text, @ObjectProjection MyInnerProjection contained) {
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
	public void path() {
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
			@IndexedEmbedded(name = "myContained")
			public Contained contained;
		}
		class MyInnerProjection {
			public final String text;

			@ProjectionConstructor
			public MyInnerProjection(String text) {
				this.text = text;
			}
		}
		class MyProjection {
			public final String text;
			public final MyInnerProjection contained;

			@ProjectionConstructor
			public MyProjection(String text,
					@ObjectProjection(path = "myContained") MyInnerProjection contained) {
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
								f.object( "myContained" )
										.from(
												dummyProjectionForEnclosingClassInstance( f ),
												f.field( "myContained.text", String.class )
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
	public void invalidType() {
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
		class MyProjection {
			public final MyNonProjection contained;

			@ProjectionConstructor
			public MyProjection(@ObjectProjection MyNonProjection contained) {
				this.contained = contained;
			}
		}

		assertThatThrownBy( () -> setupHelper.start()
				.withAnnotatedTypes( MyProjection.class, MyNonProjection.class )
				.setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( MyProjection.class.getName() )
						.constructorContext( ProjectionConstructorObjectProjectionIT.class, MyNonProjection.class )
						.methodParameterContext( 1, "contained" )
						.failure( "Invalid object class for projection",
								MyNonProjection.class.getName(),
								"Make sure that this class is mapped correctly, "
										+ "either through annotations (@ProjectionConstructor) or programmatic mapping" ) );
	}

	@Test
	public void nonMatchingIncludePaths() {
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
			public MyInnerProjection(String text) {
				this.text = text;
			}
		}
		class MyProjection {
			public final String text;
			public final MyInnerProjection contained;

			@ProjectionConstructor
			public MyProjection(String text,
					@ObjectProjection(includePaths = "doesNotExist") MyInnerProjection contained) {
				this.text = text;
				this.contained = contained;
			}
		}

		assertThatThrownBy( () -> setupHelper.start()
				.withAnnotatedTypes( MyProjection.class, MyInnerProjection.class )
				.setup( IndexedEntity.class, Contained.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( MyProjection.class.getName() )
						.projectionConstructorContext()
						.methodParameterContext( 2, "contained" )
						.failure(
								"ObjectProjectionBinder(...) defines includePaths filters that do not match anything",
								"Non-matching includePaths filters: [doesNotExist].",
								"Encountered field paths: [text].",
								"Check the filters for typos, or remove them if they are not useful."
						)
				);
	}

	@Test
	public void nonMatchingExcludePaths() {
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
			public MyInnerProjection(String text) {
				this.text = text;
			}
		}
		class MyProjection {
			public final String text;
			public final MyInnerProjection contained;

			@ProjectionConstructor
			public MyProjection(String text,
					@ObjectProjection(excludePaths = "doesNotExist") MyInnerProjection contained) {
				this.text = text;
				this.contained = contained;
			}
		}

		assertThatThrownBy( () -> setupHelper.start()
				.withAnnotatedTypes( MyProjection.class, MyInnerProjection.class )
				.setup( IndexedEntity.class, Contained.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( MyProjection.class.getName() )
						.projectionConstructorContext()
						.methodParameterContext( 2, "contained" )
						.failure(
								"ObjectProjectionBinder(...) defines excludePaths filters that do not match anything",
								"Non-matching excludePaths filters: [doesNotExist].",
								"Encountered field paths: [text].",
								"Check the filters for typos, or remove them if they are not useful."
						)
				);
	}

	@Test
	public void inObjectField() {
		class ContainedLevel2 {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public Integer integer;
		}
		class ContainedLevel1 {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public Integer integer;
			@IndexedEmbedded
			public ContainedLevel2 contained;
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@IndexedEmbedded
			public ContainedLevel1 contained;
		}
		class MyInnerProjectionLevel2 {
			public final String text;

			@ProjectionConstructor
			public MyInnerProjectionLevel2(String text) {
				this.text = text;
			}
		}
		class MyInnerProjectionLevel1 {
			public final String text;
			public final MyInnerProjectionLevel2 contained;

			@ProjectionConstructor
			public MyInnerProjectionLevel1(String text,
					@ObjectProjection MyInnerProjectionLevel2 contained) {
				this.text = text;
				this.contained = contained;
			}
		}
		class MyProjection {
			public final String text;
			public final MyInnerProjectionLevel1 contained;

			@ProjectionConstructor
			public MyProjection(String text, @ObjectProjection MyInnerProjectionLevel1 contained) {
				this.text = text;
				this.contained = contained;
			}
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( MyProjection.class, MyInnerProjectionLevel1.class, MyInnerProjectionLevel2.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, MyProjection.class,
				Arrays.asList(
						Arrays.asList( "result1", Arrays.asList( "result1_1", Arrays.asList( "result1_1_1" ) ) ),
						Arrays.asList( "result2", Arrays.asList( null, null ) ),
						Arrays.asList( "result3", null ),
						Arrays.asList( "result4", Arrays.asList( "result4_1", Arrays.asList( (Object) null ) ) ),
						Arrays.asList( "result5", Arrays.asList( "result5_1", null ) )
				),
				f -> f.composite()
						.from(
								dummyProjectionForEnclosingClassInstance( f ),
								f.field( "text", String.class ),
								f.object( "contained" )
										.from(
												dummyProjectionForEnclosingClassInstance( f ),
												f.field( "contained.text", String.class ),
												f.object( "contained.contained" )
														.from(
																dummyProjectionForEnclosingClassInstance( f ),
																f.field( "contained.contained.text", String.class )
														)
														.asList()
										)
										.asList()
						)
						.asList(),
				Arrays.asList(
						new MyProjection( "result1", new MyInnerProjectionLevel1( "result1_1",
								new MyInnerProjectionLevel2( "result1_1_1" ) ) ),
						new MyProjection( "result2", new MyInnerProjectionLevel1( null, null ) ),
						new MyProjection( "result3", null ),
						new MyProjection( "result4", new MyInnerProjectionLevel1( "result4_1",
								new MyInnerProjectionLevel2( null ) ) ),
						new MyProjection( "result5", new MyInnerProjectionLevel1( "result5_1", null ) )
				)
		);
	}

	@Test
	public void inObjectField_filteredOut() {
		class ContainedLevel2 {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public Integer integer;
		}
		class ContainedLevel1 {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public Integer integer;
			@IndexedEmbedded
			public ContainedLevel2 contained;
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@IndexedEmbedded
			public ContainedLevel1 contained;
		}
		class MyInnerProjectionLevel2 {
			public final String text;

			@ProjectionConstructor
			public MyInnerProjectionLevel2(String text) {
				this.text = text;
			}
		}
		class MyInnerProjectionLevel1 {
			public final String text;
			public final MyInnerProjectionLevel2 contained;

			@ProjectionConstructor
			public MyInnerProjectionLevel1(String text,
					@ObjectProjection MyInnerProjectionLevel2 contained) {
				this.text = text;
				this.contained = contained;
			}
		}
		class MyProjection {
			public final String text;
			public final MyInnerProjectionLevel1 contained;

			@ProjectionConstructor
			public MyProjection(String text,
					@ObjectProjection(includeDepth = 1) MyInnerProjectionLevel1 contained) {
				this.text = text;
				this.contained = contained;
			}
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( MyProjection.class, MyInnerProjectionLevel1.class, MyInnerProjectionLevel2.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, MyProjection.class,
				Arrays.asList(
						Arrays.asList( "result1", Arrays.asList( "result1_1" ) ),
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
												// "contained.contained" got filtered out due to @ObjectProjection filters
												f.constant( null )
										)
										.asList()
						)
						.asList(),
				Arrays.asList(
						new MyProjection( "result1", new MyInnerProjectionLevel1( "result1_1", null ) ),
						new MyProjection( "result2", null )
				)
		);
	}

	@Test
	public void inObjectField_multiValued_filteredOut() {
		class ContainedLevel2 {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public Integer integer;
		}
		class ContainedLevel1 {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public Integer integer;
			@IndexedEmbedded
			public List<ContainedLevel2> contained;
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@IndexedEmbedded
			public ContainedLevel1 contained;
		}
		class MyInnerProjectionLevel2 {
			public final String text;

			@ProjectionConstructor
			public MyInnerProjectionLevel2(String text) {
				this.text = text;
			}
		}
		class MyInnerProjectionLevel1 {
			public final String text;
			public final List<MyInnerProjectionLevel2> contained;

			@ProjectionConstructor
			public MyInnerProjectionLevel1(String text,
					@ObjectProjection List<MyInnerProjectionLevel2> contained) {
				this.text = text;
				this.contained = contained;
			}
		}
		class MyProjection {
			public final String text;
			public final MyInnerProjectionLevel1 contained;

			@ProjectionConstructor
			public MyProjection(String text,
					@ObjectProjection(includeDepth = 1) MyInnerProjectionLevel1 contained) {
				this.text = text;
				this.contained = contained;
			}
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( MyProjection.class, MyInnerProjectionLevel1.class, MyInnerProjectionLevel2.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, MyProjection.class,
				Arrays.asList(
						Arrays.asList( "result1", Arrays.asList( "result1_1" ) ),
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
												// "contained.contained" got filtered out due to @ObjectProjection filters
												f.constant( Collections.emptyList() )
										)
										.asList()
						)
						.asList(),
				Arrays.asList(
						new MyProjection( "result1", new MyInnerProjectionLevel1( "result1_1", Collections.emptyList() ) ),
						new MyProjection( "result2", null )
				)
		);
	}

	@Test
	public void multiValued_list() {
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
			public MyProjection(String text, @ObjectProjection List<MyInnerProjection> contained) {
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
	public void multiValued_collection() {
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
			public MyProjection(String text, @ObjectProjection Collection<MyInnerProjection> contained) {
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
	public void multiValued_iterable() {
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
			public MyProjection(String text, @ObjectProjection Iterable<MyInnerProjection> contained) {
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
	public void multiValued_set() {
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
			public MyProjection(String text, @ObjectProjection Set<MyInnerProjection> contained) {
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
						.constructorContext( ProjectionConstructorObjectProjectionIT.class, String.class, Set.class )
						.methodParameterContext( 2, "contained" )
						.failure( "Invalid parameter type for projection constructor",
								"java.util.Set<" + MyInnerProjection.class.getName() + ">",
								"When inferring the cardinality of inner projections from constructor parameters,"
										+ " multi-valued constructor parameters must be lists (java.util.List<...>)"
										+ " or list supertypes (java.lang.Iterable<...>, java.util.Collection<...>)" ) );
	}
}
