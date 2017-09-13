/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.jdk9_modules;

import org.hibernate.search.test.integration.jdk9_modules.client.service.MyEntityService;

import org.junit.Assert;
import org.junit.Test;

public class MyEntityServiceIT {

	/*
	 * Test that the service successfully uses Hibernate Search in a JDK9 module.
	 * We don't really care about the features themselves,
	 * but that's the easiest way to check that Hibernate Search is being used.
	 */
	@Test
	public void test() {
		MyEntityService service = new MyEntityService();
		service.add( 1, "foo" );
		service.add( 2, "bar" );
		service.add( 3, "foo bar" );
		Assert.assertEquals( 2, service.search( "foo" ).size() );
	}

}


