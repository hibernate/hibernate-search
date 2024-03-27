/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.bridge;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.cfg.Configuration;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.DatabaseContainer;

import org.junit.jupiter.api.Test;

/**
 * @author Emmanuel Bernard
 */
class UnresolvedBridgeTest {

	@Test
	void testSerializableType() {
		Configuration cfg = new Configuration();

		// DB properties:
		Map<String, Object> db = new HashMap<>();
		DatabaseContainer.configuration().add( db );
		for ( Map.Entry<String, Object> entry : db.entrySet() ) {
			cfg.setProperty( entry.getKey(), entry.getValue().toString() );
		}

		for ( int i = 0; i < getAnnotatedClasses().length; i++ ) {
			cfg.addAnnotatedClass( getAnnotatedClasses()[i] );
		}
		try {
			cfg.buildSessionFactory();
			fail( "Undefined bridge went through" );
		}
		catch (Exception e) {
			Throwable ee = e;
			boolean hasSearchException = false;
			for ( ;; ) {
				if ( ee == null ) {
					break;
				}
				else if ( ee instanceof SearchException ) {
					hasSearchException = true;
					break;
				}
				ee = ee.getCause();
			}
			assertTrue( hasSearchException );
		}
	}

	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Gangster.class
		};
	}
}
