/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.query.dsl.EntityContext;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.dsl.QueryContextBuilder;
import org.hibernate.search.util.impl.ScopedAnalyzer;

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
