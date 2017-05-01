/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.impl;

import org.hibernate.search.FullTextSession;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;


/**
 * Creates concrete instances of FullTextSession and SearchFactory without exposing the underlying types.
 *
 * @author Sanne Grinovero (C) 2014 Red Hat Inc.
 */
public final class ImplementationFactory {

	private static final Log log = LoggerFactory.make();

	private ImplementationFactory() {
		//not meant to be instantiated
	}

	public static FullTextSession createFullTextSession(org.hibernate.Session session) {
		if ( session == null ) {
			throw log.getNullSessionPassedToFullTextSessionCreationException();
		}
		else {
			return new FullTextSessionImpl( session );
		}
	}

	public static SearchFactory createSearchFactory(ExtendedSearchIntegrator searchIntegrator) {
		return new SearchFactoryImpl( searchIntegrator );
	}

}
