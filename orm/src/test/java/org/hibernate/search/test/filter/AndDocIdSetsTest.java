/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.test.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.DocIdBitSet;
import org.apache.lucene.util.OpenBitSet;
import org.apache.lucene.util.SortedVIntList;
import org.hibernate.search.filter.impl.AndDocIdSet;
import org.junit.Test;

/**
 * Functionality testcase for org.hibernate.search.filter.AndDocIdSet.
 * There is a main method to run some very approximate performance
 * comparisons with the use of java.util.BitSet ands.
 * The numbers show the AndDocIdSet should be used only when it's not
 * possible to rely on a BitSet; in this class however we use BitSet
 * as it's useful to test the implementation.
 *
 * @author Sanne Grinovero
 * @author Hardy Ferentschik
 * @see AndDocIdSet
 * @see BitSet
 */
public class AndDocIdSetsTest {

	static final List<Integer> testDataFrom0to9 = toImmutableList( 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 );
	static final List<Integer> testDataFrom1to10 = toImmutableList( 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 );
	static final List<Integer> testDataFrom1to9 = toImmutableList( 1, 2, 3, 4, 5, 6, 7, 8, 9 );

	private static List<Integer> toImmutableList(int... is) {
		List<Integer> l = new ArrayList<Integer>( is.length );
		for ( int i1 : is ) {
			l.add( i1 );
		}
		return Collections.unmodifiableList( l );
	}

	@SuppressWarnings("unchecked")
	List<Integer> andLists(List<Integer>... lists) {
		if ( lists.length == 0 ) {
			return Collections.EMPTY_LIST;
		}
		List<Integer> result = new ArrayList<Integer>( lists[0] );
		for ( int i = 1; i < lists.length; i++ ) {
			result.retainAll( lists[i] );
		}
		return result;
	}

	// auto-testing of test utility methods for AND operations on test arrays

	@SuppressWarnings("unchecked")
	@Test
	public void testAndingArrays() {
		List<Integer> andLists = andLists( testDataFrom0to9, testDataFrom1to10 );
		assertTrue( andLists.containsAll( testDataFrom1to9 ) );
		assertFalse( andLists.contains( Integer.valueOf( 0 ) ) );
		assertFalse( andLists.contains( Integer.valueOf( 10 ) ) );
		assertTrue( andLists.equals( testDataFrom1to9 ) );
		DocIdSet docIdSet0_9 = arrayToDocIdSet( testDataFrom0to9 );
		DocIdSet docIdSet1_10 = arrayToDocIdSet( testDataFrom1to10 );
		DocIdSet docIdSet1_9 = arrayToDocIdSet( testDataFrom1to9 );
		assertTrue( docIdSetsEqual( docIdSet0_9, docIdSet0_9 ) );
		assertTrue( docIdSetsEqual( docIdSet1_10, docIdSet1_10 ) );
		assertFalse( docIdSetsEqual( docIdSet1_10, docIdSet1_9 ) );
		assertFalse( docIdSetsEqual( docIdSet0_9, docIdSet1_9 ) );
	}

	// auto-testing of test utility methods for conversion in DocIdSetIterator

	@Test
	public void testIteratorMatchesTestArray() throws IOException {
		DocIdSet docIdSet0_9 = arrayToDocIdSet( testDataFrom0to9 );
		DocIdSetIterator docIdSetIterator = docIdSet0_9.iterator();
		assertTrue( docIdSetIterator.nextDoc() != DocIdSetIterator.NO_MORE_DOCS );
		assertEquals( 0, docIdSetIterator.docID() );
		assertEquals( 9, docIdSetIterator.advance( 9 ) );
		assertEquals( DocIdSetIterator.NO_MORE_DOCS, docIdSetIterator.advance( 10 ) );
	}

	@Test
	public void testAndDocIdSets() {
		List<DocIdSet> filters = new ArrayList<DocIdSet>( 2 );
		filters.add( arrayToDocIdSet( testDataFrom0to9 ) );
		filters.add( arrayToDocIdSet( testDataFrom1to10 ) );
		DocIdSet expected = arrayToDocIdSet( testDataFrom1to9 );
		DocIdSet testedSet = new AndDocIdSet( filters, 10 );
		assertTrue( docIdSetsEqual( expected, testedSet ) );
	}

	@Test
	public void testOnRandomBigArrays() {
		onRandomBigArraysTest( 13L );
		onRandomBigArraysTest( 9L );
		onRandomBigArraysTest( 71L );
	}

	public void onRandomBigArraysTest(long randomSeed) {
		List<BitSet> filtersData = makeRandomBitSetList( randomSeed, 4, 1000000, 1500000 );
		BitSet expectedBitset = applyANDOnBitSets( filtersData );
		List<DocIdSet> filters = toDocIdSetList( filtersData );
		DocIdBitSet expectedDocIdSet = new DocIdBitSet( expectedBitset );
		DocIdSet testedSet = new AndDocIdSet( filters, 1500000 );
		assertTrue( docIdSetsEqual( expectedDocIdSet, testedSet ) );
	}

	private static List<DocIdSet> toDocIdSetList(List<BitSet> filtersData) {
		List<DocIdSet> docIdSets = new ArrayList<DocIdSet>( filtersData.size() );
		for ( BitSet bitSet : filtersData ) {
			docIdSets.add( new DocIdBitSet( bitSet ) );
		}
		return docIdSets;
	}

	public static void main(String[] args) throws IOException {
		compareAndingPerformance( 8, 1000000, 1500000 );
		compareAndingPerformance( 4, 1000000, 1500000 );
		compareAndingPerformance( 2, 1000000, 1500000 );
		compareAndingPerformance( 2, 100000000, 150000000 );
		compareAndingPerformance( 4, 100000000, 150000000 );
		compareAndingPerformance( 8, 100000000, 150000000 );
	}

	private static void compareAndingPerformance(final int listSize,
												final int minBitsSize, final int maxBitsSize) throws IOException {
		List<BitSet> filtersData = makeRandomBitSetList( 13L, listSize, minBitsSize, maxBitsSize );
		DocIdSet andedByBitsResult = null;
		DocIdSet andedByIterationResult = null;
		{
			long startTime = System.nanoTime();
			for ( int i = 0; i < 1000; i++ ) {
				BitSet expectedBitset = applyANDOnBitSets( filtersData );
				andedByBitsResult = new DocIdBitSet( expectedBitset );
				// iteration is needed to have a fair comparison with other impl:
				iterateOnResults( andedByBitsResult );
			}
			long totalTimeMs = TimeUnit.NANOSECONDS.toMillis( System.nanoTime() - startTime );
			System.out.println(
					"Time to \"AND " + listSize +
							" BitSets and iterate on results\" 1000 times: " +
							totalTimeMs + "ms. (" +
							minBitsSize + " minimum BitSet size)"
			);
		}
		List<DocIdSet> docIdSetList = toDocIdSetList( filtersData );
		{
			long startTime = System.nanoTime();
			for ( int i = 0; i < 1000; i++ ) {
				andedByIterationResult = new AndDocIdSet( docIdSetList, maxBitsSize );
				// iteration is needed because the AND is been done lazily on iterator access:
				iterateOnResults( andedByIterationResult );
			}
			long totalTimeMs = TimeUnit.NANOSECONDS.toMillis( System.nanoTime() - startTime );
			System.out.println(
					"Time to \"use AndDocIdSet iterator on " + listSize +
							" Filters and iterate on results\" 1000 times: " +
							totalTimeMs + "ms. (" +
							minBitsSize + " minimum BitSet size)"
			);
		}
		System.out.println( " Results are same: " + docIdSetsEqual( andedByBitsResult, andedByIterationResult ) );
	}

	private static void iterateOnResults(DocIdSet docIdBitSet) throws IOException {
		DocIdSetIterator iterator = docIdBitSet.iterator();
		int currentDoc;
		do {
			currentDoc = iterator.nextDoc();
		}
		while ( currentDoc != DocIdSetIterator.NO_MORE_DOCS );
	}

	private static BitSet applyANDOnBitSets(final List<BitSet> filtersData) {
		BitSet andedBitSet = null;
		for ( BitSet bits : filtersData ) {
			if ( andedBitSet == null ) {
				andedBitSet = (BitSet) bits.clone();
			}
			else {
				andedBitSet.and( bits );
			}
		}
		return andedBitSet;
	}

	private static List<BitSet> makeRandomBitSetList(final long randomSeed, final int listSize,
													final int minBitsSize, final int maxBitsSize) {
		Random r = new Random( randomSeed ); //have a fixed Seed for repeatable tests
		List<BitSet> resultList = new ArrayList<BitSet>( listSize );
		for ( int i = 0; i < listSize; i++ ) {
			int arraySize = minBitsSize + r.nextInt( maxBitsSize - minBitsSize );
			resultList.add( makeRandomBitSet( r, arraySize ) );
		}
		return resultList;
	}

	private static BitSet makeRandomBitSet(final Random randomSource, final int maxSize) {
		BitSet bitSet = new BitSet();
		for ( int datai = 0; datai < maxSize; datai++ ) {
			// each bit has 50% change to be set:
			if ( randomSource.nextBoolean() ) {
				bitSet.set( datai );
			}
		}
		return bitSet;
	}

	/**
	 * Converts a list of Integers representing Document ids
	 * into a Lucene DocIdSet
	 *
	 * @param docIdList list of integers to convert
	 *
	 * @return a instance of {@code DocIdBitSet} filled with the integers from the specified list
	 */
	public DocIdSet arrayToDocIdSet(List<Integer> docIdList) {
		BitSet bitset = new BitSet();
		for ( int i : docIdList ) {
			bitset.set( i );
		}
		return new DocIdBitSet( bitset );
	}

	public DocIdSet integersToDocIdSet(int... integers) {
		BitSet bitset = new BitSet();
		for ( int i : integers ) {
			bitset.set( i );
		}
		return new DocIdBitSet( bitset );
	}

	/**
	 * @param expected the doc id set as expected
	 * @param actual the doc id test as returned by the test
	 *
	 * @return true if the two DocIdSet are equal: contain the same number of ids, same order and all are equal
	 */
	public static boolean docIdSetsEqual(DocIdSet expected, DocIdSet actual) {
		try {
			DocIdSetIterator iterA = expected.iterator();
			DocIdSetIterator iterB = actual.iterator();
			int nextA;
			int nextB;
			do {
				nextA = iterA.nextDoc();
				nextB = iterB.nextDoc();
				if ( nextA != nextB ) {
					return false;
				}
				assertEquals( iterA.docID(), iterB.docID() );
			} while ( nextA != DocIdSetIterator.NO_MORE_DOCS );
		}
		catch (IOException ioe) {
			fail( "these DocIdSetIterator instances should not throw any exceptions" );
		}
		return true;
	}

	// HSEARCH-610
	@Test
	public void testWithOpenBitSet() {
		DocIdSet idSet1 = new OpenBitSet( new long[] { 1121 }, 1 ); // bits 0, 5, 6, 10
		DocIdSet idSet2 = new OpenBitSet( new long[] { 64 }, 1 ); // bit 6
		DocIdSet actual = createAndDocIdSet( idSet1, idSet2 );

		DocIdSet expected = integersToDocIdSet( 6 );
		assertTrue( docIdSetsEqual( expected, actual ) );
	}

	// HSEARCH-610
	@Test
	public void testWithDocIdBitSet() {
		DocIdSet idSet1 = integersToDocIdSet( 0, 5, 6, 10 );
		DocIdSet idSet2 = integersToDocIdSet( 6 );
		DocIdSet actual = createAndDocIdSet( idSet1, idSet2 );

		DocIdSet expected = integersToDocIdSet( 6 );
		assertTrue( docIdSetsEqual( expected, actual ) );
	}

	// HSEARCH-610
	@Test
	public void testWithSortedVIntList() {
		SortedVIntList idSet1 = new SortedVIntList( 0, 5, 6, 10 );
		SortedVIntList idSet2 = new SortedVIntList( 6 );
		AndDocIdSet actual = createAndDocIdSet( idSet1, idSet2 );

		DocIdSet expected = integersToDocIdSet( 6 );
		assertTrue( docIdSetsEqual( expected, actual ) );
	}

	@Test
	public void testEmptyDocIdSet() throws Exception {
		DocIdSet idSet1 = new DocIdBitSet( new BitSet() );
		DocIdSet idSet2 = integersToDocIdSet( 0, 5, 6, 10 );
		DocIdSet actual = createAndDocIdSet( idSet1, idSet2 );

		DocIdSet expected = DocIdSet.EMPTY_DOCIDSET;
		assertTrue( docIdSetsEqual( expected, actual ) );
	}

	private AndDocIdSet createAndDocIdSet(DocIdSet... docIdSets) {
		List<DocIdSet> list = new ArrayList<DocIdSet>();
		list.addAll( Arrays.asList( docIdSets ) );
		return new AndDocIdSet( list, 100 );
	}
}
