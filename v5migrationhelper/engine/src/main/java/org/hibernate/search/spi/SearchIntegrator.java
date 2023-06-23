/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.spi;

import java.util.function.Consumer;

import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.query.dsl.QueryContextBuilder;
import org.hibernate.search.query.dsl.impl.ConnectedQueryContextBuilder;
import org.hibernate.search.query.engine.impl.HSQueryImpl;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.query.engine.spi.V5MigrationSearchSession;
import org.hibernate.search.scope.spi.V5MigrationSearchScope;

import org.apache.lucene.search.Query;

/**
 * This contract gives access to lower level APIs of Hibernate Search for
 * frameworks integrating with it.
 * Frameworks should not expose this as public API though, but expose a simplified view; for
 * example the Hibernate Search ORM module expose the {@code org.hibernate.search.SearchFactory} contract to
 * its clients.
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 * @deprecated This class will be removed without replacement. Use actual API instead.
 */
@Deprecated
public interface SearchIntegrator {

	V5MigrationSearchScope scope(Class<?>... targetTypes);

	/**
	 * Return an Hibernate Search query object.
	 * <p>This method DOES support non-Lucene backends (e.g. Elasticsearch).
	 * <p>The returned object uses fluent APIs to define additional query settings.
	 *
	 * @param fullTextQuery the full-text engine query
	 * @param session the session (for entity loading)
	 * @param loadOptionsContributor a contributor of loading options (for entity loading)
	 * @param entityTypes the targeted entity types
	 * @param <LOS> The type of the initial step of the loading options definition DSL accessible through
	 * {@link SearchQueryOptionsStep#loading(Consumer)}.
	 * @return an Hibernate Search query object
	 */
	default <LOS> HSQuery createHSQuery(Query fullTextQuery, V5MigrationSearchSession<LOS> session,
			Consumer<LOS> loadOptionsContributor, Class<?>... entityTypes) {
		V5MigrationSearchScope scope = entityTypes == null || entityTypes.length == 0
				? scope( Object.class )
				: scope( entityTypes );
		return new HSQueryImpl<>( scope, session, fullTextQuery, loadOptionsContributor );
	}

	/**
	 * @return return a query builder providing a fluent API to create Lucene queries
	 */
	default QueryContextBuilder buildQueryBuilder() {
		return new ConnectedQueryContextBuilder( this );
	}
}
