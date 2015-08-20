/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.util.impl;

import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Unit test for some trivial logging methods.
 */
public class LoggingCreationTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void verifyNullAssertionFailure() {
		thrown.expect( AssertionFailure.class );
		thrown.expectMessage( "HSEARCH000224: Non optional parameter named 'bar' was null" );

		new Foo( null );
	}

	private static class Foo {

		private static Log log = LoggerFactory.make();

		Foo(String bar) {
			if ( bar == null ) {
				throw log.parametersShouldNotBeNull( "bar" );
			}
		}
	}

}
