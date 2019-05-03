/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.query.spi;

import org.hibernate.search.engine.search.dsl.query.SearchQueryContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultContext;

public interface SearchQueryContextImplementor<N extends SearchQueryContext<N, Q>, Q>
		extends SearchQueryResultContext<N, Q>, SearchQueryContext<N, Q> {
}
