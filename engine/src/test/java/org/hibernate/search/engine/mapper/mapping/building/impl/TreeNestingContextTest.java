/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.mapper.mapping.building.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.common.impl.CollectionHelper.asSet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

import org.hibernate.search.engine.common.tree.TreeFilterDefinition;
import org.hibernate.search.engine.common.tree.spi.TreeFilterPathTracker;
import org.hibernate.search.engine.common.tree.spi.TreeNestingContext;
import org.hibernate.search.engine.common.tree.spi.TreeNodeInclusion;
import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;
import org.hibernate.search.engine.mapper.model.spi.MappingElement;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class TreeNestingContextTest {
	private static final BiFunction<MappingElement, String, SearchException> CYCLIC_RECURSION_EXCEPTION_FACTORY =
			(mappingElement, cyclicRecursionPath) -> new SearchException(
					cyclicRecursionMessage( mappingElement, cyclicRecursionPath ) );

	private static String cyclicRecursionMessage(MappingElement mappingElement, String cyclicRecursionPath) {
		return "Cyclic recursion! Root = " + mappingElement.toString() + ", path = " + cyclicRecursionPath;
	}

	@Mock(strictness = Mock.Strictness.LENIENT)
	private MappableTypeModel typeModel1Mock;

	@Mock(strictness = Mock.Strictness.LENIENT)
	private MappableTypeModel typeModel2Mock;

	@Mock(strictness = Mock.Strictness.LENIENT)
	private MappableTypeModel typeModel3Mock;

	@Mock(strictness = Mock.Strictness.LENIENT)
	private MappableTypeModel typeModel4Mock;

	@Mock
	private TreeNestingContext.LeafFactory<Object> leafFactoryMock;

	@Mock
	private TreeNestingContext.CompositeFactory<Object> compositeFactoryMock;

	@Mock
	private TreeNestingContext.UnfilteredFactory<Object> unfilteredFactoryMock;

	@Mock
	private TreeNestingContext.NestedContextBuilder<Object> nestedContextBuilderMock;

	@BeforeEach
	void setup() {
		when( typeModel1Mock.name() ).thenReturn( "typeModel1Mock" );
		when( typeModel2Mock.name() ).thenReturn( "typeModel2Mock" );
		when( typeModel3Mock.name() ).thenReturn( "typeModel3Mock" );
		when( typeModel4Mock.name() ).thenReturn( "typeModel4Mock" );
	}

	@Test
	void noFilter() {
		TreeNestingContext rootContext = TreeNestingContext.root();

		checkFooBarIncluded( "", rootContext );

		TreeNestingContext level1Context =
				checkCompositeIncluded( "level1", rootContext, "level1" );

		checkFooBarIncluded( "", level1Context );
	}

	@Test
	void nestComposed_noFilter() {
		TreeNestingContext rootContext = TreeNestingContext.root();

		TreeNestingContext level1Context = checkSimpleComposedFilterIncluded(
				"level1", rootContext, typeModel1Mock, "level1.prefix1_",
				null, null, null
		);
		checkFooBarIncluded( "prefix1_", level1Context );

		// Check simple nesting (no filter composition)

		TreeNestingContext level2NonComposedContext =
				checkCompositeIncluded( "prefix1_level2", level1Context, "level2" );
		checkFooBarIncluded( "", level2NonComposedContext );

		// Check nesting with filter composition

		TreeNestingContext level2Context = checkSimpleComposedFilterIncluded(
				"prefix1_level2", level1Context, typeModel2Mock, "level2.prefix2_",
				null, null, null
		);
		checkFooBarIncluded( "prefix2_", level2Context );
	}

	@Test
	void nestComposed_noFilter_detectCycle_direct() {
		TreeNestingContext rootContext = TreeNestingContext.root();

		String relativePrefix = "level1.prefix1_";

		TreeNestingContext level1Context = checkSimpleComposedFilterIncluded(
				"level1", rootContext, typeModel1Mock, relativePrefix,
				null, null, null
		);
		checkFooBarIncluded( "prefix1_", level1Context );

		MappingElement level1MappingElement = new StubMappingElement( typeModel1Mock, relativePrefix );
		assertThatThrownBy( () -> {
			TreeFilterDefinition level1Definition = new TreeFilterDefinition(
					null, null, null
			);
			level1Context.nestComposed(
					level1MappingElement,
					relativePrefix, level1Definition,
					new TreeFilterPathTracker( level1Definition ),
					nestedContextBuilderMock, CYCLIC_RECURSION_EXCEPTION_FACTORY
			);
		} )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( cyclicRecursionMessage(
						level1MappingElement, "level1.prefix1_level1.prefix1_" ) );
		verifyNoOtherInteractionsAndReset();
	}

	@Test
	void nestComposed_noFilter_detectCycle_indirect() {
		TreeNestingContext rootContext = TreeNestingContext.root();

		String level1RelativePrefix = "level1.prefix1_";
		TreeNestingContext level1Context = checkSimpleComposedFilterIncluded(
				"level1", rootContext, typeModel1Mock, level1RelativePrefix,
				null, null, null
		);

		TreeNestingContext level2Context = checkSimpleComposedFilterIncluded(
				"prefix1_level2", level1Context, typeModel1Mock, "level2.prefix2_",
				null, null, null
		);

		MappingElement level1MappingElement = new StubMappingElement( typeModel1Mock, level1RelativePrefix );
		assertThatThrownBy( () -> {
			TreeFilterDefinition level1Definition = new TreeFilterDefinition(
					null, null, null
			);
			level2Context.nestComposed(
					level1MappingElement, level1RelativePrefix,
					level1Definition, new TreeFilterPathTracker( level1Definition ),
					nestedContextBuilderMock, CYCLIC_RECURSION_EXCEPTION_FACTORY
			);
		} )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( cyclicRecursionMessage(
						level1MappingElement, "level1.prefix1_level2.prefix2_level1.prefix1_" ) );
		verifyNoOtherInteractionsAndReset();
	}

	@Test
	void nestComposed_noFilter_multiLevelPrefix() {
		TreeNestingContext rootContext = TreeNestingContext.root();

		ArgumentCaptor<TreeNestingContext> nestedContextCapture =
				ArgumentCaptor.forClass( TreeNestingContext.class );

		Object expectedReturn;
		Optional<Object> actualReturn;

		expectedReturn = new Object();
		when( nestedContextBuilderMock.build( any() ) )
				.thenReturn( expectedReturn );
		TreeFilterDefinition definition = new TreeFilterDefinition(
				null, null, null
		);
		String relativePrefix = "level1.level2.level3.prefix1_";
		actualReturn = rootContext.nestComposed(
				new StubMappingElement( typeModel1Mock, relativePrefix ),
				relativePrefix, definition, new TreeFilterPathTracker( definition ),
				nestedContextBuilderMock, CYCLIC_RECURSION_EXCEPTION_FACTORY
		);
		InOrder inOrder = inOrder( nestedContextBuilderMock );
		inOrder.verify( nestedContextBuilderMock ).appendObject( "level1" );
		inOrder.verify( nestedContextBuilderMock ).appendObject( "level2" );
		inOrder.verify( nestedContextBuilderMock ).appendObject( "level3" );
		inOrder.verify( nestedContextBuilderMock ).build( nestedContextCapture.capture() );
		verifyNoOtherInteractionsAndReset();
		assertThat( actualReturn ).isNotNull()
				.isPresent()
				.get().isSameAs( expectedReturn );

		TreeNestingContext level3Context = nestedContextCapture.getValue();

		checkFooBarIncluded( "prefix1_", level3Context );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2552")
	void nestComposed_includePaths() {
		TreeNestingContext rootContext = TreeNestingContext.root();

		Set<String> includePaths = new HashSet<>();

		includePaths.add( "level2.level3" );
		includePaths.add( "level2.prefix2_level3" );

		TreeNestingContext level1Context = checkSimpleComposedFilterIncluded(
				"level1", rootContext, typeModel1Mock, "level1.",
				null, includePaths, null
		);
		checkFooBarExcluded( "", level1Context );
		checkFooBarComposedFilterExcluded( level1Context, typeModel2Mock );
		checkLeafExcluded( "level3", level1Context, "level3" );
		checkCompositeExcluded( "level3", level1Context, "level3" );
		checkLeafIncluded( "level2", level1Context, "level2" );
		// Names including dots and matching the filter will be accepted
		checkLeafIncluded( "level2.level3", level1Context, "level2.level3" );
		checkCompositeIncluded( "level2.level3", level1Context, "level2.level3" );

		// Check simple nesting (no filter composition)

		TreeNestingContext level2NonComposedContext =
				checkCompositeIncluded( "level2", level1Context, "level2" );
		checkFooBarExcluded( "", level2NonComposedContext );
		checkLeafIncluded( "level3", level2NonComposedContext, "level3" );
		checkLeafIncluded( "prefix2_level3", level2NonComposedContext, "prefix2_level3" );
		checkCompositeIncluded( "level3", level2NonComposedContext, "level3" );
		checkCompositeIncluded( "prefix2_level3", level2NonComposedContext, "prefix2_level3" );
		checkLeafExcluded( "level3.foo", level2NonComposedContext, "level3.foo" );
		checkCompositeExcluded( "level3.foo", level2NonComposedContext, "level3.foo" );

		// Check nesting with filter composition without a prefix

		TreeNestingContext level2Context = checkSimpleComposedFilterIncluded(
				"level2", level1Context, typeModel2Mock, "level2.",
				null, null, null
		);
		checkFooBarExcluded( "", level2Context );
		checkFooBarComposedFilterExcluded( level2Context, typeModel3Mock );
		checkLeafIncluded( "level3", level2Context, "level3" );
		checkLeafIncluded( "prefix2_level3", level2Context, "prefix2_level3" );
		checkCompositeIncluded( "level3", level2Context, "level3" );
		checkCompositeIncluded( "prefix2_level3", level2Context, "prefix2_level3" );
		checkLeafExcluded( "level3.foo", level2Context, "level3.foo" );
		checkCompositeExcluded( "level3.foo", level2Context, "level3.foo" );

		// Check nesting with filter composition with a prefix

		level2Context = checkSimpleComposedFilterIncluded(
				"level2", level1Context, typeModel2Mock, "level2.prefix2_",
				null, null, null
		);
		checkFooBarExcluded( "prefix2_", level2Context );
		checkFooBarComposedFilterExcluded( level2Context, typeModel3Mock );
		checkLeafIncluded( "prefix2_level3", level2Context, "level3" );
		checkCompositeIncluded( "prefix2_level3", level2Context, "level3" );
		checkLeafExcluded( "prefix2_prefix2_level3", level2Context, "prefix2_level3" );
		checkCompositeExcluded( "prefix2_prefix2_level3", level2Context, "prefix2_level3" );
		checkLeafExcluded( "prefix2_level3.foo", level2Context, "level3.foo" );
		checkCompositeExcluded( "prefix2_level3.foo", level2Context, "level3.foo" );

		// Check nesting with filter composition with path filter composition

		includePaths.clear();
		includePaths.add( "prefix2_level3" );

		level2Context = checkSimpleComposedFilterIncluded(
				"level2", level1Context, typeModel2Mock, "level2.",
				null, includePaths, null
		);
		checkFooBarExcluded( "", level2Context );
		checkFooBarComposedFilterExcluded( level2Context, typeModel3Mock );
		checkLeafIncluded( "prefix2_level3", level2Context, "prefix2_level3" );
		checkCompositeIncluded( "prefix2_level3", level2Context, "prefix2_level3" );
		checkLeafExcluded( "prefix2_level3.foo", level2Context, "prefix2_level3.foo" );
		checkCompositeExcluded( "prefix2_level3.foo", level2Context, "prefix2_level3.foo" );
		// Excluded due to additional filters: this right here checks HSEARCH-2552 is fixed
		checkLeafExcluded( "level3", level2Context, "level3" );
		checkCompositeExcluded( "level3", level2Context, "level3" );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3136")
	void nestComposed_includePaths_tracking() {
		TreeNestingContext rootContext = TreeNestingContext.root();

		Set<String> includePaths = new HashSet<>();
		includePaths.add( "included" );
		includePaths.add( "notEncountered" );
		includePaths.add( "level2NonComposed" );
		includePaths.add( "level2NonComposed.included" );
		includePaths.add( "level2NonComposed.notEncountered" );
		includePaths.add( "level2Composed.included" );
		includePaths.add( "level2Composed.notEncountered" );
		includePaths.add( "level2Composed.excludedBecauseOfLevel2" );
		TreeFilterDefinition level1Definition = new TreeFilterDefinition( null, includePaths, null );
		TreeFilterPathTracker level1PathTracker = new TreeFilterPathTracker( level1Definition );
		TreeNestingContext level1Context = checkSimpleComposedFilterIncluded(
				"level1", rootContext,
				typeModel1Mock, "level1.", level1Definition, level1PathTracker
		);
		// Initially no path was encountered so all includePaths are useless
		assertThat( level1PathTracker.encounteredFieldPaths() )
				.isEmpty();
		// We have no exclude paths so it should be empty all the time:
		assertThat( level1PathTracker.uselessExcludePaths() ).isEmpty();
		assertThat( level1PathTracker.uselessIncludePaths() )
				.containsOnly(
						"included",
						"notEncountered",
						"level2NonComposed",
						"level2NonComposed.included",
						"level2NonComposed.notEncountered",
						"level2Composed.included",
						"level2Composed.notEncountered",
						"level2Composed.excludedBecauseOfLevel2"
				);

		// Encounter "included" and "excludedBecauseOfLevel1"
		checkLeafIncluded( "included", level1Context, "included" );
		checkLeafExcluded( "excludedBecauseOfLevel1", level1Context, "excludedBecauseOfLevel1" );
		assertThat( level1PathTracker.encounteredFieldPaths() )
				.containsOnly(
						"included", // Added
						"excludedBecauseOfLevel1" // Added
				);
		// We have no exclude paths so it should be empty all the time:
		assertThat( level1PathTracker.uselessExcludePaths() ).isEmpty();
		assertThat( level1PathTracker.uselessIncludePaths() )
				.containsOnly(
						// "included" removed
						"notEncountered",
						"level2NonComposed",
						"level2NonComposed.included",
						"level2NonComposed.notEncountered",
						"level2Composed.included",
						"level2Composed.notEncountered",
						"level2Composed.excludedBecauseOfLevel2"
				);

		// Check simple nesting (no filter composition)
		TreeNestingContext level2NonComposedContext =
				checkCompositeIncluded( "level2NonComposed", level1Context, "level2NonComposed" );
		assertThat( level1PathTracker.encounteredFieldPaths() )
				.containsOnly(
						"included",
						"excludedBecauseOfLevel1",
						"level2NonComposed" // Added
				);
		// We have no exclude paths so it should be empty all the time:
		assertThat( level1PathTracker.uselessExcludePaths() ).isEmpty();
		assertThat( level1PathTracker.uselessIncludePaths() )
				.containsOnly(
						"notEncountered",
						// "level2NonComposed" removed
						"level2NonComposed.included",
						"level2NonComposed.notEncountered",
						"level2Composed.included",
						"level2Composed.notEncountered",
						"level2Composed.excludedBecauseOfLevel2"
				);

		// Encounter "level2NonComposed.included" and "level2NonComposed.excludedBecauseOfLevel1"
		checkLeafIncluded( "included", level2NonComposedContext, "included" );
		checkLeafExcluded( "excludedBecauseOfLevel1", level2NonComposedContext, "excludedBecauseOfLevel1" );
		assertThat( level1PathTracker.encounteredFieldPaths() )
				.containsOnly(
						"included",
						"excludedBecauseOfLevel1",
						"level2NonComposed",
						"level2NonComposed.included", // Added
						"level2NonComposed.excludedBecauseOfLevel1" // Added
				);
		// We have no exclude paths so it should be empty all the time:
		assertThat( level1PathTracker.uselessExcludePaths() ).isEmpty();
		assertThat( level1PathTracker.uselessIncludePaths() )
				.containsOnly(
						"notEncountered",
						// "level2NonComposed.included" removed
						"level2NonComposed.notEncountered",
						"level2Composed.included",
						"level2Composed.notEncountered",
						"level2Composed.excludedBecauseOfLevel2"
				);

		// Check composition
		includePaths.clear();
		includePaths.add( "included" );
		includePaths.add( "notEncountered" );
		includePaths.add( "excludedBecauseOfLevel1" );
		TreeFilterDefinition level2Definition = new TreeFilterDefinition( null, includePaths, null );
		TreeFilterPathTracker level2PathTracker = new TreeFilterPathTracker( level2Definition );
		TreeNestingContext level2ComposedContext = checkSimpleComposedFilterIncluded(
				"level2Composed", level1Context,
				typeModel2Mock, "level2Composed.", level2Definition, level2PathTracker
		);
		assertThat( level2PathTracker.encounteredFieldPaths() )
				.isEmpty();
		assertThat( level1PathTracker.encounteredFieldPaths() )
				.containsOnly(
						"included",
						"excludedBecauseOfLevel1",
						"level2NonComposed",
						"level2NonComposed.included",
						"level2NonComposed.excludedBecauseOfLevel1",
						"level2Composed" // Added
				);
		assertThat( level2PathTracker.uselessIncludePaths() )
				.containsOnly(
						"included",
						"notEncountered",
						"excludedBecauseOfLevel1"
				);
		// We have no exclude paths so it should be empty all the time:
		assertThat( level1PathTracker.uselessExcludePaths() ).isEmpty();
		assertThat( level1PathTracker.uselessIncludePaths() )
				.containsOnly(
						// No change expected
						"notEncountered",
						"level2NonComposed.notEncountered",
						"level2Composed.included",
						"level2Composed.notEncountered",
						"level2Composed.excludedBecauseOfLevel2"
				);

		// Encounter "level2Composed.included" and "level2Composed.excludedBecauseOfLevel1"
		checkLeafIncluded( "included", level2ComposedContext, "included" );
		checkLeafExcluded( "excludedBecauseOfLevel1", level2ComposedContext, "excludedBecauseOfLevel1" );
		assertThat( level2PathTracker.encounteredFieldPaths() )
				.containsOnly(
						"included", // Added
						"excludedBecauseOfLevel1" // Added
				);
		assertThat( level1PathTracker.encounteredFieldPaths() )
				.containsOnly(
						"included",
						"excludedBecauseOfLevel1",
						"level2NonComposed",
						"level2NonComposed.included",
						"level2NonComposed.excludedBecauseOfLevel1",
						"level2Composed",
						"level2Composed.included", // Added
						"level2Composed.excludedBecauseOfLevel1" // Added
				);
		assertThat( level2PathTracker.uselessIncludePaths() )
				.containsOnly(
						// "included" removed
						"notEncountered"
				// "excludedBecauseOfLevel1" removed
				);
		// We have no exclude paths so it should be empty all the time:
		assertThat( level1PathTracker.uselessExcludePaths() ).isEmpty();
		assertThat( level1PathTracker.uselessIncludePaths() )
				.containsOnly(
						"notEncountered",
						"level2NonComposed.notEncountered",
						// "level2Composed.included" removed
						"level2Composed.notEncountered",
						"level2Composed.excludedBecauseOfLevel2"
				);

		// Encounter "level2Composed.excludedBecauseOfLevel2"
		checkLeafExcluded( "excludedBecauseOfLevel2", level2ComposedContext, "excludedBecauseOfLevel2" );
		assertThat( level2PathTracker.encounteredFieldPaths() )
				.containsOnly(
						"included",
						"excludedBecauseOfLevel1",
						"excludedBecauseOfLevel2" // Added
				);
		assertThat( level1PathTracker.encounteredFieldPaths() )
				.containsOnly(
						"included",
						"excludedBecauseOfLevel1",
						"level2NonComposed",
						"level2NonComposed.included",
						"level2NonComposed.excludedBecauseOfLevel1",
						"level2Composed",
						"level2Composed.included",
						"level2Composed.excludedBecauseOfLevel1"
				//"level2Composed.excludedBecauseOfLevel2" // should not be added since it is excluded at lvl2, hence it wasn't encountered at lvl1.
				);
		assertThat( level2PathTracker.uselessIncludePaths() )
				.containsOnly(
						// No change expected
						"notEncountered"
				);
		// We have no exclude paths so it should be empty all the time:
		assertThat( level1PathTracker.uselessExcludePaths() ).isEmpty();
		assertThat( level1PathTracker.uselessIncludePaths() )
				.containsOnly(
						"notEncountered",
						"level2NonComposed.notEncountered",
						"level2Composed.notEncountered",
						// No change expected: "excludedBecauseOfLevel2" was excluded in the end, so it really is useless
						"level2Composed.excludedBecauseOfLevel2"
				);
	}

	/*
	 * Test is using the following pseudo model ( all IndexedEmbedded has "notEncountered" excludes ):
	 *
	 * public static class Indexed {
	 * 		String exclude;
	 * 		String include;
	 * 		@IndexedEmbedded( exclude Level1( exclude, level2.exclude, level2.level3.exclude ))
	 * 		Level1 level1;
	 * 		Level1 level1NotIndexed;
	 * 	}
	 *
	 * 	public static class Level1 {
	 * 		String exclude;
	 * 		String include;
	 * 		@IndexedEmbedded( exclude Level2(String exclude) )
	 * 		Level2 level2;
	 * 	}
	 *
	 * 	public static class Level2 {
	 * 		String exclude;
	 * 		String include;
	 * 		String excludedInLevel1Filter;
	 * 		@IndexedEmbedded
	 * 		Level3 level3;
	 * 		Level3 level3NotAnnotated;
	 * 	}
	 *
	 * 	public static class Level3 {
	 * 		String exclude;
	 * 		String include;
	 * 	}
	 */

	@Test
	void nestComposed_excludePaths_tracking() {
		TreeNestingContext rootContext = TreeNestingContext.root();

		Set<String> excludePaths = new HashSet<>();
		excludePaths.add( "exclude" );
		excludePaths.add( "notEncountered" );
		excludePaths.add( "level2.exclude" );
		excludePaths.add( "level2.notEncountered" );
		excludePaths.add( "level2.level3.exclude" );
		excludePaths.add( "level2.level3.notEncountered" );

		TreeFilterDefinition level1Definition = new TreeFilterDefinition( null, null, excludePaths );
		TreeFilterPathTracker level1PathTracker = new TreeFilterPathTracker( level1Definition );
		TreeNestingContext level1Context = checkSimpleComposedFilterIncluded(
				"level1", rootContext,
				typeModel1Mock, "level1.", level1Definition, level1PathTracker
		);
		// Initially no path was encountered so all excludePaths are useless and there's no include paths so no useless included paths as a result
		assertThat( level1PathTracker.encounteredFieldPaths() ).isEmpty();
		assertThat( level1PathTracker.uselessIncludePaths() ).isEmpty();
		assertThat( level1PathTracker.uselessExcludePaths() ).containsOnlyOnceElementsOf( excludePaths );


		// Encounter "excluded" and "included"
		checkLeafExcluded( "exclude", level1Context, "exclude" );
		checkLeafIncluded( "include", level1Context, "include" );
		assertThat( level1PathTracker.encounteredFieldPaths() )
				.containsOnly(
						"exclude", // Added
						"include" // Added
				);
		// We have no include paths so it should be empty all the time:
		assertThat( level1PathTracker.uselessIncludePaths() ).isEmpty();
		assertThat( level1PathTracker.uselessExcludePaths() )
				.containsOnly(
						// "exclude", // removed
						"notEncountered",
						"level2.exclude",
						"level2.notEncountered",
						"level2.level3.exclude",
						"level2.level3.notEncountered"
				);

		TreeFilterDefinition level2Definition = new TreeFilterDefinition(
				null, null, asSet( "excludedInLevel1Filter", "notEncountered" ) );
		TreeFilterPathTracker level2PathTracker = new TreeFilterPathTracker( level2Definition );
		TreeNestingContext level2ComposedContext = checkSimpleComposedFilterIncluded(
				"level2", level1Context,
				typeModel2Mock, "level2.", level2Definition, level2PathTracker
		);
		assertThat( level2PathTracker.encounteredFieldPaths() ).isEmpty();
		assertThat( level1PathTracker.encounteredFieldPaths() )
				.containsOnly(
						"exclude",
						"include",
						"level2" // Added
				);
		assertThat( level2PathTracker.uselessIncludePaths() ).isEmpty();
		assertThat( level2PathTracker.uselessExcludePaths() )
				.containsOnly(
						"notEncountered",
						"excludedInLevel1Filter"
				);

		// go through level2 properties:
		checkLeafIncluded( "include", level2ComposedContext, "include" );
		checkLeafExcluded( "exclude", level2ComposedContext, "exclude" );
		checkLeafExcluded( "excludedInLevel1Filter", level2ComposedContext, "excludedInLevel1Filter" );

		assertThat( level2PathTracker.encounteredFieldPaths() )
				.containsOnly(
						"include", // Added
						"exclude", // Added
						"excludedInLevel1Filter" // Added
				);
		assertThat( level1PathTracker.encounteredFieldPaths() )
				.containsOnly(
						"exclude",
						"include",
						"level2",
						"level2.include", // Added
						"level2.exclude" // Added
				// "level2.excludedInLevel1Filter" // Should not be added since it was excluded at lvl2 tracking.
				);
		assertThat( level2PathTracker.uselessIncludePaths() ).isEmpty();
		assertThat( level2PathTracker.uselessExcludePaths() )
				.containsOnly(
						"notEncountered"
				// "excludedInLevel1Filter" // Removed
				);
		assertThat( level1PathTracker.uselessIncludePaths() ).isEmpty();
		assertThat( level1PathTracker.uselessExcludePaths() )
				.containsOnly(
						// "exclude", // removed
						"notEncountered",
						// "level2.exclude", // removed
						"level2.notEncountered",
						"level2.level3.exclude",
						"level2.level3.notEncountered"
				);


		TreeFilterDefinition level3Definition = new TreeFilterDefinition( null, null, asSet( "notEncountered" ) );
		TreeFilterPathTracker level3PathTracker = new TreeFilterPathTracker( level3Definition );
		TreeNestingContext level3ComposedContext = checkSimpleComposedFilterIncluded(
				"level3", level2ComposedContext,
				typeModel3Mock, "level3.", level3Definition, level3PathTracker
		);
		assertThat( level3PathTracker.encounteredFieldPaths() ).isEmpty();
		assertThat( level2PathTracker.encounteredFieldPaths() ).containsOnly(
				"include",
				"exclude",
				"excludedInLevel1Filter",
				"level3" // Added
		);
		assertThat( level1PathTracker.encounteredFieldPaths() )
				.containsOnly(
						"exclude",
						"include",
						"level2",
						"level2.include",
						"level2.exclude",
						"level2.level3"
				);
		assertThat( level2PathTracker.uselessIncludePaths() ).isEmpty();
		assertThat( level2PathTracker.uselessExcludePaths() )
				.containsOnly(
						"notEncountered"
				);
		assertThat( level1PathTracker.uselessIncludePaths() ).isEmpty();
		assertThat( level1PathTracker.uselessExcludePaths() )
				.containsOnly(
						"notEncountered",
						"level2.notEncountered",
						"level2.level3.exclude",
						"level2.level3.notEncountered"
				);

		checkLeafIncluded( "include", level3ComposedContext, "include" );
		checkLeafExcluded( "exclude", level3ComposedContext, "exclude" );

		assertThat( level3PathTracker.encounteredFieldPaths() ).containsOnly(
				"include", // Added
				"exclude" // Added
		);
		assertThat( level2PathTracker.encounteredFieldPaths() ).containsOnly(
				"include",
				"exclude",
				"excludedInLevel1Filter",
				"level3",
				"level3.include", // Added
				"level3.exclude" // Added
		);
		assertThat( level1PathTracker.encounteredFieldPaths() )
				.containsOnly(
						"exclude",
						"include",
						"level2",
						"level2.include",
						"level2.exclude",
						"level2.level3",
						"level2.level3.include", // Added
						"level2.level3.exclude" // Added
				);
		assertThat( level2PathTracker.uselessIncludePaths() ).isEmpty();
		assertThat( level2PathTracker.uselessExcludePaths() )
				.containsOnly(
						"notEncountered"
				);
		assertThat( level1PathTracker.uselessIncludePaths() ).isEmpty();
		assertThat( level1PathTracker.uselessExcludePaths() )
				.containsOnly(
						"notEncountered",
						"level2.notEncountered",
						// "level2.level3.exclude", // removed
						"level2.level3.notEncountered"
				);
	}

	/*
	 * Test is using the following pseudo model:
	 *
	 * public static class Indexed {
	 * 		String exclude;
	 * 		String include;
	 * 		@IndexedEmbedded( depth = 1 exclude = level2.exclude))
	 * 		Level1 level1;
	 * 		Level1 level1NotIndexed;
	 * 	}
	 *
	 * 	public static class Level1 {
	 * 		String exclude;
	 * 		String include;
	 * 		@IndexedEmbedded
	 * 		Level2 level2;
	 * 	}
	 *
	 * 	public static class Level2 {
	 * 		String exclude;
	 * 		String include;
	 * 		String excludedInLevel1Filter;
	 * 		@IndexedEmbedded
	 * 		Level3 level3;
	 * 		Level3 level3NotAnnotated;
	 * 	}
	 *
	 * 	public static class Level3 {
	 * 		String exclude;
	 * 		String include;
	 * 	}
	 */

	@Test
	void nestComposed_excludePaths_depth1_excludeLevel2() {
		TreeNestingContext rootContext = TreeNestingContext.root();

		Set<String> excludePaths = new HashSet<>();
		excludePaths.add( "level2.exclude" );

		TreeFilterDefinition level1Definition = new TreeFilterDefinition( 1, null, excludePaths );
		TreeFilterPathTracker level1PathTracker = new TreeFilterPathTracker( level1Definition );
		TreeNestingContext level1Context = checkSimpleComposedFilterIncluded(
				"level1", rootContext,
				typeModel1Mock, "level1.", level1Definition, level1PathTracker
		);
		// Initially no path was encountered so all excludePaths are useless and there's no include paths so no useless included paths as a result
		assertThat( level1PathTracker.encounteredFieldPaths() ).isEmpty();
		assertThat( level1PathTracker.uselessIncludePaths() ).isEmpty();
		assertThat( level1PathTracker.uselessExcludePaths() ).containsOnlyOnceElementsOf( excludePaths );


		// Encounter "excluded" and "included"
		checkLeafIncluded( "exclude", level1Context, "exclude" );
		checkLeafIncluded( "include", level1Context, "include" );

		// We have no include paths so it should be empty all the time:
		assertThat( level1PathTracker.uselessIncludePaths() ).isEmpty();
		assertThat( level1PathTracker.uselessExcludePaths() )
				.containsOnly(
						"level2.exclude" // so far it is useless
				);

		TreeFilterDefinition level2Definition = TreeFilterDefinition.includeAll();
		TreeFilterPathTracker level2PathTracker = new TreeFilterPathTracker( level2Definition );
		checkSimpleComposedFilterExcluded(
				level1Context,
				typeModel2Mock, "level2.", level2Definition, level2PathTracker
		);

		assertThat( level1PathTracker.uselessExcludePaths() )
				.containsOnly(
						"level2.exclude" // it is still useless
				);

		assertThat( level2PathTracker.uselessExcludePaths() ).isEmpty();
	}

	/*
	 * Test is using the following pseudo model:
	 *
	 * public static class Indexed {
	 * 		String exclude;
	 * 		String include;
	 * 		@IndexedEmbedded( exclude = level2.exclude ))
	 * 		Level1 level1;
	 * 		Level1 level1NotIndexed;
	 * 	}
	 *
	 * 	public static class Level1 {
	 * 		String exclude;
	 * 		String include;
	 * 		@IndexedEmbedded( exclude = exclude ))
	 * 		Level2 level2;
	 * 	}
	 *
	 * 	public static class Level2 {
	 * 		String exclude;
	 * 		String include;
	 * 		String excludedInLevel1Filter;
	 * 		@IndexedEmbedded
	 * 		Level3 level3;
	 * 		Level3 level3NotAnnotated;
	 * 	}
	 *
	 * 	public static class Level3 {
	 * 		String exclude;
	 * 		String include;
	 * 	}
	 */

	@Test
	void nestComposed_excludePaths_sameFieldDifferentLevels() {
		TreeNestingContext rootContext = TreeNestingContext.root();

		Set<String> excludePaths = new HashSet<>();
		excludePaths.add( "level2.exclude" );

		TreeFilterDefinition level1Definition = new TreeFilterDefinition( null, null, excludePaths );
		TreeFilterPathTracker level1PathTracker = new TreeFilterPathTracker( level1Definition );
		TreeNestingContext level1Context = checkSimpleComposedFilterIncluded(
				"level1", rootContext,
				typeModel1Mock, "level1.", level1Definition, level1PathTracker
		);
		// Initially no path was encountered so all excludePaths are useless and there's no include paths so no useless included paths as a result
		assertThat( level1PathTracker.encounteredFieldPaths() ).isEmpty();
		assertThat( level1PathTracker.uselessIncludePaths() ).isEmpty();
		assertThat( level1PathTracker.uselessExcludePaths() ).containsOnlyOnceElementsOf( excludePaths );


		// Encounter "excluded" and "included"
		checkLeafIncluded( "exclude", level1Context, "exclude" );
		checkLeafIncluded( "include", level1Context, "include" );

		// We have no include paths so it should be empty all the time:
		assertThat( level1PathTracker.uselessIncludePaths() ).isEmpty();
		assertThat( level1PathTracker.uselessExcludePaths() )
				.containsOnly(
						"level2.exclude" // so far it is useless
				);

		TreeFilterDefinition level2Definition = new TreeFilterDefinition( null, null, asSet( "exclude" ) );
		TreeFilterPathTracker level2PathTracker = new TreeFilterPathTracker( level2Definition );
		TreeNestingContext level2Context = checkSimpleComposedFilterIncluded(
				"level2", level1Context,
				typeModel2Mock, "level2.", level2Definition, level2PathTracker
		);

		checkLeafExcluded( "exclude", level2Context, "exclude" );
		checkLeafIncluded( "include", level2Context, "include" );

		assertThat( level1PathTracker.uselessExcludePaths() ).containsOnly(
				"level2.exclude" // added since we have an exclude filter at lvl2 already...
		);

		assertThat( level2PathTracker.uselessExcludePaths() ).isEmpty();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2194")
	void nestComposed_noFilterThenIncludePaths() {
		TreeNestingContext rootContext = TreeNestingContext.root();

		// First level of composition: no filter
		TreeNestingContext level1Context = checkSimpleComposedFilterIncluded(
				"professionnelGC", rootContext, typeModel1Mock, "professionnelGC.",
				null, null, null
		);

		// Second level of composition: includePaths filter
		TreeNestingContext level2Context = checkSimpleComposedFilterIncluded(
				"groupe", level1Context, typeModel1Mock, "groupe.",
				null, asSet( "raisonSociale" ), null
		);

		checkLeafIncluded( "raisonSociale", level2Context, "raisonSociale" );
		checkLeafExcluded( "notRaisonSociale", level2Context, "notRaisonSociale" );
	}

	@Test
	void nestComposed_depth0() {
		TreeNestingContext rootContext = TreeNestingContext.root();

		// Depth == 0 => only include paths if they are explicitly included.
		// There is little use for this without includePaths, but we test it as an edge case

		checkSimpleComposedFilterExcluded(
				rootContext, typeModel1Mock,
				"level1.", 0, null, null
		);
	}

	@Test
	void nestComposed_depth1() {
		TreeNestingContext rootContext = TreeNestingContext.root();

		// Depth == 1 => implicitly include all fields at the first level,
		// unless a field is explicitly excluded,
		// but only include nested paths in composed filters if they are explicitly included.
		// (which they won't, because we don't use includePaths/excludePaths here).

		TreeNestingContext level1Context = checkSimpleComposedFilterIncluded(
				"level1", rootContext, typeModel1Mock,
				"level1.", 1, null, null
		);
		checkFooBarIncluded( "", level1Context );
		checkFooBarComposedFilterExcluded( level1Context, typeModel2Mock );

		// Check simple nesting (no filter composition)

		TreeNestingContext level2NonComposedContext =
				checkCompositeIncluded( "level2", level1Context, "level2" );
		checkFooBarIncluded( "", level2NonComposedContext );

		TreeNestingContext level3NonComposedContext =
				checkCompositeIncluded( "level3", level2NonComposedContext, "level3" );
		checkFooBarIncluded( "", level3NonComposedContext );
	}

	@Test
	void nestComposed_depth3_overridden() {
		TreeNestingContext rootContext = TreeNestingContext.root();

		// Depth == 3 => allow three levels of nesting with filter composition, including level1,
		// and allow unlimited nesting without filter composition from any of those three levels

		TreeNestingContext level1Context = checkSimpleComposedFilterIncluded(
				"level1", rootContext, typeModel1Mock,
				"level1.", 3, null, null
		);
		checkFooBarIncluded( "", level1Context );

		// Check simple nesting (no filter composition)

		TreeNestingContext level2NonComposedContext =
				checkCompositeIncluded( "level2", level1Context, "level2" );
		checkFooBarIncluded( "", level2NonComposedContext );

		TreeNestingContext level3NonComposedContext =
				checkCompositeIncluded( "level3", level2NonComposedContext, "level3" );
		checkFooBarIncluded( "", level3NonComposedContext );

		TreeNestingContext level4NonComposedContext =
				checkCompositeIncluded( "level4", level3NonComposedContext, "level4" );
		checkFooBarIncluded( "", level4NonComposedContext );

		// Check nesting with filter composition

		TreeNestingContext level2Context = checkSimpleComposedFilterIncluded(
				"level2", level1Context, typeModel2Mock,
				"level2.", null, null, null
		);
		checkFooBarIncluded( "", level2Context );
		level3NonComposedContext =
				checkCompositeIncluded( "level3", level2Context, "level3" );
		checkFooBarIncluded( "", level3NonComposedContext );
		level4NonComposedContext =
				checkCompositeIncluded( "level4", level3NonComposedContext, "level4" );
		checkFooBarIncluded( "", level4NonComposedContext );

		TreeNestingContext level3Context = checkSimpleComposedFilterIncluded(
				"level3", level2Context, typeModel3Mock,
				"level3.", null, null, null
		);
		checkFooBarIncluded( "", level3Context );
		level4NonComposedContext =
				checkCompositeIncluded( "level4", level3Context, "level4" );
		checkFooBarIncluded( "", level4NonComposedContext );

		checkSimpleComposedFilterExcluded(
				level3Context, typeModel4Mock,
				"level4.", null, null, null
		);

		// Check nesting with filter composition with a depth override

		level2Context = checkSimpleComposedFilterIncluded(
				"level2", level1Context, typeModel2Mock,
				"level2.", 1, null, null
		);
		checkFooBarIncluded( "", level2Context );

		checkSimpleComposedFilterExcluded(
				level2Context, typeModel3Mock,
				"level3.", null, null, null
		);
	}

	@Test
	void nestComposed_includePaths_depth1() {
		TreeNestingContext rootContext = TreeNestingContext.root();

		Set<String> includePaths = new HashSet<>();

		includePaths.add( "level2.level3" );
		includePaths.add( "level2.prefix2_level3" );

		TreeNestingContext level1Context = checkSimpleComposedFilterIncluded(
				"level1", rootContext, typeModel1Mock, "level1.",
				1, includePaths, null
		);
		checkFooBarIncluded( "", level1Context );
		checkFooBarComposedFilterExcluded( level1Context, typeModel2Mock );
		checkLeafIncluded( "level2", level1Context, "level2" );

		// Check simple nesting (no filter composition)

		TreeNestingContext level2NonComposedContext =
				checkCompositeIncluded( "level2", level1Context, "level2" );
		checkFooBarIncluded( "", level2NonComposedContext );

		// Check nesting with filter composition without a prefix

		TreeNestingContext level2Context = checkSimpleComposedFilterIncluded(
				"level2", level1Context, typeModel2Mock, "level2.",
				null, null, null
		);
		checkFooBarExcluded( "", level2Context );
		checkFooBarComposedFilterExcluded( level2Context, typeModel3Mock );
		checkLeafIncluded( "level3", level2Context, "level3" );
		checkLeafIncluded( "prefix2_level3", level2Context, "prefix2_level3" );
		checkCompositeIncluded( "level3", level2Context, "level3" );
		checkCompositeIncluded( "prefix2_level3", level2Context, "prefix2_level3" );
		checkLeafExcluded( "level3.foo", level2Context, "level3.foo" );
		checkCompositeExcluded( "level3.foo", level2Context, "level3.foo" );

		// Check nesting with filter composition with a prefix

		level2Context = checkSimpleComposedFilterIncluded(
				"level2", level1Context, typeModel2Mock, "level2.prefix2_",
				null, null, null
		);
		checkFooBarExcluded( "prefix2_", level2Context );
		checkFooBarComposedFilterExcluded( level2Context, typeModel3Mock );
		checkLeafIncluded( "prefix2_level3", level2Context, "level3" );
		checkCompositeIncluded( "prefix2_level3", level2Context, "level3" );
		checkLeafExcluded( "prefix2_prefix2_level3", level2Context, "prefix2_level3" );
		checkCompositeExcluded( "prefix2_prefix2_level3", level2Context, "prefix2_level3" );
		checkLeafExcluded( "prefix2_level3.foo", level2Context, "level3.foo" );
		checkCompositeExcluded( "prefix2_level3.foo", level2Context, "level3.foo" );

		// Check nesting with filter composition with path filter composition

		includePaths.clear();
		includePaths.add( "level3" );

		level2Context = checkSimpleComposedFilterIncluded(
				"level2", level1Context, typeModel2Mock, "level2.",
				null, includePaths, null
		);
		checkFooBarExcluded( "", level2Context );
		checkFooBarComposedFilterExcluded( level2Context, typeModel3Mock );
		checkLeafIncluded( "level3", level2Context, "level3" );
		checkCompositeIncluded( "level3", level2Context, "level3" );
		checkLeafExcluded( "level3.foo", level2Context, "level3.foo" );
		checkCompositeExcluded( "level3.foo", level2Context, "level3.foo" );
		// Excluded due to additional filters
		checkLeafExcluded( "prefix2_level3", level2Context, "prefix2_level3" );
		checkCompositeExcluded( "prefix2_level3", level2Context, "prefix2_level3" );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3136")
	void nestComposed_includePaths_depth1_tracking() {
		TreeNestingContext rootContext = TreeNestingContext.root();

		Set<String> includePaths = new HashSet<>();

		includePaths.add( "included" );
		includePaths.add( "notEncountered" );
		includePaths.add( "level2.level3.included" );
		includePaths.add( "level2.level3.notEncountered" );
		TreeFilterDefinition level1Definition = new TreeFilterDefinition( 1, includePaths, null );
		TreeFilterPathTracker level1PathTracker = new TreeFilterPathTracker( level1Definition );
		TreeNestingContext level1Context = checkSimpleComposedFilterIncluded(
				"level1", rootContext,
				typeModel1Mock, "level1.",
				level1Definition, level1PathTracker
		);
		// Initially no path was encountered so all includePaths are useless
		assertThat( level1PathTracker.encounteredFieldPaths() )
				.isEmpty();
		assertThat( level1PathTracker.uselessIncludePaths() )
				.containsOnly(
						"included",
						"notEncountered",
						"level2.level3.included",
						"level2.level3.notEncountered"
				);

		// Encounter "included" and "excludedBecauseOfLevel1"
		checkLeafIncluded( "included", level1Context, "included" );
		checkLeafIncluded( "includedBecauseOfDepth", level1Context, "includedBecauseOfDepth" );
		assertThat( level1PathTracker.encounteredFieldPaths() )
				.containsOnly(
						"included", // Added
						"includedBecauseOfDepth" // Added
				);
		assertThat( level1PathTracker.uselessIncludePaths() )
				.containsOnly(
						// "included" removed
						"notEncountered",
						"level2.level3.included",
						"level2.level3.notEncountered"
				);

		// Encounter a nested filter
		TreeFilterDefinition level2Definition = TreeFilterDefinition.includeAll();
		TreeFilterPathTracker level2PathTracker = new TreeFilterPathTracker( level2Definition );
		TreeNestingContext level2Context = checkSimpleComposedFilterIncluded(
				"level2", level1Context,
				typeModel2Mock, "level2.", level2Definition, level2PathTracker
		);
		TreeFilterDefinition level3Definition = TreeFilterDefinition.includeAll();
		TreeFilterPathTracker level3PathTracker = new TreeFilterPathTracker( level3Definition );
		TreeNestingContext level3Context = checkSimpleComposedFilterIncluded(
				"level3", level2Context,
				typeModel2Mock, "level3.", level3Definition, level3PathTracker
		);
		assertThat( level3PathTracker.uselessIncludePaths() )
				.isEmpty();
		assertThat( level2PathTracker.uselessIncludePaths() )
				.isEmpty();
		assertThat( level1PathTracker.encounteredFieldPaths() )
				.containsOnly(
						"included",
						"includedBecauseOfDepth",
						"level2", // Added
						"level2.level3" // Added
				);
		assertThat( level1PathTracker.uselessIncludePaths() )
				.containsOnly(
						// No change expected
						"notEncountered",
						"level2.level3.included",
						"level2.level3.notEncountered"
				);

		// Encounter "level2.level3.included" and "level2.level3.excludedBecauseOfLevel1"
		checkLeafIncluded( "included", level3Context, "included" );
		checkLeafExcluded( "excludedBecauseOfLevel1", level3Context, "excludedBecauseOfLevel1" );
		assertThat( level1PathTracker.encounteredFieldPaths() )
				.containsOnly(
						"included",
						"includedBecauseOfDepth",
						"level2",
						"level2.level3",
						"level2.level3.included", // Added
						"level2.level3.excludedBecauseOfLevel1" // Added
				);
		assertThat( level1PathTracker.uselessIncludePaths() )
				.containsOnly(
						"notEncountered",
						// "level2.level3.included" removed
						"level2.level3.notEncountered"
				);
	}

	@Test
	void nestComposed_includePaths_nestComposed_includePaths() {
		TreeNestingContext rootContext = TreeNestingContext.root();

		Set<String> includePaths = new HashSet<>();

		includePaths.add( "level2.level3" );
		TreeNestingContext level1Context = checkSimpleComposedFilterIncluded(
				"level1", rootContext, typeModel1Mock, "level1.",
				null, includePaths, null
		);
		checkFooBarExcluded( "", level1Context );
		checkFooBarComposedFilterExcluded( level1Context, typeModel2Mock );
		checkLeafIncluded( "level2", level1Context, "level2" );

		includePaths.clear();
		includePaths.add( "level3" );
		includePaths.add( "level3-alt.level4" );
		TreeNestingContext level2Context = checkSimpleComposedFilterIncluded(
				"level2", level1Context, typeModel2Mock, "level2.",
				null, includePaths, null
		);
		checkFooBarExcluded( "", level2Context );
		checkFooBarComposedFilterExcluded( level2Context, typeModel3Mock );
		checkLeafIncluded( "level3", level2Context, "level3" );
		checkLeafExcluded( "level3-alt", level2Context, "level3-alt" );
		checkCompositeExcluded( "level3-alt", level2Context, "level3-alt" );

		// Also test a level 2 that has completely incompatible includePaths
		includePaths.clear();
		includePaths.add( "level3-alt.level4" );
		checkSimpleComposedFilterExcluded(
				level1Context, typeModel2Mock, "level2.",
				null, includePaths, null
		);

		TreeNestingContext level3Context =
				checkCompositeIncluded( "level3", level2Context, "level3" );
		checkFooBarExcluded( "", level3Context );
	}

	@Test
	void nestComposed_includePaths_nestComposed_depth1AndIncludePaths() {
		TreeNestingContext rootContext = TreeNestingContext.root();

		Set<String> includePaths = new HashSet<>();

		includePaths.add( "level2.level3" );
		TreeNestingContext level1Context = checkSimpleComposedFilterIncluded(
				"level1", rootContext, typeModel1Mock, "level1.",
				null, includePaths, null
		);
		checkFooBarExcluded( "", level1Context );
		checkFooBarComposedFilterExcluded( level1Context, typeModel2Mock );
		checkLeafIncluded( "level2", level1Context, "level2" );

		includePaths.clear();
		includePaths.add( "level3-alt.level4" );
		TreeNestingContext level2Context = checkSimpleComposedFilterIncluded(
				"level2", level1Context, typeModel2Mock, "level2.",
				1, includePaths, null
		);
		checkFooBarExcluded( "", level2Context );
		checkFooBarComposedFilterExcluded( level2Context, typeModel3Mock );
		checkLeafIncluded( "level3", level2Context, "level3" );
		checkLeafExcluded( "level3-alt", level2Context, "level3-alt" );
		checkCompositeExcluded( "level3-alt", level2Context, "level3-alt" );

		TreeNestingContext level3Context =
				checkCompositeIncluded( "level3", level2Context, "level3" );
		checkFooBarExcluded( "", level3Context );

	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3684")
	void nestComposed_includePaths_nestComposed_depth2AndIncludePaths() {
		TreeNestingContext rootContext = TreeNestingContext.root();

		Set<String> includePaths = new HashSet<>();

		includePaths.add( "text" );
		includePaths.add( "nested.nested.text" );
		TreeNestingContext level1Context = checkSimpleComposedFilterIncluded(
				"level1", rootContext, typeModel1Mock, "level1.",
				2, includePaths, null
		);

		// Same includePaths as above
		TreeNestingContext level2Context = checkSimpleComposedFilterIncluded(
				"nested", level1Context, typeModel2Mock, "nested.",
				2, includePaths, null
		);
		checkFooBarIncluded( "", level2Context );
		checkFooBarComposedFilterExcluded( level2Context, typeModel3Mock );
		checkLeafIncluded( "text", level2Context, "text" );
		checkCompositeIncluded( "nested", level2Context, "nested" );

		// Also check composing a third level
		// Same includePaths as above
		TreeNestingContext level3Context = checkSimpleComposedFilterIncluded(
				"nested", level2Context, typeModel3Mock, "nested.",
				2, includePaths, null
		);
		checkFooBarExcluded( "", level3Context );
		checkFooBarComposedFilterExcluded( level3Context, typeModel3Mock );
		checkLeafIncluded( "text", level3Context, "text" );
		checkCompositeExcluded( "nested", level3Context, "nested" );

		// A fourth level should be completely excluded
		// Same includePaths as above
		checkSimpleComposedFilterExcluded(
				level3Context, typeModel4Mock, "nested.",
				2, includePaths, null
		);
	}

	private void checkLeafIncluded(String expectedPrefixedName, TreeNestingContext context,
			String relativeFieldName) {
		Object expectedReturn = new Object();
		when( leafFactoryMock.create( expectedPrefixedName, TreeNodeInclusion.INCLUDED ) )
				.thenReturn( expectedReturn );
		Object actualReturn = context.nest( relativeFieldName, leafFactoryMock );
		verifyNoOtherInteractionsAndReset();
		assertThat( expectedReturn ).isSameAs( actualReturn );
	}

	private void checkLeafExcluded(String expectedPrefixedName, TreeNestingContext context,
			String relativeFieldName) {
		Object expectedReturn = new Object();
		when( leafFactoryMock.create( expectedPrefixedName, TreeNodeInclusion.EXCLUDED ) )
				.thenReturn( expectedReturn );
		Object actualReturn = context.nest( relativeFieldName, leafFactoryMock );
		verifyNoOtherInteractionsAndReset();
		assertThat( expectedReturn ).isSameAs( actualReturn );
	}

	private TreeNestingContext checkCompositeIncluded(String expectedPrefixedName,
			TreeNestingContext context, String relativeFieldName) {
		ArgumentCaptor<TreeNestingContext> nestedContextCapture =
				ArgumentCaptor.forClass( TreeNestingContext.class );
		Object expectedReturn = new Object();
		when( compositeFactoryMock.create(
				eq( expectedPrefixedName ), eq( TreeNodeInclusion.INCLUDED ),
				nestedContextCapture.capture()
		) )
				.thenReturn( expectedReturn );
		Object actualReturn = context.nest( relativeFieldName, compositeFactoryMock );
		verifyNoOtherInteractionsAndReset();
		assertThat( expectedReturn ).isSameAs( actualReturn );

		// Also check that dynamic leaves will be included
		checkDynamicIncluded( "", nestedContextCapture.getValue() );

		return nestedContextCapture.getValue();
	}

	private void checkCompositeExcluded(String expectedPrefixedName, TreeNestingContext context,
			String relativeFieldName) {
		checkCompositeExcluded( expectedPrefixedName, context, relativeFieldName, true );
	}

	private void checkCompositeExcluded(String expectedPrefixedName, TreeNestingContext context,
			String relativeFieldName, boolean recurse) {
		ArgumentCaptor<TreeNestingContext> nestedContextCapture =
				ArgumentCaptor.forClass( TreeNestingContext.class );
		Object expectedReturn = new Object();
		when( compositeFactoryMock.create(
				eq( expectedPrefixedName ), eq( TreeNodeInclusion.EXCLUDED ),
				nestedContextCapture.capture()
		) )
				.thenReturn( expectedReturn );
		Object actualReturn = context.nest( relativeFieldName, compositeFactoryMock );
		verifyNoOtherInteractionsAndReset();
		assertThat( expectedReturn ).isSameAs( actualReturn );

		if ( recurse ) {
			// Also check that leaves will be excluded
			checkFooBarExcluded( "", nestedContextCapture.getValue(), false );
			checkDynamicExcluded( "", nestedContextCapture.getValue() );
		}
	}

	private void checkDynamicIncluded(String expectedPrefix, TreeNestingContext context) {
		Object expectedReturn = new Object();
		when( unfilteredFactoryMock.create( TreeNodeInclusion.INCLUDED, expectedPrefix ) )
				.thenReturn( expectedReturn );
		Object actualReturn = context.nestUnfiltered( unfilteredFactoryMock );
		verifyNoOtherInteractionsAndReset();
		assertThat( expectedReturn ).isSameAs( actualReturn );
	}

	private void checkDynamicExcluded(String expectedPrefix, TreeNestingContext context) {
		Object expectedReturn = new Object();
		when( unfilteredFactoryMock.create( TreeNodeInclusion.EXCLUDED, expectedPrefix ) )
				.thenReturn( expectedReturn );
		Object actualReturn = context.nestUnfiltered( unfilteredFactoryMock );
		verifyNoOtherInteractionsAndReset();
		assertThat( expectedReturn ).isSameAs( actualReturn );
	}

	private TreeNestingContext checkSimpleComposedFilterIncluded(String expectedObjectName,
			TreeNestingContext context,
			MappableTypeModel typeModel, String relativePrefix,
			Integer depth, Set<String> includePaths, Set<String> excludePaths) {
		TreeFilterDefinition definition = new TreeFilterDefinition( depth, includePaths, excludePaths );
		return checkSimpleComposedFilterIncluded( expectedObjectName, context,
				typeModel, relativePrefix, definition,
				new TreeFilterPathTracker( definition ) );
	}

	private TreeNestingContext checkSimpleComposedFilterIncluded(String expectedObjectName,
			TreeNestingContext context,
			MappableTypeModel definingTypeModel, String relativePrefix, TreeFilterDefinition definition,
			TreeFilterPathTracker pathTracker) {
		ArgumentCaptor<TreeNestingContext> nestedContextCapture =
				ArgumentCaptor.forClass( TreeNestingContext.class );
		Object expectedReturn = new Object();
		when( nestedContextBuilderMock.build( any() ) ).thenReturn( expectedReturn );
		Optional<Object> actualReturn = context.nestComposed(
				new StubMappingElement( definingTypeModel, relativePrefix ),
				relativePrefix, definition,
				pathTracker, nestedContextBuilderMock,
				CYCLIC_RECURSION_EXCEPTION_FACTORY
		);
		assertThat( actualReturn ).as( "Expected .nestComposed() to return a non-null result" )
				.isNotNull()
				.as( "Expected the composed filter to be included in " + context )
				.isPresent();
		InOrder inOrder = inOrder( nestedContextBuilderMock );
		inOrder.verify( nestedContextBuilderMock ).appendObject( expectedObjectName );
		inOrder.verify( nestedContextBuilderMock ).build( nestedContextCapture.capture() );
		verifyNoOtherInteractionsAndReset();
		assertThat( expectedReturn ).isSameAs( actualReturn.get() );

		return nestedContextCapture.getValue();
	}

	private void checkSimpleComposedFilterExcluded(TreeNestingContext context,
			MappableTypeModel typeModel, String relativePrefix,
			Integer depth, Set<String> includePaths, Set<String> excludePaths) {
		TreeFilterDefinition definition = new TreeFilterDefinition( depth, includePaths, excludePaths );
		checkSimpleComposedFilterExcluded( context, typeModel, relativePrefix, definition,
				new TreeFilterPathTracker( definition ) );
	}

	private void checkSimpleComposedFilterExcluded(TreeNestingContext context,
			MappableTypeModel definingTypeModel, String relativePrefix, TreeFilterDefinition definition,
			TreeFilterPathTracker pathTracker) {
		Optional<Object> actualReturn = context.nestComposed( new StubMappingElement( definingTypeModel, relativePrefix ),
				relativePrefix, definition, pathTracker, nestedContextBuilderMock, CYCLIC_RECURSION_EXCEPTION_FACTORY );
		verifyNoOtherInteractionsAndReset();
		assertThat( actualReturn ).as( "Expected .nestComposed() to return a non-null result" )
				.isNotNull()
				.as( "Expected the composed filter to be excluded from " + context )
				.isEmpty();
	}

	private void checkFooBarIncluded(String expectedPrefix, TreeNestingContext context) {
		checkLeafIncluded( expectedPrefix + "foo", context, "foo" );
		checkLeafIncluded( expectedPrefix + "bar", context, "bar" );
		checkCompositeIncluded( expectedPrefix + "foo", context, "foo" );
		checkCompositeIncluded( expectedPrefix + "bar", context, "bar" );

		// Also test weird names that include dots
		checkLeafIncluded( expectedPrefix + "foo.bar", context, "foo.bar" );
		checkCompositeIncluded( expectedPrefix + "foo.bar", context, "foo.bar" );

		// Also test dynamic fields
		checkDynamicIncluded( expectedPrefix, context );
	}

	private void checkFooBarExcluded(String expectedPrefix, TreeNestingContext context) {
		checkFooBarExcluded( expectedPrefix, context, true );
	}

	private void checkFooBarExcluded(String expectedPrefix, TreeNestingContext context, boolean recurse) {
		checkLeafExcluded( expectedPrefix + "foo", context, "foo" );
		checkLeafExcluded( expectedPrefix + "bar", context, "bar" );
		checkCompositeExcluded( expectedPrefix + "foo", context, "foo", recurse );
		checkCompositeExcluded( expectedPrefix + "bar", context, "bar", recurse );

		// Also test weird names that include dots
		checkLeafExcluded( expectedPrefix + "foo.bar", context, "foo.bar" );
		checkCompositeExcluded( expectedPrefix + "foo.bar", context, "foo.bar", recurse );
	}

	private void checkFooBarComposedFilterExcluded(TreeNestingContext context, MappableTypeModel typeModel) {
		checkSimpleComposedFilterExcluded(
				context, typeModel, "foo.", null, null, null
		);
		checkSimpleComposedFilterExcluded(
				context, typeModel, "prefix1_", null, null, null
		);
		checkSimpleComposedFilterExcluded(
				context, typeModel, "foo.prefix1_", null, null, null
		);
		checkSimpleComposedFilterExcluded(
				context, typeModel, "foo.bar.prefix1_", null, null, null
		);
		Set<String> includePaths = new HashSet<>();
		includePaths.add( "foo" );
		includePaths.add( "bar" );
		checkSimpleComposedFilterExcluded(
				context, typeModel, "foo", 3, includePaths, null
		);
	}

	private void verifyNoOtherInteractionsAndReset() {
		verifyNoMoreInteractions( leafFactoryMock, compositeFactoryMock, unfilteredFactoryMock, nestedContextBuilderMock );
		reset( leafFactoryMock, compositeFactoryMock, unfilteredFactoryMock, nestedContextBuilderMock );
	}

	private static final class StubMappingElement implements MappingElement {
		private final MappableTypeModel definingType;
		private final String relativePrefix;

		private StubMappingElement(MappableTypeModel definingType, String relativePrefix) {
			this.definingType = definingType;
			this.relativePrefix = relativePrefix;
		}

		@Override
		public String toString() {
			return "indexed-embedded with prefix '" + relativePrefix + "'";
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			StubMappingElement that = (StubMappingElement) o;
			return Objects.equals( definingType, that.definingType )
					&& Objects.equals(
							relativePrefix, that.relativePrefix );
		}

		@Override
		public int hashCode() {
			return Objects.hash( definingType, relativePrefix );
		}

		@Override
		public EventContext eventContext() {
			return EventContexts.fromType( definingType );
		}
	}
}
