//$Id$
package org.hibernate.search;

import org.hibernate.Session;
import org.hibernate.search.impl.FullTextSessionImpl;

/**
 * Helper class to get a FullTextSession out of a regular session.
 * 
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public final class Search {

	private Search() {
	}

	public static FullTextSession getFullTextSession(Session session) {
		if (session instanceof FullTextSessionImpl) {
			return (FullTextSession) session;
		}
		else {
			return new FullTextSessionImpl(session);
		}
	}
	
	/**
	 * @deprecated As of release 3.1.0, replaced by {@link #getFullTextSession(Session)}
	 */
	@Deprecated 
	public static FullTextSession createFullTextSession(Session session) {
		return getFullTextSession(session);
	}
}