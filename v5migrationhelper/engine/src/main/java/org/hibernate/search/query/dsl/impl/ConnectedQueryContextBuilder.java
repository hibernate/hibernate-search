/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.query.dsl.EntityContext;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.dsl.QueryContextBuilder;
import org.hibernate.search.scope.spi.V5MigrationSearchScope;
import org.hibernate.search.spi.SearchIntegrator;

/**
 * Assuming connection with the search factory
 *
 * @author Emmanuel Bernard
 */
public class ConnectedQueryContextBuilder implements QueryContextBuilder {

	private final SearchIntegrator integrator;

	public ConnectedQueryContextBuilder(SearchIntegrator integrator) {
		this.integrator = integrator;
	}

	@Override
	public EntityContext forEntity(Class<?> entityType) {
		return new HSearchEntityContext( entityType );
	}

	public final class HSearchEntityContext implements EntityContext {
		private final V5MigrationSearchScope scope;
		private final Map<String, String> analyzerOverrides = new HashMap<>();

		public HSearchEntityContext(Class<?> entityType) {
			this.scope = integrator.scope( entityType );
		}

		@Override
		public EntityContext overridesForField(String field, String analyzerName) {
			// Not checking that the analyzer exists here, for the sake of simplicity.
			// If the analyzer does not exist, an exception will be thrown later
			// when the field is actually used in an analysis-sensitive query.
			analyzerOverrides.put( field, analyzerName );
			return this;
		}

		@Override
		public QueryBuilder get() {
			return new ConnectedQueryBuilder( new QueryBuildingContext( integrator, scope, analyzerOverrides ) );
		}
	}
}
