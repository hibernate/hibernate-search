/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.FullTextSession;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Creates concrete instances of FullTextSession and SearchFactory without exposing the underlying types.
 *
 * @author Sanne Grinovero (C) 2014 Red Hat Inc.
 */
public final class ImplementationFactory {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

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

	public static SearchFactory createSearchFactory(V5MigrationOrmSearchIntegratorAdapter searchIntegrator) {
		return new SearchFactoryImpl( searchIntegrator );
	}

}
