/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

import org.apache.lucene.analysis.Analyzer;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.exception.AssertionFailure;

/**
 * Keep the query builder contextual information
 *
 * @author Emmanuel Bernard
 */
public class QueryBuildingContext {
	private final ExtendedSearchIntegrator factory;
	private final DocumentBuilderIndexedEntity documentBuilder;
	private final Analyzer queryAnalyzer;
	private final Class<?> entityType;

	public QueryBuildingContext(ExtendedSearchIntegrator factory, Analyzer queryAnalyzer, Class<?> entityType) {
		this.factory = factory;
		this.queryAnalyzer = queryAnalyzer;
		this.entityType = entityType;

		EntityIndexBinding indexBinding = factory.getIndexBinding( entityType );
		if ( indexBinding == null ) {
			throw new AssertionFailure( "Class is not indexed: " + entityType );
		}
		documentBuilder = indexBinding.getDocumentBuilder();
	}

	public ExtendedSearchIntegrator getFactory() {
		return factory;
	}

	public DocumentBuilderIndexedEntity getDocumentBuilder() {
		return documentBuilder;
	}

	public Analyzer getQueryAnalyzer() {
		return queryAnalyzer;
	}

	public Class<?> getEntityType() {
		return entityType;
	}
}
