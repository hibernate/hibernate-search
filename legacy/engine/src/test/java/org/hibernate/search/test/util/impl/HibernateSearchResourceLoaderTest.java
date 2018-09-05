/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.util.impl;

import java.io.InputStream;

import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.engine.service.impl.StandardServiceManager;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.testsupport.setup.BuildContextForTest;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.hibernate.search.util.impl.HibernateSearchResourceLoader;
import org.hibernate.search.util.impl.StreamHelper;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * @author Hardy Ferentschik
 */
public class HibernateSearchResourceLoaderTest {

	private HibernateSearchResourceLoader resourceLoader;

	@Before
	public void setUp() {
		SearchConfiguration searchConfiguration = new SearchConfigurationForTest();
		BuildContext buildContext = new BuildContextForTest( searchConfiguration );
		ServiceManager serviceManager = new StandardServiceManager( searchConfiguration, buildContext );
		resourceLoader = new HibernateSearchResourceLoader( serviceManager );
	}

	@Test
	public void testOpenKnownResource() throws Exception {
		// using a known resource for testing
		String resource = "log4j.properties";
		InputStream in = resourceLoader.openResource( resource );
		String resourceContent = StreamHelper.readInputStream( in );
		assertNotNull( resourceContent );
		assertFalse( resourceContent.isEmpty() );
	}

	@Test
	public void testUnKnownResource() throws Exception {
		// using a known resource for testing
		String resource = "foo";
		try {
			resourceLoader.openResource( resource );
		}
		catch (SearchException e) {
			assertEquals( "Wrong error message", "HSEARCH000114: Could not load resource: 'foo'", e.getMessage() );
		}
	}
}
