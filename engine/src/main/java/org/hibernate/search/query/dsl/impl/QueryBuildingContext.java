/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

import org.apache.lucene.analysis.Analyzer;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;

/**
 * Keep the query builder contextual information
 *
 * @author Emmanuel Bernard
 */
public class QueryBuildingContext {
	private final ExtendedSearchIntegrator factory;
	private final Analyzer queryAnalyzer;
	private final Class<?> entityType;

	public QueryBuildingContext(ExtendedSearchIntegrator factory, Analyzer queryAnalyzer, Class<?> entityType) {
		this.factory = factory;
		this.queryAnalyzer = queryAnalyzer;
		this.entityType = entityType;
	}

	public ExtendedSearchIntegrator getFactory() {
		return factory;
	}

	public Analyzer getQueryAnalyzer() {
		return queryAnalyzer;
	}

	public Class<?> getEntityType() {
		return entityType;
	}
}
