/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search;

import org.hibernate.Session;
import org.hibernate.search.impl.ImplementationFactory;

/**
 * Helper class to get a {@code FullTextSession} from a regular ORM session.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public final class Search {

	private Search() {
	}

	public static FullTextSession getFullTextSession(Session session) {
		if ( session instanceof FullTextSession ) {
			return (FullTextSession) session;
		}
		else {
			return ImplementationFactory.createFullTextSession( session );
		}
	}

}
