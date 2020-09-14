/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search;

import org.hibernate.Session;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.mapper.orm.session.SearchSession;

/**
 * Extends the Hibernate {@link Session} with fulltext search and indexing capabilities.
 *
 * @author Emmanuel Bernard
 * @deprecated Instead of using Hibernate Search 5 APIs, get a {@link SearchSession}
 * using {@link org.hibernate.search.mapper.orm.Search#session(Session)}.
 * Refer to the <a href="https://hibernate.org/search/documentation/migrate/6.0/">migration guide</a> for more information.
 */
@Deprecated
public interface FullTextSession extends Session, FullTextEntityManager {

	@Override
	FullTextQuery createFullTextQuery(org.apache.lucene.search.Query luceneQuery, Class<?>... entities);

	@Override
	FullTextSharedSessionBuilder sessionWithOptions();

}
