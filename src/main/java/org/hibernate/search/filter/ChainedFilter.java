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
 * <p>A Filter capable of chaining other filters, so that it's
 * possible to apply several filters on a Query.</p>
 * <p>The resulting filter will only enable result Documents
 * if no filter removed it.</p>
 * 
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public class ChainedFilter extends Filter {
	
	private static final long serialVersionUID = -6153052295766531920L;
	
	private final List<Filter> chainedFilters = new ArrayList<Filter>();

	public void addFilter(Filter filter) {
		this.chainedFilters.add( filter );
	}

	public boolean isEmpty() {
		return chainedFilters.size() == 0;
	}

	public BitSet bits(IndexReader reader) throws IOException {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
		int size = chainedFilters.size();
		if ( size == 0 ) {
			throw new AssertionFailure( "Chainedfilter has no filters to chain for" );
		}
		else if ( size == 1 ) {
			return chainedFilters.get( 0 ).getDocIdSet( reader );
		}
		else {
			List<DocIdSet> subSets = new ArrayList<DocIdSet>( size );
			for ( Filter f : chainedFilters ) {
				subSets.add( f.getDocIdSet( reader ) );
			}
			subSets = FilterOptimizationHelper.mergeByBitAnds( subSets );
			if ( subSets.size() == 1 ) {
				return subSets.get( 0 );
			}
			return new AndDocIdSet( subSets, reader.maxDoc() );
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder( "ChainedFilter [" );
		for (Filter filter : chainedFilters) {
			sb.append( "\n  " ).append( filter.toString() );
		}
		return sb.append("\n]" ).toString();
	}
}
