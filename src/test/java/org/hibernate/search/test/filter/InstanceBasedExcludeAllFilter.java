package org.hibernate.search.test.filter;

import java.util.BitSet;
import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.DocIdSet;

import org.hibernate.search.filter.EmptyDocIdBitSet;

/**
 * @author Emmanuel Bernard
 */
public class InstanceBasedExcludeAllFilter extends Filter {
	private volatile boolean done = false;

	@Override
	public BitSet bits(IndexReader reader) throws IOException {
		if ( done ) {
			throw new IllegalStateException( "Called twice" );
		}
		BitSet bitSet = new BitSet( reader.maxDoc() );
		done = true;
		return bitSet;
	}

	@Override
	public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
		if ( done ) {
			throw new IllegalStateException( "Called twice" );
		}
		done = true;
		return EmptyDocIdBitSet.instance;
	}
}
