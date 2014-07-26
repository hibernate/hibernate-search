/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.serialization.javaserialization.impl;

import java.util.Properties;

import org.hibernate.search.engine.service.spi.Startable;
import org.hibernate.search.indexes.serialization.spi.Deserializer;
import org.hibernate.search.indexes.serialization.spi.SerializationProvider;
import org.hibernate.search.indexes.serialization.spi.Serializer;
import org.hibernate.search.spi.BuildContext;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 * @author Hardy Ferentschik
 */
public class NativeJavaSerializationProvider implements SerializationProvider, Startable {

	private Deserializer deserializerInstance;

	@Override
	public Serializer getSerializer() {
		return new NativeJavaSerializer();
	}

	@Override
	public Deserializer getDeserializer() {
		return deserializerInstance;
	}

	@Override
	public String toString() {
		return "Simple Java based SerializationProvider";
	}

	@Override
	public void start(Properties properties, BuildContext context) {
		//The Deserializer is threadsafe and can be reused, the Serializer is not
		this.deserializerInstance = new JavaSerializationDeserializer();
	}
}
