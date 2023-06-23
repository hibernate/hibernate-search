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
import java.util.List;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.CompositeProjection;
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

import org.junit.Rule;
import org.junit.Test;

@TestForIssue(jiraKey = "HSEARCH-4574")
public class ProjectionConstructorCompositeProjectionIT extends AbstractProjectionConstructorIT {

	@Rule
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Test
	public void noArg() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@FullTextField
			public String text2;
			@GenericField
			public Integer integer;
		}
		class MyInnerProjection {
			public final String text2;
			public final Integer integer;

			@ProjectionConstructor
			public MyInnerProjection(String text2, Integer integer) {
				this.text2 = text2;
				this.integer = integer;
			}
		}
		class MyProjection {
			public final String text;
			public final MyInnerProjection composite;

			@ProjectionConstructor
			public MyProjection(String text, @CompositeProjection MyInnerProjection composite) {
				this.text = text;
				this.composite = composite;
			}
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( MyProjection.class, MyInnerProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, MyProjection.class,
				Arrays.asList(
						Arrays.asList( "result1", Arrays.asList( "result1_1", 1 ) ),
						Arrays.asList( "result2", Arrays.asList( null, null ) )
				),
				f -> f.composite()
						.from(
								dummyProjectionForEnclosingClassInstance( f ),
								f.field( "text", String.class ),
								f.composite()
										.from(
												dummyProjectionForEnclosingClassInstance( f ),
												f.field( "text2", String.class ),
												f.field( "integer", Integer.class )
										)
										.asList()
						)
						.asList(),
				Arrays.asList(
						new MyProjection( "result1", new MyInnerProjection( "result1_1", 1 ) ),
						new MyProjection( "result2", new MyInnerProjection( null, null ) )
				)
		);
	}

	@Test
	public void invalidType() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@FullTextField
			public String text2;
		}
		class MyNonProjection {
			public final String text2;
			public final Integer integer;

			public MyNonProjection() {
				this.text2 = "foo";
				this.integer = 42;
			}

			public MyNonProjection(String text2, Integer integer) {
				this.text2 = text2;
				this.integer = integer;
			}
		}
		class MyProjection {
			public final MyNonProjection composite;

			@ProjectionConstructor
			public MyProjection(@CompositeProjection MyNonProjection composite) {
				this.composite = composite;
			}
		}

		assertThatThrownBy( () -> setupHelper.start()
				.withAnnotatedTypes( MyProjection.class, MyNonProjection.class )
				.setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( MyProjection.class.getName() )
						.constructorContext( ProjectionConstructorCompositeProjectionIT.class, MyNonProjection.class )
						.methodParameterContext( 1, "composite" )
						.failure( "Invalid object class for projection",
								MyNonProjection.class.getName(),
								"Make sure that this class is mapped correctly, "
										+ "either through annotations (@ProjectionConstructor) or programmatic mapping" ) );
	}

	@Test
	public void inObjectField() {
		class Contained {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@FullTextField
			public String text2;
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
		class MyInnerProjectionLevel2 {
			public final String text2;
			public final Integer integer;

			@ProjectionConstructor
			public MyInnerProjectionLevel2(String text2, Integer integer) {
				this.text2 = text2;
				this.integer = integer;
			}
		}
		class MyInnerProjectionLevel1 {
			public final String text;
			public final MyInnerProjectionLevel2 composite;

			@ProjectionConstructor
			public MyInnerProjectionLevel1(String text, @CompositeProjection MyInnerProjectionLevel2 composite) {
				this.text = text;
				this.composite = composite;
			}
		}
		class MyProjection {
			public final String text;
			public final MyInnerProjectionLevel1 contained;

			@ProjectionConstructor
			public MyProjection(String text, MyInnerProjectionLevel1 contained) {
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
						Arrays.asList( "result1", Arrays.asList( "result1_1", Arrays.asList( "result1_1_1", 1 ) ) ),
						Arrays.asList( "result2", Arrays.asList( null, null ) ),
						Arrays.asList( "result3", Arrays.asList( "result3_1", Arrays.asList( null, null ) ) )
				),
				f -> f.composite()
						.from(
								dummyProjectionForEnclosingClassInstance( f ),
								f.field( "text", String.class ),
								f.object( "contained" )
										.from(
												dummyProjectionForEnclosingClassInstance( f ),
												f.field( "contained.text", String.class ),
												f.composite()
														.from(
																dummyProjectionForEnclosingClassInstance( f ),
																f.field( "contained.text2", String.class ),
																f.field( "contained.integer", Integer.class )
														)
														.asList()
										)
										.asList()
						)
						.asList(),
				Arrays.asList(
						new MyProjection( "result1", new MyInnerProjectionLevel1( "result1_1",
								new MyInnerProjectionLevel2( "result1_1_1", 1 ) ) ),
						new MyProjection( "result2", new MyInnerProjectionLevel1( null, null ) ),
						new MyProjection( "result3", new MyInnerProjectionLevel1( "result3_1",
								new MyInnerProjectionLevel2( null, null ) ) )
				)
		);
	}

	@Test
	public void multiValued() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@FullTextField
			public String text2;
			@GenericField
			public Integer integer;
		}
		class MyInnerProjection {
			public final String text2;
			public final Integer integer;

			@ProjectionConstructor
			public MyInnerProjection(String text2, Integer integer) {
				this.text2 = text2;
				this.integer = integer;
			}
		}
		class MyProjection {
			public final String text;
			public final List<MyInnerProjection> composite;

			@ProjectionConstructor
			public MyProjection(String text, @CompositeProjection List<MyInnerProjection> composite) {
				this.text = text;
				this.composite = composite;
			}
		}

		assertThatThrownBy( () -> setupHelper.start()
				.withAnnotatedTypes( MyProjection.class )
				.setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( MyProjection.class.getName() )
						.constructorContext( ProjectionConstructorCompositeProjectionIT.class, String.class, List.class )
						.methodParameterContext( 2, "composite" )
						.failure( "Invalid object class for projection",
								List.class.getName(),
								"Make sure that this class is mapped correctly, "
										+ "either through annotations (@ProjectionConstructor) or programmatic mapping" ) );
	}
}
