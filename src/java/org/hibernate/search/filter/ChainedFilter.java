//$Id$
package org.hibernate.search.filter;

import java.util.BitSet;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

import org.apache.lucene.search.Filter;
import org.apache.lucene.index.IndexReader;
import org.hibernate.annotations.common.AssertionFailure;

/**
 * @author Emmanuel Bernard
 */
public class ChainedFilter extends Filter {
	private List<Filter> chainedFilters = new ArrayList<Filter>();


	public void addFilter(Filter filter) {
		this.chainedFilters.add( filter );
	}

	public BitSet bits(IndexReader reader) throws IOException {
		if (chainedFilters.size() == 0) throw new AssertionFailure("Chainedfilter has no filters to chain for");
		//we need to copy the first BitSet because BitSet is modified by .logicalOp
		Filter filter = chainedFilters.get( 0 );
		BitSet result = (BitSet) filter.bits( reader ).clone();
		for (int index = 1 ; index < chainedFilters.size() ; index++) {
			result.and( chainedFilters.get( index ).bits( reader ) );
		}
		return result;
	}


	public String toString() {
		StringBuilder sb = new StringBuilder("ChainedFilter [");
		for (Filter filter : chainedFilters) {
			sb.append( "\n  ").append( filter.toString() );
		}
		return sb.append("\n]" ).toString();
	}
}
