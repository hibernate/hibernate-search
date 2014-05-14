/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.filter.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;
import org.apache.lucene.util.Bits;
import org.hibernate.search.exception.AssertionFailure;

/**
 * A Filter capable of chaining other filters, so that it's
 * possible to apply several filters on a Query.
 * <p>The resulting filter will only enable result Documents
 * if no filter removed it.</p>
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 * @author Hardy Ferentschik
 */
public class ChainedFilter extends Filter {
	private static final long serialVersionUID = -6153052295766531920L;

	private final List<Filter> chainedFilters = new ArrayList<Filter>();

	/**
	 * Add the specified filter to the chain of filters
	 *
	 * @param filter the filter to add to the filter chain. Cannot be {@code null}.
	 */
	public void addFilter(Filter filter) {
		if ( filter == null ) {
			throw new IllegalArgumentException( "The specified filter cannot be null" );
		}
		this.chainedFilters.add( filter );
	}

	/**
	 * Returns the specified filter from the current filter chain.
	 *
	 * @param filter the filter to remove form the chaim
	 *
	 * @return {@code true} if this chained filter contained the specified filter, {@code false} otherwise.
	 */
	public boolean removeFilter(Filter filter) {
		return this.chainedFilters.remove( filter );
	}

	public boolean isEmpty() {
		return chainedFilters.size() == 0;
	}

	@Override
	public DocIdSet getDocIdSet(AtomicReaderContext context, Bits acceptDocs) throws IOException {
		int size = chainedFilters.size();
		if ( size == 0 ) {
			throw new AssertionFailure( "No filters to chain" );
		}
		else if ( size == 1 ) {
			return chainedFilters.get( 0 ).getDocIdSet( context, acceptDocs );
		}
		else {
			List<DocIdSet> subSets = new ArrayList<DocIdSet>( size );
			for ( Filter f : chainedFilters ) {
				subSets.add( f.getDocIdSet( context, acceptDocs ) );
			}
			subSets = FilterOptimizationHelper.mergeByBitAnds( subSets );
			if ( subSets.size() == 1 ) {
				return subSets.get( 0 );
			}
			final AtomicReader reader = context.reader();
			return new AndDocIdSet( subSets, reader.maxDoc() );
		}
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "ChainedFilter" );
		sb.append( "{chainedFilters=" ).append( chainedFilters );
		sb.append( '}' );
		return sb.toString();
	}

}
