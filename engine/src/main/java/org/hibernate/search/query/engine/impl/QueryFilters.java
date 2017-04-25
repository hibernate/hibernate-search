/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.engine.impl;

import java.util.Collections;
import java.util.List;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.BooleanClause.Occur;

/**
 * Internal representation for a set of filters to be applied on a query.
 *
 * @author Sanne Grinovero
 */
public final class QueryFilters {

	public static final QueryFilters EMPTY_FILTERSET = new QueryFilters();

	private final List<Query> filterQueries;

	private QueryFilters() {
		filterQueries = Collections.emptyList();
	}

	public QueryFilters(List<Query> filterQueries) {
		this.filterQueries = filterQueries;
	}

	/**
	 * Will wrap the passed Query into a BooleanQuery to apply all filters as boolean clauses.
	 * Returns the unmodified (same instance) of the input if there are no filters to be applied.
	 * @param queryToFilter The query to be filtered.
	 * @return The filtered query.
	 */
	public Query filterOrPassthrough(Query queryToFilter) {
		if ( isEmpty() ) {
			return queryToFilter;
		}
		else {
			BooleanQuery.Builder boolQueryBuilder = new BooleanQuery.Builder();
			boolQueryBuilder.add( queryToFilter, Occur.MUST );
			for ( Query bc : filterQueries ) {
				boolQueryBuilder.add( bc, BooleanClause.Occur.FILTER );
			}
			return boolQueryBuilder.build();
		}
	}

	/**
	 * @return lists all Lucene query instances which should be applied as filters
	 */
	public List<Query> getFilterQueries() {
		return filterQueries;
	}

	/**
	 * @return <tt>true</tt> if this contains to filters to be applied.
	 */
	public boolean isEmpty() {
		return filterQueries.isEmpty();
	}

}
