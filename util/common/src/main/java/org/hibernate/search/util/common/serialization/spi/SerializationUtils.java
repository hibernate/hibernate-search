/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.serialization.spi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public final class SerializationUtils {

	private SerializationUtils() {
	}

	public static byte[] serialize(Object object) {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try ( ObjectOutputStream objects = new ObjectOutputStream( bytes ) ) {
			objects.writeObject( object );
		}
		catch (IOException e) {
			throw new IllegalStateException( "Unexpected exception serializing " + object, e );
		}
		return bytes.toByteArray();
	}

	public static <T> T deserialize(Class<T> type, byte[] bytes) {
		ByteArrayInputStream bytesIn = new ByteArrayInputStream( bytes );
		try ( ObjectInputStream objects = new ObjectInputStream( bytesIn ) ) {
			return type.cast( objects.readObject() );
		}
		catch (IOException | ClassNotFoundException e) {
			throw new IllegalStateException( "Unexpected exception deserializing an object of type " + type, e );
		}
	}

}
