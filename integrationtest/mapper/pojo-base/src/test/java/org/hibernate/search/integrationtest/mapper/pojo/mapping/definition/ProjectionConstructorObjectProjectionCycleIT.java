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
		}

		assertThatThrownBy( () -> setupHelper.start()
				.withAnnotatedTypes( ActualCycleDirectProjectionLevel1.class, ActualCycleDirectProjectionLevel2.class )
				.setup( Model.Level1.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( ActualCycleDirectProjectionLevel1.class.getName() )
						.constructorContext( String.class, ActualCycleDirectProjectionLevel2.class )
						.methodParameterContext( 1, "level2" )
						.typeContext( ActualCycleDirectProjectionLevel2.class.getName() )
						.constructorContext( String.class, ActualCycleDirectProjectionLevel1.class )
						.methodParameterContext( 1, "level1" )
						.typeContext( ActualCycleDirectProjectionLevel1.class.getName() )
						.constructorContext( String.class, ActualCycleDirectProjectionLevel2.class )
						.methodParameterContext( 1, "level2" )
						.multilineFailure( "Cyclic recursion starting from 'ObjectProjectionBinder(...)'",
								"on type '" + ActualCycleDirectProjectionLevel1.class.getName()
										+ "', projection constructor, parameter at index 1 (level2)",
								"Index field path starting from that location and ending with a cycle: 'level2.level1.level2.'",
								"A projection constructor cannot declare an unrestricted @ObjectProjection to itself, even indirectly",
								"To break the cycle, you should consider adding filters to your @ObjectProjection: includePaths, includeDepth, excludePaths, ..." ) );
	}

	static class ActualCycleDirectProjectionLevel1 {
		public final String text;
		public final ActualCycleDirectProjectionLevel2 level2;

		@ProjectionConstructor
		public ActualCycleDirectProjectionLevel1(String text, ActualCycleDirectProjectionLevel2 level2) {
			this.text = text;
			this.level2 = level2;
		}
	}

	static class ActualCycleDirectProjectionLevel2 {
		public final String text;
		public final ActualCycleDirectProjectionLevel1 level1;

		@ProjectionConstructor
		public ActualCycleDirectProjectionLevel2(String text, ActualCycleDirectProjectionLevel1 level1) {
			this.text = text;
			this.level1 = level1;
		}
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
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( BrokenCycleIncludePathsProjectionLevel1.class,
						BrokenCycleIncludePathsProjectionLevel2.class )
				.setup( Model.Level1.class );

		testSuccessfulRootProjectionExecutionOnly(
				mapping, Model.Level1.class, BrokenCycleIncludePathsProjectionLevel1.class,
				Arrays.asList(
						Arrays.asList( "result1",
								Arrays.asList( "result1_level2",
										Arrays.asList( "result1_level2_level1", null ) ) ),
						Arrays.asList( "result2",
								Arrays.asList( "result2_level2",
										Arrays.asList( "result2_level2_level1", null ) ) )
				),
				Arrays.asList(
						new BrokenCycleIncludePathsProjectionLevel1( "result1",
								new BrokenCycleIncludePathsProjectionLevel2( "result1_level2",
										new BrokenCycleIncludePathsProjectionLevel1( "result1_level2_level1", null ) ) ),
						new BrokenCycleIncludePathsProjectionLevel1( "result2",
								new BrokenCycleIncludePathsProjectionLevel2( "result2_level2",
										new BrokenCycleIncludePathsProjectionLevel1( "result2_level2_level1", null ) ) )
				)
		);
	}

	static class BrokenCycleIncludePathsProjectionLevel1 {
		public final String text;
		public final BrokenCycleIncludePathsProjectionLevel2 level2;

		@ProjectionConstructor
		public BrokenCycleIncludePathsProjectionLevel1(String text,
				@ObjectProjection(includePaths = { "text", "level1.text" }) BrokenCycleIncludePathsProjectionLevel2 level2) {
			this.text = text;
			this.level2 = level2;
		}
	}

	static class BrokenCycleIncludePathsProjectionLevel2 {
		public final String text;
		public final BrokenCycleIncludePathsProjectionLevel1 level1;

		@ProjectionConstructor
		public BrokenCycleIncludePathsProjectionLevel2(String text, BrokenCycleIncludePathsProjectionLevel1 level1) {
			this.text = text;
			this.level1 = level1;
		}
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
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( BrokenCycleExcludePathsProjectionLevel1.class,
						BrokenCycleExcludePathsProjectionLevel2.class )
				.setup( Model.Level1.class );

		testSuccessfulRootProjectionExecutionOnly(
				mapping, Model.Level1.class, BrokenCycleExcludePathsProjectionLevel1.class,
				Arrays.asList(
						Arrays.asList( "result1",
								Arrays.asList( "result1_level2",
										Arrays.asList( "result1_level2_level1", null ) ) ),
						Arrays.asList( "result2",
								Arrays.asList( "result2_level2",
										Arrays.asList( "result2_level2_level1", null ) ) )
				),
				Arrays.asList(
						new BrokenCycleExcludePathsProjectionLevel1( "result1",
								new BrokenCycleExcludePathsProjectionLevel2( "result1_level2",
										new BrokenCycleExcludePathsProjectionLevel1( "result1_level2_level1", null ) ) ),
						new BrokenCycleExcludePathsProjectionLevel1( "result2",
								new BrokenCycleExcludePathsProjectionLevel2( "result2_level2",
										new BrokenCycleExcludePathsProjectionLevel1( "result2_level2_level1", null ) ) )
				)
		);
	}

	static class BrokenCycleExcludePathsProjectionLevel1 {
		public final String text;
		public final BrokenCycleExcludePathsProjectionLevel2 level2;

		@ProjectionConstructor
		public BrokenCycleExcludePathsProjectionLevel1(String text,
				@ObjectProjection(excludePaths = { "level1.level2" }) BrokenCycleExcludePathsProjectionLevel2 level2) {
			this.text = text;
			this.level2 = level2;
		}
	}

	static class BrokenCycleExcludePathsProjectionLevel2 {
		public final String text;
		public final BrokenCycleExcludePathsProjectionLevel1 level1;

		@ProjectionConstructor
		public BrokenCycleExcludePathsProjectionLevel2(String text, BrokenCycleExcludePathsProjectionLevel1 level1) {
			this.text = text;
			this.level1 = level1;
		}
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
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( BrokenCycleIncludeDepthProjectionLevel1.class,
						BrokenCycleIncludeDepthProjectionLevel2.class )
				.setup( Model.Level1.class );

		testSuccessfulRootProjectionExecutionOnly(
				mapping, Model.Level1.class, BrokenCycleIncludeDepthProjectionLevel1.class,
				Arrays.asList(
						Arrays.asList( "result1",
								Arrays.asList( "result1_level2",
										Arrays.asList( "result1_level2_level1", null ) ) ),
						Arrays.asList( "result2",
								Arrays.asList( "result2_level2",
										Arrays.asList( "result2_level2_level1", null ) ) )
				),
				Arrays.asList(
						new BrokenCycleIncludeDepthProjectionLevel1( "result1",
								new BrokenCycleIncludeDepthProjectionLevel2( "result1_level2",
										new BrokenCycleIncludeDepthProjectionLevel1( "result1_level2_level1", null ) ) ),
						new BrokenCycleIncludeDepthProjectionLevel1( "result2",
								new BrokenCycleIncludeDepthProjectionLevel2( "result2_level2",
										new BrokenCycleIncludeDepthProjectionLevel1( "result2_level2_level1", null ) ) )
				)
		);
	}

	static class BrokenCycleIncludeDepthProjectionLevel1 {
		public final String text;
		public final BrokenCycleIncludeDepthProjectionLevel2 level2;

		@ProjectionConstructor
		public BrokenCycleIncludeDepthProjectionLevel1(String text,
				@ObjectProjection(includeDepth = 2) BrokenCycleIncludeDepthProjectionLevel2 level2) {
			this.text = text;
			this.level2 = level2;
		}
	}

	static class BrokenCycleIncludeDepthProjectionLevel2 {
		public final String text;
		public final BrokenCycleIncludeDepthProjectionLevel1 level1;

		@ProjectionConstructor
		public BrokenCycleIncludeDepthProjectionLevel2(String text, BrokenCycleIncludeDepthProjectionLevel1 level1) {
			this.text = text;
			this.level1 = level1;
		}
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
		}

		assertThatThrownBy( () -> setupHelper.start()
				.withAnnotatedTypes( ActualCycleIndirectNoFilterProjectionLevel1.class,
						ActualCycleIndirectNoFilterProjectionLevel2.class )
				.setup( Model.Level1.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( ActualCycleIndirectNoFilterProjectionLevel1.class.getName() )
						.constructorContext( String.class, ActualCycleIndirectNoFilterProjectionLevel2.class )
						.methodParameterContext( 1, "level2" )
						.typeContext( ActualCycleIndirectNoFilterProjectionLevel2.class.getName() )
						.constructorContext( String.class, ActualCycleIndirectNoFilterProjectionLevel3.class )
						.methodParameterContext( 1, "level3" )
						.typeContext( ActualCycleIndirectNoFilterProjectionLevel3.class.getName() )
						.constructorContext( String.class, ActualCycleIndirectNoFilterProjectionLevel1.class )
						.methodParameterContext( 1, "level1" )
						.typeContext( ActualCycleIndirectNoFilterProjectionLevel1.class.getName() )
						.constructorContext( String.class, ActualCycleIndirectNoFilterProjectionLevel2.class )
						.methodParameterContext( 1, "level2" )
						.multilineFailure( "Cyclic recursion starting from 'ObjectProjectionBinder(...)'",
								"on type '" + ActualCycleIndirectNoFilterProjectionLevel1.class.getName()
										+ "', projection constructor, parameter at index 1 (level2)",
								"Index field path starting from that location and ending with a cycle: 'level2.level3.level1.level2.'",
								"A projection constructor cannot declare an unrestricted @ObjectProjection to itself, even indirectly",
								"To break the cycle, you should consider adding filters to your @ObjectProjection: includePaths, includeDepth, excludePaths, ..." ) );
	}

	static class ActualCycleIndirectNoFilterProjectionLevel1 {
		public final String text;
		public final ActualCycleIndirectNoFilterProjectionLevel2 level2;

		@ProjectionConstructor
		public ActualCycleIndirectNoFilterProjectionLevel1(String text, ActualCycleIndirectNoFilterProjectionLevel2 level2) {
			this.text = text;
			this.level2 = level2;
		}
	}

	static class ActualCycleIndirectNoFilterProjectionLevel2 {
		public final String text;
		public final ActualCycleIndirectNoFilterProjectionLevel3 level3;

		@ProjectionConstructor
		public ActualCycleIndirectNoFilterProjectionLevel2(String text, ActualCycleIndirectNoFilterProjectionLevel3 level3) {
			this.text = text;
			this.level3 = level3;
		}
	}

	static class ActualCycleIndirectNoFilterProjectionLevel3 {
		public final String text;
		public final ActualCycleIndirectNoFilterProjectionLevel1 level1;

		@ProjectionConstructor
		public ActualCycleIndirectNoFilterProjectionLevel3(String text, ActualCycleIndirectNoFilterProjectionLevel1 level1) {
			this.text = text;
			this.level1 = level1;
		}
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
		}

		assertThatThrownBy( () -> setupHelper.start()
				.withAnnotatedTypes( ActualCycleBuriedNoFilterProjectionLevel1.class,
						ActualCycleBuriedNoFilterProjectionLevel2.class )
				.setup( Model.Level1.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( ActualCycleBuriedNoFilterProjectionLevel1.class.getName() )
						.constructorContext( String.class, ActualCycleBuriedNoFilterProjectionLevel2.class )
						.methodParameterContext( 1, "level2" )
						.typeContext( ActualCycleBuriedNoFilterProjectionLevel2.class.getName() )
						.constructorContext( String.class, ActualCycleBuriedNoFilterProjectionLevel3.class )
						.methodParameterContext( 1, "level3" )
						.typeContext( ActualCycleBuriedNoFilterProjectionLevel3.class.getName() )
						.constructorContext( String.class, ActualCycleBuriedNoFilterProjectionLevel2.class )
						.methodParameterContext( 1, "level2" )
						.typeContext( ActualCycleBuriedNoFilterProjectionLevel2.class.getName() )
						.constructorContext( String.class, ActualCycleBuriedNoFilterProjectionLevel3.class )
						.methodParameterContext( 1, "level3" )
						.multilineFailure( "Cyclic recursion starting from 'ObjectProjectionBinder(...)'",
								"on type '" + ActualCycleBuriedNoFilterProjectionLevel2.class.getName()
										+ "', projection constructor, parameter at index 1 (level3)",
								"Index field path starting from that location and ending with a cycle: 'level3.level2.level3.'",
								"A projection constructor cannot declare an unrestricted @ObjectProjection to itself, even indirectly",
								"To break the cycle, you should consider adding filters to your @ObjectProjection: includePaths, includeDepth, excludePaths, ..." ) );
	}

	static class ActualCycleBuriedNoFilterProjectionLevel1 {
		public final String text;
		public final ActualCycleBuriedNoFilterProjectionLevel2 level2;

		@ProjectionConstructor
		public ActualCycleBuriedNoFilterProjectionLevel1(String text, ActualCycleBuriedNoFilterProjectionLevel2 level2) {
			this.text = text;
			this.level2 = level2;
		}
	}

	static class ActualCycleBuriedNoFilterProjectionLevel2 {
		public final String text;
		public final ActualCycleBuriedNoFilterProjectionLevel3 level3;

		@ProjectionConstructor
		public ActualCycleBuriedNoFilterProjectionLevel2(String text, ActualCycleBuriedNoFilterProjectionLevel3 level3) {
			this.text = text;
			this.level3 = level3;
		}
	}

	static class ActualCycleBuriedNoFilterProjectionLevel3 {
		public final String text;
		public final ActualCycleBuriedNoFilterProjectionLevel2 level2;

		@ProjectionConstructor
		public ActualCycleBuriedNoFilterProjectionLevel3(String text, ActualCycleBuriedNoFilterProjectionLevel2 level2) {
			this.text = text;
			this.level2 = level2;
		}
	}
}
