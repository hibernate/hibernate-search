/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search;

import org.hibernate.Session;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.query.engine.spi.QueryDescriptor;

/**
 * Extends the Hibernate {@link Session} with fulltext search and indexing capabilities.
 *
 * @author Emmanuel Bernard
 */
public interface FullTextSession extends Session, FullTextEntityManager {

	@Override
	FullTextQuery createFullTextQuery(org.apache.lucene.search.Query luceneQuery, Class<?>... entities);

	@Override
	FullTextQuery createFullTextQuery(QueryDescriptor descriptor, Class<?>... entities);

	@Override
	FullTextSharedSessionBuilder sessionWithOptions();

}
