/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search;

import java.util.concurrent.TimeUnit;

import javax.persistence.FlushModeType;

import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Sort;
import org.hibernate.Criteria;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.search.query.DatabaseRetrievalMethod;
import org.hibernate.search.query.ObjectLookupMethod;
import org.hibernate.search.spatial.Coordinates;
import org.hibernate.transform.ResultTransformer;

/**
 * The base interface for full-text queries using the Hibernate ORM API ({@link org.hibernate.query.Query}).
 * <p>
 * This also extends the JPA counterpart, {@link org.hibernate.search.jpa.FullTextQuery}.
 *
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 */
@SuppressWarnings("rawtypes") // We extend the raw version of QueryImplementor on purpose, see HSEARCH-2564
public interface FullTextQuery extends org.hibernate.search.jpa.FullTextQuery, QueryImplementor {

	/**
	 * defines a result transformer used during projection, the Aliases provided are the projection aliases.
	 */
	@Deprecated
	@Override
	FullTextQuery setResultTransformer(ResultTransformer transformer);

	/**
	 * @param <T> the type of the unwrapped object
	 * @param type the type to unwrap
	 *
	 * @return the underlying type if possible. If not possible to unwrap to the given type an
	 *         {@code IllegalArgumentException} is thrown. Supported types are:
	 *         <ul>
	 *         <li>org.apache.lucene.search.Query the underlying lucene query</li>
	 *         </ul>
	 */
	@Override
	<T> T unwrap(Class<T> type);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// covariant overrides

	@Override
	FullTextQuery setSort(Sort sort);

	@Override
	@Deprecated
	FullTextQuery setFilter(Filter filter);

	@Override
	FullTextQuery setCriteriaQuery(Criteria criteria);

	@Override
	FullTextQuery setProjection(String... fields);

	@Override
	FullTextQuery setSpatialParameters(double latitude, double longitude, String fieldName);

	@Override
	FullTextQuery setSpatialParameters(Coordinates center, String fieldName);

	@Override
	FullTextQuery setFirstResult(int firstResult);

	@Override
	FullTextQuery setMaxResults(int maxResults);

	@Override
	FullTextQuery setHint(String hintName, Object value);

	@Override
	FullTextQuery setFlushMode(FlushModeType flushMode);

	@Override
	FullTextQuery setFetchSize(int i);

	@Override
	FullTextQuery setTimeout(long timeout, TimeUnit timeUnit);

	@Override
	FullTextQuery limitExecutionTimeTo(long timeout, TimeUnit timeUnit);

	@Override
	FullTextQuery initializeObjectsWith(ObjectLookupMethod lookupMethod, DatabaseRetrievalMethod retrievalMethod);
}
