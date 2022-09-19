/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;

import org.apache.lucene.search.Sort;

import org.hibernate.Session;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.query.Query;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.mapper.orm.search.loading.dsl.SearchLoadingOptionsStep;
import org.hibernate.search.mapper.orm.session.SearchSession;
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
 * @deprecated Instead of using Hibernate Search 5 APIs, get a {@link SearchSession}
 * using {@link org.hibernate.search.mapper.orm.Search#session(Session)},
 * then create a {@link SearchQuery} with {@link SearchSession#search(Class)}.
 * If you really need an adapter to Hibernate ORM's {@link Query} type,
 * convert that {@link SearchQuery} using {@link org.hibernate.search.mapper.orm.Search#toOrmQuery(SearchQuery)},
 * but be aware that only part of the contract is implemented.
 * Refer to the <a href="https://hibernate.org/search/documentation/migrate/6.0/">migration guide</a> for more information.
 */
@Deprecated
@SuppressWarnings("rawtypes") // We extend the raw version of QueryImplementor on purpose, see HSEARCH-2564
public interface FullTextQuery extends org.hibernate.search.jpa.FullTextQuery, QueryImplementor {

	/**
	 * defines a result transformer used during projection, the Aliases provided are the projection aliases.
	 * @deprecated Instead of using Hibernate Search 5 APIs, get a {@link SearchSession}
	 * using {@link org.hibernate.search.mapper.orm.Search#session(Session)},
	 * create a search query with {@link SearchSession#search(Class)},
	 * and define your projections using {@link SearchQuerySelectStep#select(Function)}.
	 * See in particular the composite projection, which allows applying a function to another projection:
	 * {@link SearchProjectionFactory#composite()}.
	 * Refer to the <a href="https://hibernate.org/search/documentation/migrate/6.0/">migration guide</a> for more information.
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
	FullTextQuery applyGraph(RootGraph graph, GraphSemantic semantic);

	@Override
	default FullTextQuery applyFetchGraph(RootGraph graph) {
		return applyGraph( graph, GraphSemantic.FETCH );
	}

	@Override
	default FullTextQuery applyLoadGraph(RootGraph graph) {
		return applyGraph( graph, GraphSemantic.LOAD );
	}

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

	/**
	 * {@inheritDoc}
	 * @deprecated Instead of using Hibernate Search 5 APIs, get a {@link SearchSession}
	 * using {@link org.hibernate.search.mapper.orm.Search#session(EntityManager)},
	 * create a search query with {@link SearchSession#search(Class)},
	 * and define your loading options using {@link SearchQueryOptionsStep#loading(Consumer)}.
	 * To set the equivalent to {@link #setFetchSize(int)} in Hibernate Search 6,
	 * use {@link SearchLoadingOptionsStep#fetchSize(int)}.
	 * Refer to the <a href="https://hibernate.org/search/documentation/migrate/6.0/">migration guide</a> for more information.
	 */
	@Override
	FullTextQuery setFetchSize(int i);

	@Override
	FullTextQuery setTimeout(long timeout, TimeUnit timeUnit);

	@Override
	FullTextQuery limitExecutionTimeTo(long timeout, TimeUnit timeUnit);

	@Override
	FullTextQuery initializeObjectsWith(ObjectLookupMethod lookupMethod, DatabaseRetrievalMethod retrievalMethod);
}
