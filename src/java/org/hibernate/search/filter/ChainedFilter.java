// $Id$
package org.hibernate.search.filter;

import java.util.BitSet;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;
import org.apache.lucene.index.IndexReader;
import org.hibernate.annotations.common.AssertionFailure;

/**
 * @author Emmanuel Bernard
 */
public class ChainedFilter extends Filter {
	
	private static final long serialVersionUID = -6153052295766531920L;
	
	private final List<Filter> chainedFilters = new ArrayList<Filter>();

	public void addFilter(Filter filter) {
		this.chainedFilters.add( filter );
	}

	public BitSet bits(IndexReader reader) throws IOException {
		throw new UnsupportedOperationException();
		/*
		if (chainedFilters.size() == 0) throw new AssertionFailure("Chainedfilter has no filters to chain for");
		//we need to copy the first BitSet because BitSet is modified by .logicalOp
		Filter filter = chainedFilters.get( 0 );
		BitSet result = (BitSet) filter.bits( reader ).clone();
		for (int index = 1 ; index < chainedFilters.size() ; index++) {
			result.and( chainedFilters.get( index ).bits( reader ) );
		}
		return result;
		*/
	}
	
	@Override
	public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
		int size = chainedFilters.size();
		if ( size == 0 ) {
			throw new AssertionFailure( "Chainedfilter has no filters to chain for" );
		}
		else if ( size == 1 ) {
			return chainedFilters.get(0).getDocIdSet(reader);
		}
		else {
			List<DocIdSet> subSets = new ArrayList<DocIdSet>( size );
			for ( Filter f : chainedFilters ) {
				subSets.add( f.getDocIdSet( reader ) );
			}
			return new AndDocIdSet( subSets );
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder("ChainedFilter [");
		for (Filter filter : chainedFilters) {
			sb.append( "\n  ").append( filter.toString() );
		}
		return sb.append("\n]" ).toString();
	}
}
