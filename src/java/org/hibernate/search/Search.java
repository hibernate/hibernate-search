//$Id$
package org.hibernate.search;

import org.hibernate.Session;
import org.hibernate.search.impl.FullTextSessionImpl;

/**
 * Helper class to get a FullTextSession out of a regular session
 * @author Emmanuel Bernard
 */
public final class Search {

	private Search() {
	}

	public static FullTextSession createFullTextSession(Session session) {
		if (session instanceof FullTextSessionImpl) {
			return (FullTextSession) session;
		}
		else {
			return new FullTextSessionImpl(session);
		}
	}
}