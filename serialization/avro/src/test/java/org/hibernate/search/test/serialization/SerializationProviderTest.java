/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.serialization;

import org.junit.Before;
import org.junit.Test;

import org.hibernate.search.engine.service.impl.StandardServiceManager;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.indexes.serialization.avro.impl.AvroSerializationProvider;
import org.hibernate.search.indexes.serialization.spi.SerializationProvider;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;

import static org.junit.Assert.assertTrue;

/**
 * @author Hardy Ferentschik
 */
public class SerializationProviderTest {
	private SerializationProvider serializationProvider;

	@Before
	public void setUp() {
		ServiceManager serviceManager = new StandardServiceManager(
				new SearchConfigurationForTest(),
				null
		);
		serializationProvider = serviceManager.requestService( SerializationProvider.class );
	}

	@Test
	public void testCorrectSerializationProviderType() {
		assertTrue( "Wrong serialization provider", serializationProvider instanceof AvroSerializationProvider );
	}

	@Test
	public void testCorrectSerializationProviderVersion() {
		assertTrue( "Unexpected serialization protocol version - " + serializationProvider.toString(),
				serializationProvider.toString().contains( "v1.2" ) );
	}
}
