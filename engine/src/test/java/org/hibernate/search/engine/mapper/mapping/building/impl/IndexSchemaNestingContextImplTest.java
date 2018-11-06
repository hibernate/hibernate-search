/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.impl;

import static org.easymock.EasyMock.newCapture;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.model.dsl.impl.IndexSchemaNestingContext;
import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.impl.common.CollectionHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;

public class IndexSchemaNestingContextImplTest extends EasyMockSupport {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private final MappableTypeModel typeModel1Mock = createMock( "typeModel1Mock", MappableTypeModel.class );
	private final MappableTypeModel typeModel2Mock = createMock( "typeModel2Mock", MappableTypeModel.class );
	private final MappableTypeModel typeModel3Mock = createMock( "typeModel3Mock", MappableTypeModel.class );
	private final MappableTypeModel typeModel4Mock = createMock( "typeModel4Mock", MappableTypeModel.class );
	private final StubLeafFactoryFunction leafFactoryIfIncludedMock =
			createMock( "leafFactoryIfIncludedMock", StubLeafFactoryFunction.class );
	private final StubLeafFactoryFunction leafFactoryIfExcludedMock =
			createMock( "leafFactoryIfExcludedMock", StubLeafFactoryFunction.class );
	private final StubCompositeFactoryFunction compositeFactoryIfIncludedMock =
			createMock( "compositeFactoryIfIncludedMock", StubCompositeFactoryFunction.class );
	private final StubCompositeFactoryFunction compositeFactoryIfExcludedMock =
			createMock( "compositeFactoryIfExcludedMock", StubCompositeFactoryFunction.class );
	private final StubNestedContextBuilder nestedContextBuilderMock =
			createStrictMock( StubNestedContextBuilder.class );

	@Test
	public void noFilter() {
		IndexSchemaNestingContextImpl rootContext = IndexSchemaNestingContextImpl.root();

		checkFooBarIncluded( "", rootContext );

		IndexSchemaNestingContext level1Context =
				checkCompositeIncluded( "level1", rootContext, "level1" );

		checkFooBarIncluded( "", level1Context );
	}

	@Test
	public void indexedEmbedded_noFilter() {
		IndexSchemaNestingContextImpl rootContext = IndexSchemaNestingContextImpl.root();

		IndexSchemaNestingContextImpl level1Context = checkSimpleIndexedEmbeddedIncluded(
				"level1", rootContext, typeModel1Mock, "level1.prefix1_",
				null, null
		);
		checkFooBarIncluded( "prefix1_", level1Context );

		// Check non-IndexedEmbedded nesting

		IndexSchemaNestingContext level2NonIndexedEmbeddedContext =
				checkCompositeIncluded( "prefix1_level2", level1Context, "level2" );
		checkFooBarIncluded( "", level2NonIndexedEmbeddedContext );

		// Check IndexedEmbedded composition

		IndexSchemaNestingContextImpl level2Context = checkSimpleIndexedEmbeddedIncluded(
				"prefix1_level2", level1Context, typeModel2Mock, "level2.prefix2_",
				null, null
		);
		checkFooBarIncluded( "prefix2_", level2Context );
	}

	@Test
	public void indexedEmbedded_noFilter_detectCycle_direct() {
		IndexSchemaNestingContextImpl rootContext = IndexSchemaNestingContextImpl.root();

		IndexSchemaNestingContextImpl level1Context = checkSimpleIndexedEmbeddedIncluded(
				"level1", rootContext, typeModel1Mock, "level1.prefix1_",
				null, null
		);
		checkFooBarIncluded( "prefix1_", level1Context );

		resetAll();
		EasyMock.expect( typeModel2Mock.getName() ).andReturn( "typeModel2Mock" );
		EasyMock.expect( typeModel1Mock.isSubTypeOf( typeModel2Mock ) ).andReturn( true );
		replayAll();
		thrown.expect( SearchException.class );
		thrown.expectMessage( "Found an infinite IndexedEmbedded recursion" );
		thrown.expectMessage( "path 'level1.prefix1_level1.prefix1_'" );
		thrown.expectMessage( "type '" + typeModel2Mock.toString() + "'" );
		try {
			level1Context.addIndexedEmbeddedIfIncluded( typeModel2Mock, "level1.prefix1_",
					null, null, nestedContextBuilderMock
			);
		}
		catch (SearchException e) {
			verifyAll();
			throw e;
		}
	}

	@Test
	public void indexedEmbedded_noFilter_detectCycle_indirect() {
		IndexSchemaNestingContextImpl rootContext = IndexSchemaNestingContextImpl.root();

		IndexSchemaNestingContextImpl level1Context = checkSimpleIndexedEmbeddedIncluded(
				"level1", rootContext, typeModel1Mock, "level1.prefix1_",
				null, null
		);

		IndexSchemaNestingContextImpl level2Context = checkSimpleIndexedEmbeddedIncluded(
				"prefix1_level2", level1Context, typeModel1Mock, "level2.prefix2_",
				null, null
		);

		resetAll();
		EasyMock.expect( typeModel3Mock.getName() ).andReturn( "typeModel3Mock" );
		EasyMock.expect( typeModel1Mock.isSubTypeOf( typeModel3Mock ) ).andReturn( true );
		replayAll();
		thrown.expect( SearchException.class );
		thrown.expectMessage( "Found an infinite IndexedEmbedded recursion" );
		thrown.expectMessage( "path 'level1.prefix1_level2.prefix2_level1.prefix1_'" );
		thrown.expectMessage( "type '" + typeModel3Mock.toString() + "'" );
		try {
			level2Context.addIndexedEmbeddedIfIncluded( typeModel3Mock, "level1.prefix1_",
					null, null, nestedContextBuilderMock
			);
		}
		catch (SearchException e) {
			verifyAll();
			throw e;
		}
	}

	@Test
	public void indexedEmbedded_noFilter_multiLevelInOneIndexedEmbedded() {
		IndexSchemaNestingContextImpl rootContext = IndexSchemaNestingContextImpl.root();

		StubNestedContextBuilder nestedContextBuilderMock = createStrictMock( StubNestedContextBuilder.class );
		Capture<IndexSchemaNestingContextImpl> nestedContextCapture = newCapture();

		Object expectedReturn;
		Optional<Object> actualReturn;

		resetAll();
		expectedReturn = new Object();
		nestedContextBuilderMock.appendObject( "level1" );
		nestedContextBuilderMock.appendObject( "level2" );
		nestedContextBuilderMock.appendObject( "level3" );
		EasyMock.expect( nestedContextBuilderMock.build( EasyMock.capture( nestedContextCapture ) ) )
				.andReturn( expectedReturn );
		replayAll();
		actualReturn = rootContext.addIndexedEmbeddedIfIncluded( typeModel1Mock, "level1.level2.level3.prefix1_",
				null, null, nestedContextBuilderMock );
		verifyAll();
		assertNotNull( actualReturn );
		assertTrue( actualReturn.isPresent() );
		assertSame( expectedReturn, actualReturn.get() );

		IndexSchemaNestingContextImpl level3Context = nestedContextCapture.getValue();

		checkFooBarIncluded( "prefix1_", level3Context );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2552")
	public void indexedEmbedded_includePaths() {
		IndexSchemaNestingContextImpl rootContext = IndexSchemaNestingContextImpl.root();

		Set<String> includePaths = new HashSet<>();

		includePaths.add( "level2.level3" );
		includePaths.add( "level2.prefix2_level3" );

		IndexSchemaNestingContextImpl level1Context = checkSimpleIndexedEmbeddedIncluded(
				"level1", rootContext, typeModel1Mock, "level1.",
				null, includePaths
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

		IndexSchemaNestingContextImpl level2Context = checkSimpleIndexedEmbeddedIncluded(
				"level2", level1Context, typeModel2Mock, "level2.",
				null, null
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
				null, null
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
				null, includePaths
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
	@TestForIssue(jiraKey = "HSEARCH-2194")
	public void indexedEmbedded_noFilterThenIncludePaths() {
		IndexSchemaNestingContextImpl rootContext = IndexSchemaNestingContextImpl.root();

		// First level of @IndexedEmbedded: no filter
		IndexSchemaNestingContextImpl level1Context = checkSimpleIndexedEmbeddedIncluded(
				"professionnelGC", rootContext, typeModel1Mock, "professionnelGC.",
				null, null
		);

		// Second level of @IndexedEmbedded: includePaths filter
		IndexSchemaNestingContextImpl level2Context = checkSimpleIndexedEmbeddedIncluded(
				"groupe", level1Context, typeModel1Mock, "groupe.",
				null, CollectionHelper.asSet( "raisonSociale" )
		);

		checkLeafIncluded( "raisonSociale", level2Context, "raisonSociale" );
		checkLeafExcluded( "notRaisonSociale", level2Context, "notRaisonSociale" );
	}

	@Test
	public void indexedEmbedded_depth0() {
		IndexSchemaNestingContextImpl rootContext = IndexSchemaNestingContextImpl.root();

		// Depth == 0 => do not allow IndexedEmbedded composition nor non-IndexedEmbedded nesting
		// There is little use for this, but we test it as an edge case

		checkSimpleIndexedEmbeddedExcluded(
				rootContext, typeModel1Mock,
				"level1.", 0, null
		);
	}

	@Test
	public void indexedEmbedded_depth1() {
		IndexSchemaNestingContextImpl rootContext = IndexSchemaNestingContextImpl.root();

		// Depth == 1 => do not allow IndexedEmbedded composition at all,
		// but allow unlimited non-IndexedEmbedded nesting

		IndexSchemaNestingContextImpl level1Context = checkSimpleIndexedEmbeddedIncluded(
				"level1", rootContext, typeModel1Mock,
				"level1.", 1, null
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
		IndexSchemaNestingContextImpl rootContext = IndexSchemaNestingContextImpl.root();

		// Depth == 3 => allow three levels of IndexedEmbedded composition, including level1,
		// and allow unlimited non-IndexedEmbedded nesting from any of those three levels

		IndexSchemaNestingContextImpl level1Context = checkSimpleIndexedEmbeddedIncluded(
				"level1", rootContext, typeModel1Mock,
				"level1.", 3, null
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

		IndexSchemaNestingContextImpl level2Context = checkSimpleIndexedEmbeddedIncluded(
				"level2", level1Context, typeModel2Mock,
				"level2.", null, null
		);
		checkFooBarIncluded( "", level2Context );
		level3NonIndexedEmbeddedContext =
				checkCompositeIncluded( "level3", level2Context, "level3" );
		checkFooBarIncluded( "", level3NonIndexedEmbeddedContext );
		level4NonIndexedEmbeddedContext =
				checkCompositeIncluded( "level4", level3NonIndexedEmbeddedContext, "level4" );
		checkFooBarIncluded( "", level4NonIndexedEmbeddedContext );

		IndexSchemaNestingContextImpl level3Context = checkSimpleIndexedEmbeddedIncluded(
				"level3", level2Context, typeModel3Mock,
				"level3.", null, null
		);
		checkFooBarIncluded( "", level3Context );
		level4NonIndexedEmbeddedContext =
				checkCompositeIncluded( "level4", level3Context, "level4" );
		checkFooBarIncluded( "", level4NonIndexedEmbeddedContext );

		checkSimpleIndexedEmbeddedExcluded(
				level3Context, typeModel4Mock,
				"level4.", null, null
		);

		// Check IndexedEmbedded composition with a depth override

		level2Context = checkSimpleIndexedEmbeddedIncluded(
				"level2", level1Context, typeModel2Mock,
				"level2.", 1, null
		);
		checkFooBarIncluded( "", level2Context );

		checkSimpleIndexedEmbeddedExcluded(
				level2Context, typeModel3Mock,
				"level3.", null, null
		);
	}

	@Test
	public void indexedEmbedded_includePaths_depth1() {
		IndexSchemaNestingContextImpl rootContext = IndexSchemaNestingContextImpl.root();

		Set<String> includePaths = new HashSet<>();

		includePaths.add( "level2.level3" );
		includePaths.add( "level2.prefix2_level3" );

		IndexSchemaNestingContextImpl level1Context = checkSimpleIndexedEmbeddedIncluded(
				"level1", rootContext, typeModel1Mock, "level1.",
				1, includePaths
		);
		checkFooBarIncluded( "", level1Context );
		checkFooBarIndexedEmbeddedExcluded( level1Context, typeModel2Mock );
		checkLeafIncluded( "level2", level1Context, "level2" );

		// Check non-IndexedEmbedded nesting

		IndexSchemaNestingContext level2NonIndexedEmbeddedContext =
				checkCompositeIncluded( "level2", level1Context, "level2" );
		checkFooBarIncluded( "", level2NonIndexedEmbeddedContext );

		// Check IndexedEmbedded composition without a prefix

		IndexSchemaNestingContextImpl level2Context = checkSimpleIndexedEmbeddedIncluded(
				"level2", level1Context, typeModel2Mock, "level2.",
				null, null
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
				null, null
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
				null, includePaths
		);
		checkFooBarExcluded( "", level2Context );
		checkFooBarIndexedEmbeddedExcluded( level2Context, typeModel3Mock );
		checkLeafIncluded( "prefix2_level3", level2Context, "prefix2_level3" );
		checkCompositeIncluded( "prefix2_level3", level2Context, "prefix2_level3" );
		checkLeafExcluded( "prefix2_level3.foo", level2Context, "prefix2_level3.foo" );
		checkCompositeExcluded( "prefix2_level3.foo", level2Context, "prefix2_level3.foo" );
		// Excluded due to additional filters
		checkLeafExcluded( "level3", level2Context, "level3" );
		checkCompositeExcluded( "level3", level2Context, "level3" );
	}

	@Test
	public void indexedEmbedded_includePaths_embedding_depth1AndIncludePaths() {
		IndexSchemaNestingContextImpl rootContext = IndexSchemaNestingContextImpl.root();

		Set<String> includePaths = new HashSet<>();

		includePaths.add( "level2.level3" );
		IndexSchemaNestingContextImpl level1Context = checkSimpleIndexedEmbeddedIncluded(
				"level1", rootContext, typeModel1Mock, "level1.",
				null, includePaths
		);
		checkFooBarExcluded( "", level1Context );
		checkFooBarIndexedEmbeddedExcluded( level1Context, typeModel2Mock );
		checkLeafIncluded( "level2", level1Context, "level2" );

		includePaths.clear();
		includePaths.add( "level3-alt.level4" );
		IndexSchemaNestingContextImpl level2Context = checkSimpleIndexedEmbeddedIncluded(
				"level2", level1Context, typeModel2Mock, "level2.",
				1, includePaths
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

	private void checkLeafIncluded(String expectedPrefixedName, IndexSchemaNestingContext context,
			String relativeFieldName) {
		resetAll();
		Object expectedReturn = new Object();
		EasyMock.expect( leafFactoryIfIncludedMock.apply( expectedPrefixedName ) ).andReturn( expectedReturn );
		replayAll();
		Object actualReturn = context.nest( relativeFieldName, leafFactoryIfIncludedMock, leafFactoryIfExcludedMock );
		verifyAll();
		assertSame( expectedReturn, actualReturn );
	}

	private void checkLeafExcluded(String expectedPrefixedName, IndexSchemaNestingContext context,
			String relativeFieldName) {
		resetAll();
		Object expectedReturn = new Object();
		EasyMock.expect( leafFactoryIfExcludedMock.apply( expectedPrefixedName ) ).andReturn( expectedReturn );
		replayAll();
		Object actualReturn = context.nest( relativeFieldName, leafFactoryIfIncludedMock, leafFactoryIfExcludedMock );
		verifyAll();
		assertSame( expectedReturn, actualReturn );
	}

	private IndexSchemaNestingContext checkCompositeIncluded(String expectedPrefixedName,
			IndexSchemaNestingContext context, String relativeFieldName) {
		Capture<IndexSchemaNestingContext> nestedContextCapture = newCapture();
		resetAll();
		Object expectedReturn = new Object();
		EasyMock.expect( compositeFactoryIfIncludedMock.apply(
				EasyMock.eq( expectedPrefixedName ), EasyMock.capture( nestedContextCapture )
		) )
				.andReturn( expectedReturn );
		replayAll();
		Object actualReturn = context.nest( relativeFieldName, compositeFactoryIfIncludedMock, compositeFactoryIfExcludedMock );
		verifyAll();
		assertSame( expectedReturn, actualReturn );
		return nestedContextCapture.getValue();
	}

	private void checkCompositeExcluded(String expectedPrefixedName, IndexSchemaNestingContext context,
			String relativeFieldName) {
		checkCompositeExcluded( expectedPrefixedName, context, relativeFieldName, true );
	}

	private void checkCompositeExcluded(String expectedPrefixedName, IndexSchemaNestingContext context,
			String relativeFieldName, boolean recurse) {
		Capture<IndexSchemaNestingContext> nestedContextCapture = newCapture();
		resetAll();
		Object expectedReturn = new Object();
		EasyMock.expect( compositeFactoryIfExcludedMock.apply(
				EasyMock.eq( expectedPrefixedName ), EasyMock.capture( nestedContextCapture )
		) )
				.andReturn( expectedReturn );
		replayAll();
		Object actualReturn = context.nest( relativeFieldName, compositeFactoryIfIncludedMock, compositeFactoryIfExcludedMock );
		verifyAll();
		assertSame( expectedReturn, actualReturn );

		if ( recurse ) {
			// Also check that leafs will be excluded
			checkFooBarExcluded( "", nestedContextCapture.getValue(), false );
		}
	}

	private IndexSchemaNestingContextImpl checkSimpleIndexedEmbeddedIncluded(String expectedObjectName,
			IndexSchemaNestingContextImpl context, MappableTypeModel typeModel,
			String relativePrefix, Integer depth, Set<String> includePaths) {
		Capture<IndexSchemaNestingContextImpl> nestedContextCapture = newCapture();
		resetAll();
		Object expectedReturn = new Object();
		nestedContextBuilderMock.appendObject( expectedObjectName );
		EasyMock.expect( nestedContextBuilderMock.build( EasyMock.capture( nestedContextCapture ) ) )
				.andReturn( expectedReturn );
		replayAll();
		Optional<Object> actualReturn = context.addIndexedEmbeddedIfIncluded( typeModel, relativePrefix,
				depth, includePaths, nestedContextBuilderMock );
		assertNotNull( "Expected addIndexedEmbeddedIfIncluded to return a non-null result", actualReturn );
		assertTrue( "Expected the indexedEmbedded to be included in " + context, actualReturn.isPresent() );
		verifyAll();
		assertSame( expectedReturn, actualReturn.get() );
		return nestedContextCapture.getValue();
	}

	private void checkSimpleIndexedEmbeddedExcluded(IndexSchemaNestingContextImpl context, MappableTypeModel typeModel,
			String relativePrefix, Integer depth, Set<String> includePaths) {
		resetAll();
		replayAll();
		Optional<Object> actualReturn = context.addIndexedEmbeddedIfIncluded( typeModel, relativePrefix,
				depth, includePaths, nestedContextBuilderMock );
		verifyAll();
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

	private void checkFooBarIndexedEmbeddedExcluded(IndexSchemaNestingContextImpl context, MappableTypeModel typeModel) {
		checkSimpleIndexedEmbeddedExcluded(
				context, typeModel, "foo.", null, null
		);
		checkSimpleIndexedEmbeddedExcluded(
				context, typeModel, "prefix1_", null, null
		);
		checkSimpleIndexedEmbeddedExcluded(
				context, typeModel, "foo.prefix1_", null, null
		);
		checkSimpleIndexedEmbeddedExcluded(
				context, typeModel, "foo.bar.prefix1_", null, null
		);
		Set<String> includePaths = new HashSet<>();
		includePaths.add( "foo" );
		includePaths.add( "bar" );
		checkSimpleIndexedEmbeddedExcluded(
				context, typeModel, "foo", 3, includePaths
		);
	}

	private interface StubLeafFactoryFunction extends Function<String, Object> {
	}

	private interface StubCompositeFactoryFunction extends BiFunction<String, IndexSchemaNestingContext, Object> {
	}

	private interface StubNestedContextBuilder extends IndexSchemaNestingContextImpl.NestedContextBuilder<Object> {
	}
}
