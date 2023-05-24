/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.common.impl.CollectionHelper.asSet;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.engine.backend.document.model.dsl.impl.IndexSchemaNestingContext;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEmbeddedDefinition;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEmbeddedPathTracker;
import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

public class ConfiguredIndexSchemaManagerNestingContextTest {

	@Rule
	public final MockitoRule mockito = MockitoJUnit.rule().strictness( Strictness.STRICT_STUBS );

	@Mock(strictness = Mock.Strictness.LENIENT)
	private MappableTypeModel typeModel1Mock;

	@Mock(strictness = Mock.Strictness.LENIENT)
	private MappableTypeModel typeModel2Mock;

	@Mock(strictness = Mock.Strictness.LENIENT)
	private MappableTypeModel typeModel3Mock;

	@Mock(strictness = Mock.Strictness.LENIENT)
	private MappableTypeModel typeModel4Mock;

	@Mock
	private IndexSchemaNestingContext.LeafFactory<Object> leafFactoryMock;

	@Mock
	private IndexSchemaNestingContext.CompositeFactory<Object> compositeFactoryMock;

	@Mock
	private IndexSchemaNestingContext.UnfilteredFactory<Object> unfilteredFactoryMock;

	@Mock
	private ConfiguredIndexSchemaNestingContext.NestedContextBuilder<Object> nestedContextBuilderMock;

	@Before
	public void setup() {
		when( typeModel1Mock.name() ).thenReturn( "typeModel1Mock" );
		when( typeModel2Mock.name() ).thenReturn( "typeModel2Mock" );
		when( typeModel3Mock.name() ).thenReturn( "typeModel3Mock" );
		when( typeModel4Mock.name() ).thenReturn( "typeModel4Mock" );
	}

	@Test
	public void noFilter() {
		ConfiguredIndexSchemaNestingContext rootContext = ConfiguredIndexSchemaNestingContext.root();

		checkFooBarIncluded( "", rootContext );

		IndexSchemaNestingContext level1Context =
				checkCompositeIncluded( "level1", rootContext, "level1" );

		checkFooBarIncluded( "", level1Context );
	}

	@Test
	public void indexedEmbedded_noFilter() {
		ConfiguredIndexSchemaNestingContext rootContext = ConfiguredIndexSchemaNestingContext.root();

		ConfiguredIndexSchemaNestingContext level1Context = checkSimpleIndexedEmbeddedIncluded(
				"level1", rootContext, typeModel1Mock, "level1.prefix1_",
				null, null, null
		);
		checkFooBarIncluded( "prefix1_", level1Context );

		// Check non-IndexedEmbedded nesting

		IndexSchemaNestingContext level2NonIndexedEmbeddedContext =
				checkCompositeIncluded( "prefix1_level2", level1Context, "level2" );
		checkFooBarIncluded( "", level2NonIndexedEmbeddedContext );

		// Check IndexedEmbedded composition

		ConfiguredIndexSchemaNestingContext level2Context = checkSimpleIndexedEmbeddedIncluded(
				"prefix1_level2", level1Context, typeModel2Mock, "level2.prefix2_",
				null, null, null
		);
		checkFooBarIncluded( "prefix2_", level2Context );
	}

	@Test
	public void indexedEmbedded_noFilter_detectCycle_direct() {
		ConfiguredIndexSchemaNestingContext rootContext = ConfiguredIndexSchemaNestingContext.root();

		ConfiguredIndexSchemaNestingContext level1Context = checkSimpleIndexedEmbeddedIncluded(
				"level1", rootContext, typeModel1Mock, "level1.prefix1_",
				null, null, null
		);
		checkFooBarIncluded( "prefix1_", level1Context );

		assertThatThrownBy( () -> {
				IndexedEmbeddedDefinition level1Definition = new IndexedEmbeddedDefinition(
						typeModel1Mock, "level1.prefix1_", ObjectStructure.DEFAULT,
						null, null, null
				);
				level1Context.addIndexedEmbeddedIfIncluded(
						level1Definition, new IndexedEmbeddedPathTracker( level1Definition ),
						nestedContextBuilderMock
				);
		} )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Cyclic @IndexedEmbedded recursion starting from type '" + typeModel1Mock.toString() + "'",
						"Path starting from that type and ending with a cycle: 'level1.prefix1_level1.prefix1_'"
				);
		verifyNoOtherInteractionsAndReset();
	}

	@Test
	public void indexedEmbedded_noFilter_detectCycle_indirect() {
		ConfiguredIndexSchemaNestingContext rootContext = ConfiguredIndexSchemaNestingContext.root();

		ConfiguredIndexSchemaNestingContext level1Context = checkSimpleIndexedEmbeddedIncluded(
				"level1", rootContext, typeModel1Mock, "level1.prefix1_",
				null, null, null
		);

		ConfiguredIndexSchemaNestingContext level2Context = checkSimpleIndexedEmbeddedIncluded(
				"prefix1_level2", level1Context, typeModel1Mock, "level2.prefix2_",
				null, null, null
		);

		assertThatThrownBy( () -> {
			IndexedEmbeddedDefinition level2Definition = new IndexedEmbeddedDefinition(
					typeModel1Mock, "level1.prefix1_", ObjectStructure.DEFAULT,
					null, null, null
			);
			level2Context.addIndexedEmbeddedIfIncluded(
					level2Definition, new IndexedEmbeddedPathTracker( level2Definition ),
					nestedContextBuilderMock
			);
		} )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Cyclic @IndexedEmbedded recursion starting from type '" + typeModel1Mock.toString() + "'",
						"Path starting from that type and ending with a cycle: 'level1.prefix1_level2.prefix2_level1.prefix1_'"
				);
		verifyNoOtherInteractionsAndReset();
	}

	@Test
	public void indexedEmbedded_noFilter_multiLevelInOneIndexedEmbedded() {
		ConfiguredIndexSchemaNestingContext rootContext = ConfiguredIndexSchemaNestingContext.root();

		ArgumentCaptor<ConfiguredIndexSchemaNestingContext> nestedContextCapture =
				ArgumentCaptor.forClass( ConfiguredIndexSchemaNestingContext.class );

		Object expectedReturn;
		Optional<Object> actualReturn;

		expectedReturn = new Object();
		when( nestedContextBuilderMock.build( any() ) )
				.thenReturn( expectedReturn );
		IndexedEmbeddedDefinition definition = new IndexedEmbeddedDefinition(
				typeModel1Mock, "level1.level2.level3.prefix1_", ObjectStructure.DEFAULT,
				null, null, null
		);
		actualReturn = rootContext.addIndexedEmbeddedIfIncluded(
				definition, new IndexedEmbeddedPathTracker( definition ),
				nestedContextBuilderMock
		);
		InOrder inOrder = inOrder( nestedContextBuilderMock );
		inOrder.verify( nestedContextBuilderMock ).appendObject( "level1" );
		inOrder.verify( nestedContextBuilderMock ).appendObject( "level2" );
		inOrder.verify( nestedContextBuilderMock ).appendObject( "level3" );
		inOrder.verify( nestedContextBuilderMock ).build( nestedContextCapture.capture() );
		verifyNoOtherInteractionsAndReset();
		assertNotNull( actualReturn );
		assertTrue( actualReturn.isPresent() );
		assertSame( expectedReturn, actualReturn.get() );

		ConfiguredIndexSchemaNestingContext level3Context = nestedContextCapture.getValue();

		checkFooBarIncluded( "prefix1_", level3Context );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2552")
	public void indexedEmbedded_includePaths() {
		ConfiguredIndexSchemaNestingContext rootContext = ConfiguredIndexSchemaNestingContext.root();

		Set<String> includePaths = new HashSet<>();

		includePaths.add( "level2.level3" );
		includePaths.add( "level2.prefix2_level3" );

		ConfiguredIndexSchemaNestingContext level1Context = checkSimpleIndexedEmbeddedIncluded(
				"level1", rootContext, typeModel1Mock, "level1.",
				null, includePaths, null
		);
		checkFooBarExcluded( "", level1Context );
		checkFooBarIndexedEmbeddedExcluded( level1Context, typeModel2Mock );
		checkLeafExcluded( "level3", level1Context, "level3" );
		checkCompositeExcluded( "level3", level1Context, "level3" );
		checkLeafIncluded( "level2", level1Context, "level2" );
		// Names including dots and matching the filter will be accepted
		checkLeafIncluded( "level2.level3", level1Context, "level2.level3" );
		checkCompositeIncluded( "level2.level3", level1Context, "level2.level3" );

		// Check non-IndexedEmbedded nesting

		IndexSchemaNestingContext level2NonIndexedEmbeddedContext =
				checkCompositeIncluded( "level2", level1Context, "level2" );
		checkFooBarExcluded( "", level2NonIndexedEmbeddedContext );
		checkLeafIncluded( "level3", level2NonIndexedEmbeddedContext, "level3" );
		checkLeafIncluded( "prefix2_level3", level2NonIndexedEmbeddedContext, "prefix2_level3" );
		checkCompositeIncluded( "level3", level2NonIndexedEmbeddedContext, "level3" );
		checkCompositeIncluded( "prefix2_level3", level2NonIndexedEmbeddedContext, "prefix2_level3" );
		checkLeafExcluded( "level3.foo", level2NonIndexedEmbeddedContext, "level3.foo" );
		checkCompositeExcluded( "level3.foo", level2NonIndexedEmbeddedContext, "level3.foo" );

		// Check IndexedEmbedded composition without a prefix

		ConfiguredIndexSchemaNestingContext level2Context = checkSimpleIndexedEmbeddedIncluded(
				"level2", level1Context, typeModel2Mock, "level2.",
				null, null, null
		);
		checkFooBarExcluded( "", level2Context );
		checkFooBarIndexedEmbeddedExcluded( level2Context, typeModel3Mock );
		checkLeafIncluded( "level3", level2Context, "level3" );
		checkLeafIncluded( "prefix2_level3", level2Context, "prefix2_level3" );
		checkCompositeIncluded( "level3", level2Context, "level3" );
		checkCompositeIncluded( "prefix2_level3", level2Context, "prefix2_level3" );
		checkLeafExcluded( "level3.foo", level2Context, "level3.foo" );
		checkCompositeExcluded( "level3.foo", level2Context, "level3.foo" );

		// Check IndexedEmbedded composition with a prefix

		level2Context = checkSimpleIndexedEmbeddedIncluded(
				"level2", level1Context, typeModel2Mock, "level2.prefix2_",
				null, null, null
		);
		checkFooBarExcluded( "prefix2_", level2Context );
		checkFooBarIndexedEmbeddedExcluded( level2Context, typeModel3Mock );
		checkLeafIncluded( "prefix2_level3", level2Context, "level3" );
		checkCompositeIncluded( "prefix2_level3", level2Context, "level3" );
		checkLeafExcluded( "prefix2_prefix2_level3", level2Context, "prefix2_level3" );
		checkCompositeExcluded( "prefix2_prefix2_level3", level2Context, "prefix2_level3" );
		checkLeafExcluded( "prefix2_level3.foo", level2Context, "level3.foo" );
		checkCompositeExcluded( "prefix2_level3.foo", level2Context, "level3.foo" );

		// Check IndexedEmbedded composition with path filter composition

		includePaths.clear();
		includePaths.add( "prefix2_level3" );

		level2Context = checkSimpleIndexedEmbeddedIncluded(
				"level2", level1Context, typeModel2Mock, "level2.",
				null, includePaths, null
		);
		checkFooBarExcluded( "", level2Context );
		checkFooBarIndexedEmbeddedExcluded( level2Context, typeModel3Mock );
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
	public void indexedEmbedded_includePaths_tracking() {
		ConfiguredIndexSchemaNestingContext rootContext = ConfiguredIndexSchemaNestingContext.root();

		Set<String> includePaths = new HashSet<>();
		includePaths.add( "included" );
		includePaths.add( "notEncountered" );
		includePaths.add( "level2NonIndexedEmbedded" );
		includePaths.add( "level2NonIndexedEmbedded.included" );
		includePaths.add( "level2NonIndexedEmbedded.notEncountered" );
		includePaths.add( "level2IndexedEmbedded.included" );
		includePaths.add( "level2IndexedEmbedded.notEncountered" );
		includePaths.add( "level2IndexedEmbedded.excludedBecauseOfLevel2" );
		IndexedEmbeddedDefinition level1Definition = new IndexedEmbeddedDefinition(
				typeModel1Mock, "level1.", ObjectStructure.DEFAULT, null, includePaths, null
		);
		IndexedEmbeddedPathTracker level1PathTracker = new IndexedEmbeddedPathTracker( level1Definition );
		ConfiguredIndexSchemaNestingContext level1Context = checkSimpleIndexedEmbeddedIncluded(
				"level1", rootContext,
				level1Definition, level1PathTracker
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
						"level2NonIndexedEmbedded",
						"level2NonIndexedEmbedded.included",
						"level2NonIndexedEmbedded.notEncountered",
						"level2IndexedEmbedded.included",
						"level2IndexedEmbedded.notEncountered",
						"level2IndexedEmbedded.excludedBecauseOfLevel2"
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
						"level2NonIndexedEmbedded",
						"level2NonIndexedEmbedded.included",
						"level2NonIndexedEmbedded.notEncountered",
						"level2IndexedEmbedded.included",
						"level2IndexedEmbedded.notEncountered",
						"level2IndexedEmbedded.excludedBecauseOfLevel2"
				);

		// Check non-IndexedEmbedded nesting
		IndexSchemaNestingContext level2NonIndexedEmbeddedContext =
				checkCompositeIncluded( "level2NonIndexedEmbedded", level1Context, "level2NonIndexedEmbedded" );
		assertThat( level1PathTracker.encounteredFieldPaths() )
				.containsOnly(
						"included",
						"excludedBecauseOfLevel1",
						"level2NonIndexedEmbedded" // Added
				);
		// We have no exclude paths so it should be empty all the time:
		assertThat( level1PathTracker.uselessExcludePaths() ).isEmpty();
		assertThat( level1PathTracker.uselessIncludePaths() )
				.containsOnly(
						"notEncountered",
						// "level2NonIndexedEmbedded" removed
						"level2NonIndexedEmbedded.included",
						"level2NonIndexedEmbedded.notEncountered",
						"level2IndexedEmbedded.included",
						"level2IndexedEmbedded.notEncountered",
						"level2IndexedEmbedded.excludedBecauseOfLevel2"
				);

		// Encounter "level2NonIndexedEmbedded.included" and "level2NonIndexedEmbedded.excludedBecauseOfLevel1"
		checkLeafIncluded( "included", level2NonIndexedEmbeddedContext, "included" );
		checkLeafExcluded( "excludedBecauseOfLevel1", level2NonIndexedEmbeddedContext, "excludedBecauseOfLevel1" );
		assertThat( level1PathTracker.encounteredFieldPaths() )
				.containsOnly(
						"included",
						"excludedBecauseOfLevel1",
						"level2NonIndexedEmbedded",
						"level2NonIndexedEmbedded.included", // Added
						"level2NonIndexedEmbedded.excludedBecauseOfLevel1" // Added
				);
		// We have no exclude paths so it should be empty all the time:
		assertThat( level1PathTracker.uselessExcludePaths() ).isEmpty();
		assertThat( level1PathTracker.uselessIncludePaths() )
				.containsOnly(
						"notEncountered",
						// "level2NonIndexedEmbedded.included" removed
						"level2NonIndexedEmbedded.notEncountered",
						"level2IndexedEmbedded.included",
						"level2IndexedEmbedded.notEncountered",
						"level2IndexedEmbedded.excludedBecauseOfLevel2"
				);

		// Check IndexedEmbedded nesting
		includePaths.clear();
		includePaths.add( "included" );
		includePaths.add( "notEncountered" );
		includePaths.add( "excludedBecauseOfLevel1" );
		IndexedEmbeddedDefinition level2Definition = new IndexedEmbeddedDefinition(
				typeModel2Mock, "level2IndexedEmbedded.", ObjectStructure.DEFAULT,
				null, includePaths, null
		);
		IndexedEmbeddedPathTracker level2PathTracker = new IndexedEmbeddedPathTracker( level2Definition );
		ConfiguredIndexSchemaNestingContext level2IndexedEmbeddedContext = checkSimpleIndexedEmbeddedIncluded(
				"level2IndexedEmbedded", level1Context,
				level2Definition, level2PathTracker
		);
		assertThat( level2PathTracker.encounteredFieldPaths() )
				.isEmpty();
		assertThat( level1PathTracker.encounteredFieldPaths() )
				.containsOnly(
						"included",
						"excludedBecauseOfLevel1",
						"level2NonIndexedEmbedded",
						"level2NonIndexedEmbedded.included",
						"level2NonIndexedEmbedded.excludedBecauseOfLevel1",
						"level2IndexedEmbedded" // Added
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
						"level2NonIndexedEmbedded.notEncountered",
						"level2IndexedEmbedded.included",
						"level2IndexedEmbedded.notEncountered",
						"level2IndexedEmbedded.excludedBecauseOfLevel2"
				);

		// Encounter "level2IndexedEmbedded.included" and "level2IndexedEmbedded.excludedBecauseOfLevel1"
		checkLeafIncluded( "included", level2IndexedEmbeddedContext, "included" );
		checkLeafExcluded( "excludedBecauseOfLevel1", level2IndexedEmbeddedContext, "excludedBecauseOfLevel1" );
		assertThat( level2PathTracker.encounteredFieldPaths() )
				.containsOnly(
						"included", // Added
						"excludedBecauseOfLevel1" // Added
				);
		assertThat( level1PathTracker.encounteredFieldPaths() )
				.containsOnly(
						"included",
						"excludedBecauseOfLevel1",
						"level2NonIndexedEmbedded",
						"level2NonIndexedEmbedded.included",
						"level2NonIndexedEmbedded.excludedBecauseOfLevel1",
						"level2IndexedEmbedded",
						"level2IndexedEmbedded.included", // Added
						"level2IndexedEmbedded.excludedBecauseOfLevel1" // Added
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
						"level2NonIndexedEmbedded.notEncountered",
						// "level2IndexedEmbedded.included" removed
						"level2IndexedEmbedded.notEncountered",
						"level2IndexedEmbedded.excludedBecauseOfLevel2"
				);

		// Encounter "level2IndexedEmbedded.excludedBecauseOfLevel2"
		checkLeafExcluded( "excludedBecauseOfLevel2", level2IndexedEmbeddedContext, "excludedBecauseOfLevel2" );
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
						"level2NonIndexedEmbedded",
						"level2NonIndexedEmbedded.included",
						"level2NonIndexedEmbedded.excludedBecauseOfLevel1",
						"level2IndexedEmbedded",
						"level2IndexedEmbedded.included",
						"level2IndexedEmbedded.excludedBecauseOfLevel1"
						//"level2IndexedEmbedded.excludedBecauseOfLevel2" // should not be added since it is excluded at lvl2, hence it wasn't encountered at lvl1.
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
						"level2NonIndexedEmbedded.notEncountered",
						"level2IndexedEmbedded.notEncountered",
						// No change expected: "excludedBecauseOfLevel2" was excluded in the end, so it really is useless
						"level2IndexedEmbedded.excludedBecauseOfLevel2"
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
	 * 		String excludedInLevel1Embedded;
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
	public void indexedEmbedded_excludePaths_tracking() {
		ConfiguredIndexSchemaNestingContext rootContext = ConfiguredIndexSchemaNestingContext.root();

		Set<String> excludePaths = new HashSet<>();
		excludePaths.add( "exclude" );
		excludePaths.add( "notEncountered" );
		excludePaths.add( "level2.exclude" );
		excludePaths.add( "level2.notEncountered" );
		excludePaths.add( "level2.level3.exclude" );
		excludePaths.add( "level2.level3.notEncountered" );

		IndexedEmbeddedDefinition level1Definition = new IndexedEmbeddedDefinition(
				typeModel1Mock, "level1.", ObjectStructure.DEFAULT, null, null, excludePaths
		);
		IndexedEmbeddedPathTracker level1PathTracker = new IndexedEmbeddedPathTracker( level1Definition );
		ConfiguredIndexSchemaNestingContext level1Context = checkSimpleIndexedEmbeddedIncluded(
				"level1", rootContext,
				level1Definition, level1PathTracker
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

		IndexedEmbeddedDefinition level2Definition = new IndexedEmbeddedDefinition(
				typeModel2Mock, "level2.", ObjectStructure.DEFAULT,
				null, null, asSet( "excludedInLevel1Embedded", "notEncountered" )
		);
		IndexedEmbeddedPathTracker level2PathTracker = new IndexedEmbeddedPathTracker( level2Definition );
		ConfiguredIndexSchemaNestingContext level2IndexedEmbeddedContext = checkSimpleIndexedEmbeddedIncluded(
				"level2", level1Context,
				level2Definition, level2PathTracker
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
						"excludedInLevel1Embedded"
				);

		// go through level2 properties:
		checkLeafIncluded( "include", level2IndexedEmbeddedContext, "include" );
		checkLeafExcluded( "exclude", level2IndexedEmbeddedContext, "exclude" );
		checkLeafExcluded( "excludedInLevel1Embedded", level2IndexedEmbeddedContext, "excludedInLevel1Embedded" );

		assertThat( level2PathTracker.encounteredFieldPaths() )
				.containsOnly(
						"include", // Added
						"exclude", // Added
						"excludedInLevel1Embedded" // Added
				);
		assertThat( level1PathTracker.encounteredFieldPaths() )
				.containsOnly(
						"exclude",
						"include",
						"level2",
						"level2.include", // Added
						"level2.exclude" // Added
						// "level2.excludedInLevel1Embedded" // Should not be added since it was excluded at lvl2 tracking.
				);
		assertThat( level2PathTracker.uselessIncludePaths() ).isEmpty();
		assertThat( level2PathTracker.uselessExcludePaths() )
				.containsOnly(
						"notEncountered"
						// "excludedInLevel1Embedded" // Removed
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


		IndexedEmbeddedDefinition level3Definition = new IndexedEmbeddedDefinition(
				typeModel3Mock, "level3.", ObjectStructure.DEFAULT,
				null, null, asSet( "notEncountered" )
		);
		IndexedEmbeddedPathTracker level3PathTracker = new IndexedEmbeddedPathTracker( level3Definition );
		ConfiguredIndexSchemaNestingContext level3IndexedEmbeddedContext = checkSimpleIndexedEmbeddedIncluded(
				"level3", level2IndexedEmbeddedContext,
				level3Definition, level3PathTracker
		);
		assertThat( level3PathTracker.encounteredFieldPaths() ).isEmpty();
		assertThat( level2PathTracker.encounteredFieldPaths() ).containsOnly(
				"include",
				"exclude",
				"excludedInLevel1Embedded",
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

		checkLeafIncluded( "include", level3IndexedEmbeddedContext, "include" );
		checkLeafExcluded( "exclude", level3IndexedEmbeddedContext, "exclude" );

		assertThat( level3PathTracker.encounteredFieldPaths() ).containsOnly(
				"include", // Added
				"exclude" // Added
		);
		assertThat( level2PathTracker.encounteredFieldPaths() ).containsOnly(
				"include",
				"exclude",
				"excludedInLevel1Embedded",
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
	 * 		String excludedInLevel1Embedded;
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
	public void indexedEmbedded_excludePaths_depth1_exclude_level2() {
		ConfiguredIndexSchemaNestingContext rootContext = ConfiguredIndexSchemaNestingContext.root();

		Set<String> excludePaths = new HashSet<>();
		excludePaths.add( "level2.exclude" );

		IndexedEmbeddedDefinition level1Definition = new IndexedEmbeddedDefinition(
				typeModel1Mock, "level1.", ObjectStructure.DEFAULT, 1, null, excludePaths
		);
		IndexedEmbeddedPathTracker level1PathTracker = new IndexedEmbeddedPathTracker( level1Definition );
		ConfiguredIndexSchemaNestingContext level1Context = checkSimpleIndexedEmbeddedIncluded(
				"level1", rootContext,
				level1Definition, level1PathTracker
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

		IndexedEmbeddedDefinition level2Definition = new IndexedEmbeddedDefinition(
				typeModel2Mock, "level2.", ObjectStructure.DEFAULT,
				null, null, null
		);
		IndexedEmbeddedPathTracker level2PathTracker = new IndexedEmbeddedPathTracker( level2Definition );
		checkSimpleIndexedEmbeddedExcluded(
				level1Context,
				level2Definition, level2PathTracker
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
	 * 		String excludedInLevel1Embedded;
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
	public void indexedEmbedded_excludePaths_same_field_different_levels() {
		ConfiguredIndexSchemaNestingContext rootContext = ConfiguredIndexSchemaNestingContext.root();

		Set<String> excludePaths = new HashSet<>();
		excludePaths.add( "level2.exclude" );

		IndexedEmbeddedDefinition level1Definition = new IndexedEmbeddedDefinition(
				typeModel1Mock, "level1.", ObjectStructure.DEFAULT, null, null, excludePaths
		);
		IndexedEmbeddedPathTracker level1PathTracker = new IndexedEmbeddedPathTracker( level1Definition );
		ConfiguredIndexSchemaNestingContext level1Context = checkSimpleIndexedEmbeddedIncluded(
				"level1", rootContext,
				level1Definition, level1PathTracker
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

		IndexedEmbeddedDefinition level2Definition = new IndexedEmbeddedDefinition(
				typeModel2Mock, "level2.", ObjectStructure.DEFAULT,
				null, null, asSet( "exclude" )
		);
		IndexedEmbeddedPathTracker level2PathTracker = new IndexedEmbeddedPathTracker( level2Definition );
		ConfiguredIndexSchemaNestingContext level2Context = checkSimpleIndexedEmbeddedIncluded(
				"level2", level1Context,
				level2Definition, level2PathTracker
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
	public void indexedEmbedded_noFilterThenIncludePaths() {
		ConfiguredIndexSchemaNestingContext rootContext = ConfiguredIndexSchemaNestingContext.root();

		// First level of @IndexedEmbedded: no filter
		ConfiguredIndexSchemaNestingContext level1Context = checkSimpleIndexedEmbeddedIncluded(
				"professionnelGC", rootContext, typeModel1Mock, "professionnelGC.",
				null, null, null
		);

		// Second level of @IndexedEmbedded: includePaths filter
		ConfiguredIndexSchemaNestingContext level2Context = checkSimpleIndexedEmbeddedIncluded(
				"groupe", level1Context, typeModel1Mock, "groupe.",
				null, asSet( "raisonSociale" ), null
		);

		checkLeafIncluded( "raisonSociale", level2Context, "raisonSociale" );
		checkLeafExcluded( "notRaisonSociale", level2Context, "notRaisonSociale" );
	}

	@Test
	public void indexedEmbedded_depth0() {
		ConfiguredIndexSchemaNestingContext rootContext = ConfiguredIndexSchemaNestingContext.root();

		// Depth == 0 => only include fields and nested indexed-embeddeds if they are explicitly included.
		// There is little use for this without includePaths, but we test it as an edge case

		checkSimpleIndexedEmbeddedExcluded(
				rootContext, typeModel1Mock,
				"level1.", 0, null, null
		);
	}

	@Test
	public void indexedEmbedded_depth1() {
		ConfiguredIndexSchemaNestingContext rootContext = ConfiguredIndexSchemaNestingContext.root();

		// Depth == 1 => implicitly include all fields at the first level,
		// unless a field is explicitly excluded,
		// but only include nested indexed-embeddeds and their fields if they are explicitly included.
		// (which they won't, because we don't use includePaths/excludePaths here).

		ConfiguredIndexSchemaNestingContext level1Context = checkSimpleIndexedEmbeddedIncluded(
				"level1", rootContext, typeModel1Mock,
				"level1.", 1, null, null
		);
		checkFooBarIncluded( "", level1Context );
		checkFooBarIndexedEmbeddedExcluded( level1Context, typeModel2Mock );

		// Check non-IndexedEmbedded nesting

		IndexSchemaNestingContext level2NonIndexedEmbeddedContext =
				checkCompositeIncluded( "level2", level1Context, "level2" );
		checkFooBarIncluded( "", level2NonIndexedEmbeddedContext );

		IndexSchemaNestingContext level3NonIndexedEmbeddedContext =
				checkCompositeIncluded( "level3", level2NonIndexedEmbeddedContext, "level3" );
		checkFooBarIncluded( "", level3NonIndexedEmbeddedContext );
	}

	@Test
	public void indexedEmbedded_depth3_overridden() {
		ConfiguredIndexSchemaNestingContext rootContext = ConfiguredIndexSchemaNestingContext.root();

		// Depth == 3 => allow three levels of IndexedEmbedded composition, including level1,
		// and allow unlimited non-IndexedEmbedded nesting from any of those three levels

		ConfiguredIndexSchemaNestingContext level1Context = checkSimpleIndexedEmbeddedIncluded(
				"level1", rootContext, typeModel1Mock,
				"level1.", 3, null, null
		);
		checkFooBarIncluded( "", level1Context );

		// Check non-IndexedEmbedded nesting

		IndexSchemaNestingContext level2NonIndexedEmbeddedContext =
				checkCompositeIncluded( "level2", level1Context, "level2" );
		checkFooBarIncluded( "", level2NonIndexedEmbeddedContext );

		IndexSchemaNestingContext level3NonIndexedEmbeddedContext =
				checkCompositeIncluded( "level3", level2NonIndexedEmbeddedContext, "level3" );
		checkFooBarIncluded( "", level3NonIndexedEmbeddedContext );

		IndexSchemaNestingContext level4NonIndexedEmbeddedContext =
				checkCompositeIncluded( "level4", level3NonIndexedEmbeddedContext, "level4" );
		checkFooBarIncluded( "", level4NonIndexedEmbeddedContext );

		// Check IndexedEmbedded composition

		ConfiguredIndexSchemaNestingContext level2Context = checkSimpleIndexedEmbeddedIncluded(
				"level2", level1Context, typeModel2Mock,
				"level2.", null, null, null
		);
		checkFooBarIncluded( "", level2Context );
		level3NonIndexedEmbeddedContext =
				checkCompositeIncluded( "level3", level2Context, "level3" );
		checkFooBarIncluded( "", level3NonIndexedEmbeddedContext );
		level4NonIndexedEmbeddedContext =
				checkCompositeIncluded( "level4", level3NonIndexedEmbeddedContext, "level4" );
		checkFooBarIncluded( "", level4NonIndexedEmbeddedContext );

		ConfiguredIndexSchemaNestingContext level3Context = checkSimpleIndexedEmbeddedIncluded(
				"level3", level2Context, typeModel3Mock,
				"level3.", null, null, null
		);
		checkFooBarIncluded( "", level3Context );
		level4NonIndexedEmbeddedContext =
				checkCompositeIncluded( "level4", level3Context, "level4" );
		checkFooBarIncluded( "", level4NonIndexedEmbeddedContext );

		checkSimpleIndexedEmbeddedExcluded(
				level3Context, typeModel4Mock,
				"level4.", null, null, null
		);

		// Check IndexedEmbedded composition with a depth override

		level2Context = checkSimpleIndexedEmbeddedIncluded(
				"level2", level1Context, typeModel2Mock,
				"level2.", 1, null, null
		);
		checkFooBarIncluded( "", level2Context );

		checkSimpleIndexedEmbeddedExcluded(
				level2Context, typeModel3Mock,
				"level3.", null, null, null
		);
	}

	@Test
	public void indexedEmbedded_includePaths_depth1() {
		ConfiguredIndexSchemaNestingContext rootContext = ConfiguredIndexSchemaNestingContext.root();

		Set<String> includePaths = new HashSet<>();

		includePaths.add( "level2.level3" );
		includePaths.add( "level2.prefix2_level3" );

		ConfiguredIndexSchemaNestingContext level1Context = checkSimpleIndexedEmbeddedIncluded(
				"level1", rootContext, typeModel1Mock, "level1.",
				1, includePaths, null
		);
		checkFooBarIncluded( "", level1Context );
		checkFooBarIndexedEmbeddedExcluded( level1Context, typeModel2Mock );
		checkLeafIncluded( "level2", level1Context, "level2" );

		// Check non-IndexedEmbedded nesting

		IndexSchemaNestingContext level2NonIndexedEmbeddedContext =
				checkCompositeIncluded( "level2", level1Context, "level2" );
		checkFooBarIncluded( "", level2NonIndexedEmbeddedContext );

		// Check IndexedEmbedded composition without a prefix

		ConfiguredIndexSchemaNestingContext level2Context = checkSimpleIndexedEmbeddedIncluded(
				"level2", level1Context, typeModel2Mock, "level2.",
				null, null, null
		);
		checkFooBarExcluded( "", level2Context );
		checkFooBarIndexedEmbeddedExcluded( level2Context, typeModel3Mock );
		checkLeafIncluded( "level3", level2Context, "level3" );
		checkLeafIncluded( "prefix2_level3", level2Context, "prefix2_level3" );
		checkCompositeIncluded( "level3", level2Context, "level3" );
		checkCompositeIncluded( "prefix2_level3", level2Context, "prefix2_level3" );
		checkLeafExcluded( "level3.foo", level2Context, "level3.foo" );
		checkCompositeExcluded( "level3.foo", level2Context, "level3.foo" );

		// Check IndexedEmbedded composition with a prefix

		level2Context = checkSimpleIndexedEmbeddedIncluded(
				"level2", level1Context, typeModel2Mock, "level2.prefix2_",
				null, null, null
		);
		checkFooBarExcluded( "prefix2_", level2Context );
		checkFooBarIndexedEmbeddedExcluded( level2Context, typeModel3Mock );
		checkLeafIncluded( "prefix2_level3", level2Context, "level3" );
		checkCompositeIncluded( "prefix2_level3", level2Context, "level3" );
		checkLeafExcluded( "prefix2_prefix2_level3", level2Context, "prefix2_level3" );
		checkCompositeExcluded( "prefix2_prefix2_level3", level2Context, "prefix2_level3" );
		checkLeafExcluded( "prefix2_level3.foo", level2Context, "level3.foo" );
		checkCompositeExcluded( "prefix2_level3.foo", level2Context, "level3.foo" );

		// Check IndexedEmbedded composition with path filter composition

		includePaths.clear();
		includePaths.add( "level3" );

		level2Context = checkSimpleIndexedEmbeddedIncluded(
				"level2", level1Context, typeModel2Mock, "level2.",
				null, includePaths, null
		);
		checkFooBarExcluded( "", level2Context );
		checkFooBarIndexedEmbeddedExcluded( level2Context, typeModel3Mock );
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
	public void indexedEmbedded_includePaths_depth1_tracking() {
		ConfiguredIndexSchemaNestingContext rootContext = ConfiguredIndexSchemaNestingContext.root();

		Set<String> includePaths = new HashSet<>();

		includePaths.add( "included" );
		includePaths.add( "notEncountered" );
		includePaths.add( "level2.level3.included" );
		includePaths.add( "level2.level3.notEncountered" );
		IndexedEmbeddedDefinition level1Definition = new IndexedEmbeddedDefinition(
				typeModel1Mock, "level1.", ObjectStructure.DEFAULT, 1, includePaths, null
		);
		IndexedEmbeddedPathTracker level1PathTracker = new IndexedEmbeddedPathTracker( level1Definition );
		ConfiguredIndexSchemaNestingContext level1Context = checkSimpleIndexedEmbeddedIncluded(
				"level1", rootContext,
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

		// Encounter a nested indexedEmbedded
		IndexedEmbeddedDefinition level2Definition = new IndexedEmbeddedDefinition(
				typeModel2Mock, "level2.", ObjectStructure.DEFAULT, null, null, null
		);
		IndexedEmbeddedPathTracker level2PathTracker = new IndexedEmbeddedPathTracker( level2Definition );
		ConfiguredIndexSchemaNestingContext level2Context = checkSimpleIndexedEmbeddedIncluded(
				"level2", level1Context, level2Definition, level2PathTracker
		);
		IndexedEmbeddedDefinition level3Definition = new IndexedEmbeddedDefinition(
				typeModel2Mock, "level3.", ObjectStructure.DEFAULT, null, null, null
		);
		IndexedEmbeddedPathTracker level3PathTracker = new IndexedEmbeddedPathTracker( level3Definition );
		ConfiguredIndexSchemaNestingContext level3Context = checkSimpleIndexedEmbeddedIncluded(
				"level3", level2Context, level3Definition, level3PathTracker

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
	public void indexedEmbedded_includePaths_embedding_includePaths() {
		ConfiguredIndexSchemaNestingContext rootContext = ConfiguredIndexSchemaNestingContext.root();

		Set<String> includePaths = new HashSet<>();

		includePaths.add( "level2.level3" );
		ConfiguredIndexSchemaNestingContext level1Context = checkSimpleIndexedEmbeddedIncluded(
				"level1", rootContext, typeModel1Mock, "level1.",
				null, includePaths, null
		);
		checkFooBarExcluded( "", level1Context );
		checkFooBarIndexedEmbeddedExcluded( level1Context, typeModel2Mock );
		checkLeafIncluded( "level2", level1Context, "level2" );

		includePaths.clear();
		includePaths.add( "level3" );
		includePaths.add( "level3-alt.level4" );
		ConfiguredIndexSchemaNestingContext level2Context = checkSimpleIndexedEmbeddedIncluded(
				"level2", level1Context, typeModel2Mock, "level2.",
				null, includePaths, null
		);
		checkFooBarExcluded( "", level2Context );
		checkFooBarIndexedEmbeddedExcluded( level2Context, typeModel3Mock );
		checkLeafIncluded( "level3", level2Context, "level3" );
		checkLeafExcluded( "level3-alt", level2Context, "level3-alt" );
		checkCompositeExcluded( "level3-alt", level2Context, "level3-alt" );

		// Also test a level 2 that has completely incompatible includePaths
		includePaths.clear();
		includePaths.add( "level3-alt.level4" );
		checkSimpleIndexedEmbeddedExcluded(
				level1Context, typeModel2Mock, "level2.",
				null, includePaths, null
		);

		IndexSchemaNestingContext level3Context =
				checkCompositeIncluded( "level3", level2Context, "level3" );
		checkFooBarExcluded( "", level3Context );
	}

	@Test
	public void indexedEmbedded_includePaths_embedding_depth1AndIncludePaths() {
		ConfiguredIndexSchemaNestingContext rootContext = ConfiguredIndexSchemaNestingContext.root();

		Set<String> includePaths = new HashSet<>();

		includePaths.add( "level2.level3" );
		ConfiguredIndexSchemaNestingContext level1Context = checkSimpleIndexedEmbeddedIncluded(
				"level1", rootContext, typeModel1Mock, "level1.",
				null, includePaths, null
		);
		checkFooBarExcluded( "", level1Context );
		checkFooBarIndexedEmbeddedExcluded( level1Context, typeModel2Mock );
		checkLeafIncluded( "level2", level1Context, "level2" );

		includePaths.clear();
		includePaths.add( "level3-alt.level4" );
		ConfiguredIndexSchemaNestingContext level2Context = checkSimpleIndexedEmbeddedIncluded(
				"level2", level1Context, typeModel2Mock, "level2.",
				1, includePaths, null
		);
		checkFooBarExcluded( "", level2Context );
		checkFooBarIndexedEmbeddedExcluded( level2Context, typeModel3Mock );
		checkLeafIncluded( "level3", level2Context, "level3" );
		checkLeafExcluded( "level3-alt", level2Context, "level3-alt" );
		checkCompositeExcluded( "level3-alt", level2Context, "level3-alt" );

		IndexSchemaNestingContext level3Context =
				checkCompositeIncluded( "level3", level2Context, "level3" );
		checkFooBarExcluded( "", level3Context );

	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3684")
	public void indexedEmbedded_includePaths_embedding_depth2AndIncludePaths() {
		ConfiguredIndexSchemaNestingContext rootContext = ConfiguredIndexSchemaNestingContext.root();

		Set<String> includePaths = new HashSet<>();

		includePaths.add( "text" );
		includePaths.add( "nested.nested.text" );
		ConfiguredIndexSchemaNestingContext level1Context = checkSimpleIndexedEmbeddedIncluded(
				"level1", rootContext, typeModel1Mock, "level1.",
				2, includePaths, null
		);

		// Same includePaths as above
		ConfiguredIndexSchemaNestingContext level2Context = checkSimpleIndexedEmbeddedIncluded(
				"nested", level1Context, typeModel2Mock, "nested.",
				2, includePaths, null
		);
		checkFooBarIncluded( "", level2Context );
		checkFooBarIndexedEmbeddedExcluded( level2Context, typeModel3Mock );
		checkLeafIncluded( "text", level2Context, "text" );
		checkCompositeIncluded( "nested", level2Context, "nested" );

		// Also check embedding a third level of @IndexedEmbedded
		// Same includePaths as above
		ConfiguredIndexSchemaNestingContext level3Context = checkSimpleIndexedEmbeddedIncluded(
				"nested", level2Context, typeModel3Mock, "nested.",
				2, includePaths, null
		);
		checkFooBarExcluded( "", level3Context );
		checkFooBarIndexedEmbeddedExcluded( level3Context, typeModel3Mock );
		checkLeafIncluded( "text", level3Context, "text" );
		checkCompositeExcluded( "nested", level3Context, "nested" );

		// A fourth level should be completely excluded
		// Same includePaths as above
		checkSimpleIndexedEmbeddedExcluded(
				level3Context, typeModel4Mock, "nested.",
				2, includePaths, null
		);
	}

	private void checkLeafIncluded(String expectedPrefixedName, IndexSchemaNestingContext context,
			String relativeFieldName) {
		Object expectedReturn = new Object();
		when( leafFactoryMock.create( expectedPrefixedName, IndexFieldInclusion.INCLUDED ) )
				.thenReturn( expectedReturn );
		Object actualReturn = context.nest( relativeFieldName, leafFactoryMock );
		verifyNoOtherInteractionsAndReset();
		assertSame( expectedReturn, actualReturn );
	}

	private void checkLeafExcluded(String expectedPrefixedName, IndexSchemaNestingContext context,
			String relativeFieldName) {
		Object expectedReturn = new Object();
		when( leafFactoryMock.create( expectedPrefixedName, IndexFieldInclusion.EXCLUDED ) )
				.thenReturn( expectedReturn );
		Object actualReturn = context.nest( relativeFieldName, leafFactoryMock );
		verifyNoOtherInteractionsAndReset();
		assertSame( expectedReturn, actualReturn );
	}

	private IndexSchemaNestingContext checkCompositeIncluded(String expectedPrefixedName,
			IndexSchemaNestingContext context, String relativeFieldName) {
		ArgumentCaptor<IndexSchemaNestingContext> nestedContextCapture =
				ArgumentCaptor.forClass( ConfiguredIndexSchemaNestingContext.class );
		Object expectedReturn = new Object();
		when( compositeFactoryMock.create(
				eq( expectedPrefixedName ), eq( IndexFieldInclusion.INCLUDED ),
				nestedContextCapture.capture()
		) )
				.thenReturn( expectedReturn );
		Object actualReturn = context.nest( relativeFieldName, compositeFactoryMock );
		verifyNoOtherInteractionsAndReset();
		assertSame( expectedReturn, actualReturn );

		// Also check that dynamic leaves will be included
		checkDynamicIncluded( "", nestedContextCapture.getValue() );

		return nestedContextCapture.getValue();
	}

	private void checkCompositeExcluded(String expectedPrefixedName, IndexSchemaNestingContext context,
			String relativeFieldName) {
		checkCompositeExcluded( expectedPrefixedName, context, relativeFieldName, true );
	}

	private void checkCompositeExcluded(String expectedPrefixedName, IndexSchemaNestingContext context,
			String relativeFieldName, boolean recurse) {
		ArgumentCaptor<IndexSchemaNestingContext> nestedContextCapture =
				ArgumentCaptor.forClass( IndexSchemaNestingContext.class );
		Object expectedReturn = new Object();
		when( compositeFactoryMock.create(
				eq( expectedPrefixedName ), eq( IndexFieldInclusion.EXCLUDED ),
				nestedContextCapture.capture()
		) )
				.thenReturn( expectedReturn );
		Object actualReturn = context.nest( relativeFieldName, compositeFactoryMock );
		verifyNoOtherInteractionsAndReset();
		assertSame( expectedReturn, actualReturn );

		if ( recurse ) {
			// Also check that leaves will be excluded
			checkFooBarExcluded( "", nestedContextCapture.getValue(), false );
			checkDynamicExcluded( "", nestedContextCapture.getValue() );
		}
	}

	private void checkDynamicIncluded(String expectedPrefix, IndexSchemaNestingContext context) {
		Object expectedReturn = new Object();
		when( unfilteredFactoryMock.create( IndexFieldInclusion.INCLUDED, expectedPrefix ) )
				.thenReturn( expectedReturn );
		Object actualReturn = context.nestUnfiltered( unfilteredFactoryMock );
		verifyNoOtherInteractionsAndReset();
		assertSame( expectedReturn, actualReturn );
	}

	private void checkDynamicExcluded(String expectedPrefix, IndexSchemaNestingContext context) {
		Object expectedReturn = new Object();
		when( unfilteredFactoryMock.create( IndexFieldInclusion.EXCLUDED, expectedPrefix ) )
				.thenReturn( expectedReturn );
		Object actualReturn = context.nestUnfiltered( unfilteredFactoryMock );
		verifyNoOtherInteractionsAndReset();
		assertSame( expectedReturn, actualReturn );
	}

	private ConfiguredIndexSchemaNestingContext checkSimpleIndexedEmbeddedIncluded(String expectedObjectName,
			ConfiguredIndexSchemaNestingContext context, MappableTypeModel typeModel,
			String relativePrefix, Integer depth, Set<String> includePaths, Set<String> excludePaths) {
		IndexedEmbeddedDefinition definition = new IndexedEmbeddedDefinition(
				typeModel, relativePrefix, ObjectStructure.DEFAULT,
				depth, includePaths, excludePaths
		);
		return checkSimpleIndexedEmbeddedIncluded(
				expectedObjectName, context, definition, new IndexedEmbeddedPathTracker( definition )
		);
	}

	private ConfiguredIndexSchemaNestingContext checkSimpleIndexedEmbeddedIncluded(String expectedObjectName,
			ConfiguredIndexSchemaNestingContext context,
			IndexedEmbeddedDefinition definition,
			IndexedEmbeddedPathTracker pathTracker) {
		ArgumentCaptor<ConfiguredIndexSchemaNestingContext> nestedContextCapture =
				ArgumentCaptor.forClass( ConfiguredIndexSchemaNestingContext.class );
		Object expectedReturn = new Object();
		when( nestedContextBuilderMock.build( any() ) ).thenReturn( expectedReturn );
		Optional<Object> actualReturn = context.addIndexedEmbeddedIfIncluded(
				definition, pathTracker, nestedContextBuilderMock
		);
		assertNotNull( "Expected addIndexedEmbeddedIfIncluded to return a non-null result", actualReturn );
		assertTrue( "Expected the indexedEmbedded to be included in " + context, actualReturn.isPresent() );
		InOrder inOrder = inOrder( nestedContextBuilderMock );
		inOrder.verify( nestedContextBuilderMock ).appendObject( expectedObjectName );
		inOrder.verify( nestedContextBuilderMock ).build( nestedContextCapture.capture() );
		verifyNoOtherInteractionsAndReset();
		assertSame( expectedReturn, actualReturn.get() );

		return nestedContextCapture.getValue();
	}

	private void checkSimpleIndexedEmbeddedExcluded(ConfiguredIndexSchemaNestingContext context, MappableTypeModel typeModel,
			String relativePrefix, Integer depth, Set<String> includePaths, Set<String> excludePaths) {
		IndexedEmbeddedDefinition definition = new IndexedEmbeddedDefinition(
				typeModel, relativePrefix, ObjectStructure.DEFAULT,
				depth, includePaths, excludePaths
		);
		checkSimpleIndexedEmbeddedExcluded(
				context, definition, new IndexedEmbeddedPathTracker( definition )
		);
	}

	private void checkSimpleIndexedEmbeddedExcluded(ConfiguredIndexSchemaNestingContext context,
			IndexedEmbeddedDefinition definition, IndexedEmbeddedPathTracker pathTracker) {
		Optional<Object> actualReturn = context.addIndexedEmbeddedIfIncluded(
				definition, pathTracker, nestedContextBuilderMock
		);
		verifyNoOtherInteractionsAndReset();
		assertNotNull( "Expected addIndexedEmbeddedIfIncluded to return a non-null result", actualReturn );
		assertFalse( "Expected the indexedEmbedded to be excluded from " + context, actualReturn.isPresent() );
	}

	private void checkFooBarIncluded(String expectedPrefix, IndexSchemaNestingContext context) {
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

	private void checkFooBarExcluded(String expectedPrefix, IndexSchemaNestingContext context) {
		checkFooBarExcluded( expectedPrefix, context, true );
	}

	private void checkFooBarExcluded(String expectedPrefix, IndexSchemaNestingContext context, boolean recurse) {
		checkLeafExcluded( expectedPrefix + "foo", context, "foo" );
		checkLeafExcluded( expectedPrefix + "bar", context, "bar" );
		checkCompositeExcluded( expectedPrefix + "foo", context, "foo", recurse );
		checkCompositeExcluded( expectedPrefix + "bar", context, "bar", recurse );

		// Also test weird names that include dots
		checkLeafExcluded( expectedPrefix + "foo.bar", context, "foo.bar" );
		checkCompositeExcluded( expectedPrefix + "foo.bar", context, "foo.bar", recurse );
	}

	private void checkFooBarIndexedEmbeddedExcluded(ConfiguredIndexSchemaNestingContext context, MappableTypeModel typeModel) {
		checkSimpleIndexedEmbeddedExcluded(
				context, typeModel, "foo.", null, null, null
		);
		checkSimpleIndexedEmbeddedExcluded(
				context, typeModel, "prefix1_", null, null, null
		);
		checkSimpleIndexedEmbeddedExcluded(
				context, typeModel, "foo.prefix1_", null, null, null
		);
		checkSimpleIndexedEmbeddedExcluded(
				context, typeModel, "foo.bar.prefix1_", null, null, null
		);
		Set<String> includePaths = new HashSet<>();
		includePaths.add( "foo" );
		includePaths.add( "bar" );
		checkSimpleIndexedEmbeddedExcluded(
				context, typeModel, "foo", 3, includePaths, null
		);
	}

	private void verifyNoOtherInteractionsAndReset() {
		verifyNoMoreInteractions( leafFactoryMock, compositeFactoryMock, unfilteredFactoryMock, nestedContextBuilderMock );
		reset( leafFactoryMock, compositeFactoryMock, unfilteredFactoryMock, nestedContextBuilderMock );
	}
}
