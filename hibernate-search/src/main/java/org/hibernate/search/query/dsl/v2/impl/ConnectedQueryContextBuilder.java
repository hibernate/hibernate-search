package org.hibernate.search.query.dsl.v2.impl;

import org.hibernate.search.SearchFactory;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.query.dsl.v2.EntityContext;
import org.hibernate.search.query.dsl.v2.QueryBuilder;
import org.hibernate.search.query.dsl.v2.QueryContextBuilder;
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
		private final SearchFactoryImplementor factory;

		public HSearchEntityContext(Class<?> entityType, SearchFactoryImplementor factory) {
			this.factory = factory;
			queryAnalyzer = new ScopedAnalyzer();
			queryAnalyzer.setGlobalAnalyzer( factory.getAnalyzer( entityType ) );
		}

		public EntityContext overridesForField(String field, String analyzerName) {
			queryAnalyzer.addScopedAnalyzer( field, factory.getAnalyzer( analyzerName ) );
			return this;
		}

		public QueryBuilder get() {
			return new ConnectedQueryBuilder(queryAnalyzer, factory);
		}
	}
}
