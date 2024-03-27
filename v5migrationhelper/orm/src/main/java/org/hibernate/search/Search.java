/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search;

import java.lang.invoke.MethodHandles;

import org.hibernate.Session;
import org.hibernate.search.impl.ImplementationFactory;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Helper class to get a {@code FullTextSession} from a regular ORM session.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @deprecated Use {@link org.hibernate.search.mapper.orm.Search} instead.
 */
@Deprecated
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
	 * @deprecated Use {@link org.hibernate.search.mapper.orm.Search#session(Session)} instead.
	 */
	@Deprecated
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
