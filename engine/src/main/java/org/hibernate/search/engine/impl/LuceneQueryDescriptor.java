/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.impl;

import java.util.List;

import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.query.engine.spi.QueryDescriptor;

import org.apache.lucene.search.Query;

/**
 * A {@link QueryDescriptor} for a Lucene query.
 *
 * @author Gunnar Morling
 */
public class LuceneQueryDescriptor implements QueryDescriptor {

	private final Query luceneQuery;
	private List<Class<?>> targetEntities;

	public LuceneQueryDescriptor(Query luceneQuery, List<Class<?>> targetEntities) {
		this.luceneQuery = luceneQuery;
		this.targetEntities = targetEntities;
	}

	@Override
	public HSQuery createHSQuery(ExtendedSearchIntegrator extendedIntegrator) {
		HSQuery hsQuery = extendedIntegrator.createHSQuery();
		hsQuery.luceneQuery( luceneQuery ).targetedEntities( targetEntities );

		return hsQuery;
	}

	@Override
	public String toString() {
		return luceneQuery.toString();
	}
}
