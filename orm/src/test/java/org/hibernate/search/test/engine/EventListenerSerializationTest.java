/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.engine;

import java.io.IOException;

import org.hibernate.search.event.impl.FullTextIndexEventListener;
import org.hibernate.search.test.SerializationTestHelper;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

/**
 * Tests that the {@code FullTextIndexEventListener} is {@code Serializable}.
 *
 * @author Sanne Grinovero
 */
public class EventListenerSerializationTest {

	@Test
	public void testEventListenerSerializable() throws IOException, ClassNotFoundException {
		FullTextIndexEventListener eventListener = new FullTextIndexEventListener();
		eventListener.addSynchronization( null, null );

		Object secondListener = SerializationTestHelper
				.duplicateBySerialization( eventListener );

		assertNotNull( secondListener );
		assertTrue( secondListener != eventListener );
	}
}
