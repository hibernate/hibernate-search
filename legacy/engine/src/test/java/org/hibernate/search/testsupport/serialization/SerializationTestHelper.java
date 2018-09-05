/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.testsupport.serialization;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * @author Sanne Grinovero
 */
public final class SerializationTestHelper {

	private SerializationTestHelper() {
		// Do not instantiate this class
	}

	/**
	 * Duplicates an object using Serialization, it moves
	 * state to and from a buffer. Should be used to test
	 * correct serializability.
	 * @param o The object to "clone"
	 * @return the clone.
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static <T> T duplicateBySerialization(T o) throws IOException, ClassNotFoundException {
		// Serialize to buffer:
		java.io.ByteArrayOutputStream outStream = new java.io.ByteArrayOutputStream();
		ObjectOutputStream objectOutStream = new ObjectOutputStream( outStream );
		objectOutStream.writeObject( o );
		objectOutStream.flush();
		objectOutStream.close();
		// buffer version of Object:
		byte[] objectBuffer = outStream.toByteArray();
		// deserialize to new instance:
		java.io.ByteArrayInputStream inStream = new ByteArrayInputStream( objectBuffer );
		ObjectInputStream objectInStream = new ObjectInputStream( inStream );
		T copy = (T) objectInStream.readObject();
		return copy;
	}

}
