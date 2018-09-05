/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.impl;

import org.apache.lucene.search.Query;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.query.engine.impl.LuceneHSQuery;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.query.engine.spi.QueryDescriptor;
import org.hibernate.search.spi.CustomTypeMetadata;
import org.hibernate.search.spi.IndexedTypeMap;
import org.hibernate.search.spi.IndexedTypeSet;
import org.hibernate.search.spi.SearchIntegrator;

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
	public HSQuery createHSQuery(SearchIntegrator integrator, IndexedTypeSet types) {
		ExtendedSearchIntegrator extendedIntegrator = integrator.unwrap( ExtendedSearchIntegrator.class );
		return new LuceneHSQuery( luceneQuery, extendedIntegrator, types );
	}

	@Override
	public HSQuery createHSQuery(SearchIntegrator integrator, IndexedTypeMap<CustomTypeMetadata> types) {
		ExtendedSearchIntegrator extendedIntegrator = integrator.unwrap( ExtendedSearchIntegrator.class );
		return new LuceneHSQuery( luceneQuery, extendedIntegrator, types );
	}

	@Override
	public String toString() {
		return luceneQuery.toString();
	}
}
