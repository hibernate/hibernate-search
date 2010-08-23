package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.query.dsl.EntityContext;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.dsl.QueryContextBuilder;
import org.hibernate.search.util.ScopedAnalyzer;

/**
 * Assuming connection with the search factory
 *
 * @author Emmanuel Bernard
 */
public class ConnectedQueryContextBuilder implements QueryContextBuilder {
	private final SearchFactoryImplementor factory;

	public ConnectedQueryContextBuilder(SearchFactoryImplementor factory) {
		this.factory = factory;
	}

	public EntityContext forEntity(Class<?> entityType) {
		return new HSearchEntityContext(entityType, factory );
	}

	public final class HSearchEntityContext implements EntityContext {
		private final ScopedAnalyzer queryAnalyzer;
		private final QueryBuildingContext context;

		public HSearchEntityContext(Class<?> entityType, SearchFactoryImplementor factory) {
			queryAnalyzer = new ScopedAnalyzer();
			queryAnalyzer.setGlobalAnalyzer( factory.getAnalyzer( entityType ) );
			context = new QueryBuildingContext( factory, queryAnalyzer, entityType);
		}

		public EntityContext overridesForField(String field, String analyzerName) {
			queryAnalyzer.addScopedAnalyzer( field, factory.getAnalyzer( analyzerName ) );
			return this;
		}

		public QueryBuilder get() {
			return new ConnectedQueryBuilder(context);
		}
	}
}
