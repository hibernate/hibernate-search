package org.hibernate.search.test.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.DocIdBitSet;
import org.apache.lucene.util.OpenBitSet;
import org.hibernate.search.filter.FilterOptimizationHelper;

import junit.framework.TestCase;

/**
 * Used to test org.hibernate.search.filter.FiltersOptimizationHelper
 * @see org.hibernate.search.filter.FilterOptimizationHelper
 * @author Sanne Grinovero
 */
public class FiltersOptimizationTest extends TestCase {
	
	/**
	 * in some cases optimizations are not possible,
	 * test that mergeByBitAnds returns the same instance
	 * in that case.
	 */
	public void testSkipMerging() {
		List<DocIdSet> dataIn = new ArrayList<DocIdSet>( 3 );
		dataIn.add( makeOpenBitSetTestSet( 1,2,3,5,8,9,10,11 ) );
		dataIn.add( makeBitSetTestSet( 1,2,3,5,8,9,10,11,20 ) );
		dataIn.add( makeAnonymousTestSet( 1,2,3,5,8,9,10,11 ) );
		dataIn.add( makeAnonymousTestSet( 1,2,3,5,8,9,10,11,12 ) );
		List<DocIdSet> merge = FilterOptimizationHelper.mergeByBitAnds( dataIn );
		assertSame( dataIn, merge );
	}
	
	/**
	 * In case two filters are of OpenBitSet implementation,
	 * they should be AND-ed by using bit operations
	 * (rather than build the iterator).
	 * @throws IOException should not be thrown
	 */
	public void testDoMergingOnOpenBitSet() throws IOException {
		List<DocIdSet> dataIn = new ArrayList<DocIdSet>( 3 );
		dataIn.add( makeOpenBitSetTestSet( 1,2,5,8,9,10,11 ) );
		dataIn.add( makeOpenBitSetTestSet( 1,2,3,5,8,11 ) );
		DocIdSet unmergedSet = makeAnonymousTestSet( 1,2,3,5,8,9,10,11 );
		dataIn.add( unmergedSet );
		List<DocIdSet> merge = FilterOptimizationHelper.mergeByBitAnds( dataIn );
		assertNotSame( dataIn, merge );
		
		assertEquals( 2, merge.size() );
		assertSame( unmergedSet, merge.get( 0 ) );
		assertTrue( isIdSetSequenceSameTo( merge.get( 1 ), 1,2,5,8,11 ) );
	}
	
	/**
	 * In case two filters are of OpenBitSet implementation,
	 * they should be AND-ed by using bit operations
	 * (rather than build the iterator).
	 * @throws IOException should be thrown
	 */
	public void testDoMergingOnJavaBitSet() throws IOException {
		List<DocIdSet> dataIn = new ArrayList<DocIdSet>( 3 );
		dataIn.add( makeBitSetTestSet( 1,2,5,8,9,10,11 ) );
		dataIn.add( makeBitSetTestSet( 1,2,3,5,8,11 ) );
		DocIdSet unmergedSet = makeAnonymousTestSet( 1,2,3,5,8,9,10,11 );
		dataIn.add( unmergedSet );
		List<DocIdSet> merge = FilterOptimizationHelper.mergeByBitAnds( dataIn );
		assertNotSame( dataIn, merge );
		
		assertEquals( 2, merge.size() );
		assertSame( unmergedSet, merge.get( 0 ) );
		assertTrue( isIdSetSequenceSameTo( merge.get( 1 ), 1,2,5,8,11 ) );
	}
	
	/**
	 * Used to this test the testcase's helper method isIdSetSequenceSameTo
	 * @throws IOException
	 */
	public void testSelfIdSequenceTester() throws IOException {
		assertTrue( isIdSetSequenceSameTo(
				makeOpenBitSetTestSet( 1,2,3,5,8,11 ),
				1,2,3,5,8,11 ) );
		assertFalse( isIdSetSequenceSameTo(
				makeOpenBitSetTestSet( 1,2,3,5,8 ),
				1,2,3,5,8,11 ) );
		assertFalse( isIdSetSequenceSameTo(
				makeOpenBitSetTestSet( 1,2,3,5,8,11 ),
				1,2,3,5,8 ) );
	}
	
	/**
	 * Verifies if the docIdSet is representing a specific
	 * sequence of docIds.
	 * @param docIdSet the docIdSet to test
	 * @param expectedIds an array of document ids
	 * @return true if iterating on docIdSet returns the expectedIds
	 * @throws IOException should not happen
	 */
	private boolean isIdSetSequenceSameTo(DocIdSet docIdSet, int...expectedIds) throws IOException {
		DocIdSetIterator idSetIterator = docIdSet.iterator();
		for ( int setBit : expectedIds ) {
			if ( ! idSetIterator.next() ) {
				return false;
			}
			if ( idSetIterator.doc() != setBit ) {
				return false;
			}
		}
		if ( idSetIterator.next() ){
			return false;
		}
		return true;
	}

	/**
	 * test helper, makes an implementation of a DocIdSet
	 * @param docIds the ids it should contain
	 * @return
	 */
	private DocIdSet makeAnonymousTestSet(int... docIds) {
		DocIdSet idSet = makeOpenBitSetTestSet( docIds );
		return new DocIdSetHiddenType( idSet );
	}

	/**
	 * test helper, makes a prefilled OpenBitSet
	 * @param enabledBits the ids it should contain
	 * @return a new OpenBitSet
	 */
	private OpenBitSet makeOpenBitSetTestSet(int... enabledBits) {
		OpenBitSet set = new OpenBitSet();
		for (int position : enabledBits ) {
			// a minimal check for input duplicates:
			assertFalse( set.get( position ) );
			set.set( position );
		}
		return set;
	}
	
	/**
	 * test helper, makes a prefilled DocIdBitSet
	 * using the java.lang.BitSet
	 * @see java.lang.BitSet
	 * @param enabledBits the ids it should contain
	 * @return a ne DocIdBitSet
	 */
	private DocIdBitSet makeBitSetTestSet(int... enabledBits) {
		BitSet set = new BitSet();
		for (int position : enabledBits ) {
			// a minimal check for input duplicates:
			assertFalse( set.get( position ) );
			set.set( position );
		}
		return new DocIdBitSet( set );
	}

	/**
	 * Implementation for testing: wraps a DocIdSet with a new type
	 * to make it not possible to cast/detect to the original type.
	 */
	private class DocIdSetHiddenType extends DocIdSet {

		private final DocIdSet bitSet;

		DocIdSetHiddenType(DocIdSet wrapped) {
			this.bitSet = wrapped;
		}
		
		@Override
		public DocIdSetIterator iterator() {
			return bitSet.iterator();
		}
		
	}

}
