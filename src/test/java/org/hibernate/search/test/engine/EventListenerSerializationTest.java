package org.hibernate.search.test.engine;

import java.io.IOException;

import junit.framework.TestCase;

import org.hibernate.search.event.FullTextIndexEventListener;
import org.hibernate.search.test.SerializationTestHelper;

/**
 * Tests that the FullTextIndexEventListener is Serializable
 * 
 * @author Sanne Grinovero
 */
public class EventListenerSerializationTest extends TestCase {

	public void testEventListenerSerializable() throws IOException, ClassNotFoundException {
		FullTextIndexEventListener eventListener = new FullTextIndexEventListener();
		eventListener.addSynchronization( null, null );
		Object secondListener = SerializationTestHelper
				.duplicateBySerialization(eventListener);
		assertNotNull(secondListener);
		assertFalse(secondListener == eventListener);
	}

}
