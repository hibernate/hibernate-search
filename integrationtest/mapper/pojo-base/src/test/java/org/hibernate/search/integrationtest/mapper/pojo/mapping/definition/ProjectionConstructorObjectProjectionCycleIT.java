/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
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

@TestForIssue(jiraKey = "HSEARCH-4725")
class ProjectionConstructorObjectProjectionCycleIT extends AbstractProjectionConstructorIT {

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock )
					// We don't care about reindexing here and don't want to configure association inverse sides
					.disableAssociationReindexing();

	@Test
	void actualCycle_direct() {
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
						.methodParameterContext( 2, "level2" )
						.multilineFailure( "Cyclic recursion starting from 'ObjectProjectionBinder(...)'",
								"on type '" + Model.ProjectionLevel1.class.getName()
										+ "', projection constructor, parameter at index 2 (level2)",
								"Index field path starting from that location and ending with a cycle: 'level2.level1.level2.'",
								"A projection constructor cannot declare an unrestricted @ObjectProjection to itself, even indirectly",
								"To break the cycle, you should consider adding filters to your @ObjectProjection: includePaths, includeDepth, excludePaths, ..." ) );
	}

	@Test
	void brokenCycle_includePaths() {
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
				public ProjectionLevel1(String text,
						@ObjectProjection(includePaths = { "text", "level1.text" }) ProjectionLevel2 level2) {
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

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( Model.ProjectionLevel1.class, Model.ProjectionLevel2.class )
				.setup( Model.Level1.class );

		Model model = new Model();

		testSuccessfulRootProjectionExecutionOnly(
				mapping, Model.Level1.class, Model.ProjectionLevel1.class,
				Arrays.asList(
						Arrays.asList( "result1",
								Arrays.asList( "result1_level2",
										Arrays.asList( "result1_level2_level1", null ) ) ),
						Arrays.asList( "result2",
								Arrays.asList( "result2_level2",
										Arrays.asList( "result2_level2_level1", null ) ) )
				),
				Arrays.asList(
						model.new ProjectionLevel1( "result1",
								model.new ProjectionLevel2( "result1_level2",
										model.new ProjectionLevel1( "result1_level2_level1", null ) ) ),
						model.new ProjectionLevel1( "result2",
								model.new ProjectionLevel2( "result2_level2",
										model.new ProjectionLevel1( "result2_level2_level1", null ) ) )
				)
		);
	}

	@Test
	void brokenCycle_excludePaths() {
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
				public ProjectionLevel1(String text,
						@ObjectProjection(excludePaths = { "level1.level2" }) ProjectionLevel2 level2) {
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

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( Model.ProjectionLevel1.class, Model.ProjectionLevel2.class )
				.setup( Model.Level1.class );

		Model model = new Model();

		testSuccessfulRootProjectionExecutionOnly(
				mapping, Model.Level1.class, Model.ProjectionLevel1.class,
				Arrays.asList(
						Arrays.asList( "result1",
								Arrays.asList( "result1_level2",
										Arrays.asList( "result1_level2_level1", null ) ) ),
						Arrays.asList( "result2",
								Arrays.asList( "result2_level2",
										Arrays.asList( "result2_level2_level1", null ) ) )
				),
				Arrays.asList(
						model.new ProjectionLevel1( "result1",
								model.new ProjectionLevel2( "result1_level2",
										model.new ProjectionLevel1( "result1_level2_level1", null ) ) ),
						model.new ProjectionLevel1( "result2",
								model.new ProjectionLevel2( "result2_level2",
										model.new ProjectionLevel1( "result2_level2_level1", null ) ) )
				)
		);
	}

	@Test
	void brokenCycle_includeDepth() {
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
				public ProjectionLevel1(String text,
						@ObjectProjection(includeDepth = 2) ProjectionLevel2 level2) {
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

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( Model.ProjectionLevel1.class, Model.ProjectionLevel2.class )
				.setup( Model.Level1.class );

		Model model = new Model();

		testSuccessfulRootProjectionExecutionOnly(
				mapping, Model.Level1.class, Model.ProjectionLevel1.class,
				Arrays.asList(
						Arrays.asList( "result1",
								Arrays.asList( "result1_level2",
										Arrays.asList( "result1_level2_level1", null ) ) ),
						Arrays.asList( "result2",
								Arrays.asList( "result2_level2",
										Arrays.asList( "result2_level2_level1", null ) ) )
				),
				Arrays.asList(
						model.new ProjectionLevel1( "result1",
								model.new ProjectionLevel2( "result1_level2",
										model.new ProjectionLevel1( "result1_level2_level1", null ) ) ),
						model.new ProjectionLevel1( "result2",
								model.new ProjectionLevel2( "result2_level2",
										model.new ProjectionLevel1( "result2_level2_level1", null ) ) )
				)
		);
	}

	@Test
	void actualCycle_indirect_noFilter() {
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
						.methodParameterContext( 2, "level2" )
						.multilineFailure( "Cyclic recursion starting from 'ObjectProjectionBinder(...)'",
								"on type '" + Model.ProjectionLevel1.class.getName()
										+ "', projection constructor, parameter at index 2 (level2)",
								"Index field path starting from that location and ending with a cycle: 'level2.level3.level1.level2.'",
								"A projection constructor cannot declare an unrestricted @ObjectProjection to itself, even indirectly",
								"To break the cycle, you should consider adding filters to your @ObjectProjection: includePaths, includeDepth, excludePaths, ..." ) );
	}

	@Test
	void actualCycle_buried_noFilter() {
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
						.methodParameterContext( 2, "level3" )
						.multilineFailure( "Cyclic recursion starting from 'ObjectProjectionBinder(...)'",
								"on type '" + Model.ProjectionLevel2.class.getName()
										+ "', projection constructor, parameter at index 2 (level3)",
								"Index field path starting from that location and ending with a cycle: 'level3.level2.level3.'",
								"A projection constructor cannot declare an unrestricted @ObjectProjection to itself, even indirectly",
								"To break the cycle, you should consider adding filters to your @ObjectProjection: includePaths, includeDepth, excludePaths, ..." ) );
	}

}
