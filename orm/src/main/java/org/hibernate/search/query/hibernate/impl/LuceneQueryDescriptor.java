/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.hibernate.impl;

import org.apache.lucene.search.Query;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.query.engine.spi.QueryDescriptor;

/**
 * A {@link QueryDescriptor} for a Lucene query.
 *
 * @author Gunnar Morling
 */
public class LuceneQueryDescriptor implements QueryDescriptor {

	private final Query luceneQuery;

	public LuceneQueryDescriptor(Query luceneQuery) {
		this.luceneQuery = luceneQuery;
	}

	@Override
	public HSQuery createHSQuery(ExtendedSearchIntegrator extendedIntegrator) {
		HSQuery hsQuery = extendedIntegrator.createHSQuery();
		hsQuery.luceneQuery( luceneQuery );

		return hsQuery;
	}

	@Override
	public String toString() {
		return luceneQuery.toString();
	}
}
