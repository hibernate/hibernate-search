/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search;

import org.hibernate.Session;
import org.hibernate.search.impl.ImplementationFactory;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;

/**
 * Helper class to get a {@code FullTextSession} from a regular ORM session.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public final class Search {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private Search() {
	}

	/**
	 * Creates a FullTextSession from a regular Session.
	 * The created instance depends on the passed Session: closing either of them will
	 * close both instances. They both share the same persistence context.
	 *
	 * @param session the hibernate ORM session
	 * @return the new FullTextSession, based on the passed Session
	 * @throws IllegalArgumentException if passed null
	 */
	public static FullTextSession getFullTextSession(Session session) {
		if ( session == null ) {
			throw log.getNullSessionPassedToFullTextSessionCreationException();
		}
		else if ( session instanceof FullTextSession ) {
			return (FullTextSession) session;
		}
		else {
			return ImplementationFactory.createFullTextSession( session );
		}
	}

}
