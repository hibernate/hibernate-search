/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.java.module.service;


import org.junit.Assert;
import org.junit.Test;

public class AuthorServiceIT {

	/*
	 * Test that the service successfully uses Hibernate Search in a JDK9 module.
	 * We don't really care about the features themselves,
	 * but that's the easiest way to check that Hibernate Search is being used.
	 */
	@Test
	public void test() {
		checkIsInModulePath( Object.class );
		checkIsInModulePath( AuthorService.class );

		AuthorService service = new AuthorService();
		service.add( "foo" );
		service.add( "bar" );
		service.add( "foo bar" );
		Assert.assertEquals( 2, service.search( "foo" ).size() );
	}

	private void checkIsInModulePath(Class<?> clazz) {
		Assert.assertTrue(
				clazz + " should be part of a named module - there is a problem in test setup",
				clazz.getModule().isNamed()
		);
	}
}
