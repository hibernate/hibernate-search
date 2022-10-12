/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.java.modules.orm.lucene.service;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.hibernate.search.mapper.orm.Search;

import org.junit.Test;

public class JavaModulePathIT {

	/*
	 * Test that the service successfully uses Hibernate Search in the module path.
	 * We don't really care about the features themselves,
	 * but the easiest way to check this is to just use Hibernate Search features and see if it works.
	 */
	@Test
	public void test() {
		checkIsInModulePath( Object.class );
		checkIsInModulePath( AuthorService.class );
		checkIsInModulePath( Search.class );

		AuthorService service = new AuthorService();
		service.add( "foo" );
		service.add( "bar" );
		service.add( "foo bar" );
		assertEquals( 2, service.search( "foo" ).size() );

	}

	private void checkIsInModulePath(Class<?> clazz) {
		assertTrue(
				clazz + " should be part of a named module - there is a problem in test setup",
				clazz.getModule().isNamed()
		);
	}
}
