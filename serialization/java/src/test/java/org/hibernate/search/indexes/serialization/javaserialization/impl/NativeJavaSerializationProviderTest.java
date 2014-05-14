/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.serialization.javaserialization.impl;

import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.service.impl.StandardServiceManager;
import org.hibernate.search.indexes.serialization.spi.SerializationProvider;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Hardy Ferentschik
 */
public class NativeJavaSerializationProviderTest {
	@Test
	public void testCorrectSerializationProviderDetected() {
		ServiceManager serviceManager = new StandardServiceManager(
				new SearchConfigurationForTest(),
				null
		);

		SerializationProvider serializationProvider = serviceManager.requestService( SerializationProvider.class );
		assertTrue( "Wrong serialization provider", serializationProvider instanceof NativeJavaSerializationProvider );
	}
}
