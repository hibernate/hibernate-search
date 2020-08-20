/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.query.dsl.EntityContext;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.dsl.QueryContextBuilder;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;

/**
 * Assuming connection with the search factory
 *
 * @author Emmanuel Bernard
 */
public class ConnectedQueryContextBuilder implements QueryContextBuilder {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private final SearchIntegrator integrator;

	public ConnectedQueryContextBuilder(SearchIntegrator integrator) {
		this.integrator = integrator;
	}

	@Override
	public EntityContext forEntity(Class<?> entityType) {
		return new HSearchEntityContext( entityType );
	}

	public final class HSearchEntityContext implements EntityContext {
		private final Object scope;

		public HSearchEntityContext(Class<?> entityType) {
			throw new UnsupportedOperationException( "To be implemented through the Search 6 DSL" );
		}

		@Override
		public EntityContext overridesForField(String field, String analyzerName) {
			throw new UnsupportedOperationException( "To be implemented through the Search 6 DSL" );
		}

		@Override
		public QueryBuilder get() {
			return new ConnectedQueryBuilder( new QueryBuildingContext( integrator ) );
		}
	}
}
