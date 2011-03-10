package org.hibernate.search.query.facet;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;

import org.hibernate.search.filter.ChainedFilter;


/**
 * A Lucene filter which collects {@link Facet}s for filtering.
 *
 * @author Hardy Ferentschik
 */
public class FacetFilter extends Filter {
	// use delegation to implement chaining
	private final ChainedFilter filterChain;

	public FacetFilter() {
		filterChain = new ChainedFilter();
	}

	@Override
	public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
		return filterChain.getDocIdSet( reader );
	}

	/**
	 * Add the specified facet to this chained filter.
	 *
	 * @param facet the facet to add to the filter
	 */
	public void addFacet(Facet facet) {
		filterChain.addFilter( facet.getFacetFilter() );
	}

	/**
	 * Removes the specified facet from the chain of filters.
	 *
	 * @param facet the facet to remove
	 *
	 * @return {@code true} if this facet filter contained the specified facet, {@code false} otherwise.
	 */
	public boolean removeFacet(Facet facet) {
		return filterChain.removeFilter( facet.getFacetFilter() );
	}

	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer();
		sb.append( "FacetFilter" );
		sb.append( "{filterChain=" ).append( filterChain );
		sb.append( '}' );
		return sb.toString();
	}
}


