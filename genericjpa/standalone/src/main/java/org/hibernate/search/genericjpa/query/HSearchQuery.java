/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.query;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;

import org.hibernate.search.filter.FullTextFilter;
import org.hibernate.search.genericjpa.entity.EntityProvider;
import org.hibernate.search.query.engine.spi.FacetManager;
import org.hibernate.search.spatial.Coordinates;

public interface HSearchQuery {

	// TODO: check if more methods are from hsquery are needed here

	HSearchQuery sort(Sort sort);

	HSearchQuery filter(Filter filter);

	HSearchQuery firstResult(int firstResult);

	HSearchQuery maxResults(int maxResults);

	HSearchQuery setTimeout(long timeout, TimeUnit timeUnit);

	HSearchQuery limitExecutionTimeTo(long timeout, TimeUnit timeUnit);

	HSearchQuery setSpatialParameters(double latitude, double longitude, String fieldName);

	HSearchQuery setSpatialParameters(Coordinates center, String fieldName);

	HSearchQuery hints(Map<String, Object> hints);

	Query getLuceneQuery();

	<R> List<R> queryDto(Class<R> returnedType);

	<R> List<R> queryDto(Class<R> returnedType, String profileName);

	List<Object[]> queryProjection(String... projection);

	int queryResultSize();

	FullTextFilter enableFullTextFilter(String name);

	void disableFullTextFilter(String name);

	boolean hasPartialResults();

	/**
	 * @return return the manager for all faceting related operations
	 */
	FacetManager getFacetManager();

	Explanation explain(int documentId);

	@SuppressWarnings("rawtypes")
	List query(EntityProvider entityProvider, Fetch fetchType);

	@SuppressWarnings("rawtypes")
	default List query(EntityProvider entityProvider) {
		return this.query( entityProvider, Fetch.FIND_BY_ID );
	}

	enum Fetch {
		BATCH, FIND_BY_ID
	}

}
