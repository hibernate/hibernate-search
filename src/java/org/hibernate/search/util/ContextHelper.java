//$Id$
package org.hibernate.search.util;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.event.PostInsertEventListener;
import org.hibernate.search.event.FullTextIndexEventListener;
import org.hibernate.search.engine.SearchFactoryImplementor;

/**
 * @author Emmanuel Bernard
 * @deprecated Use {@link org.hibernate.search.FullTextSession#getSearchFactory()} instead.
 */
public abstract class ContextHelper {

	public static SearchFactoryImplementor getSearchFactory(Session session) {
		return getSearchFactoryBySFI( (SessionImplementor) session );
	}

	
	public static SearchFactoryImplementor getSearchFactoryBySFI(SessionImplementor session) {
		PostInsertEventListener[] listeners = session.getListeners().getPostInsertEventListeners();
		FullTextIndexEventListener listener = null;
		//FIXME this sucks since we mandante the event listener use
		for ( PostInsertEventListener candidate : listeners ) {
			if ( candidate instanceof FullTextIndexEventListener ) {
				listener = (FullTextIndexEventListener) candidate;
				break;
			}
		}
		if ( listener == null ) throw new HibernateException(
				"Hibernate Search Event listeners not configured, please check the reference documentation and the " +
						"application's hibernate.cfg.xml" );
		return listener.getSearchFactoryImplementor();
	}
}
