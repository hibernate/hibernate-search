/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.serialization.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;

import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Emmanuel Bernard
 */
public final class SerializationHelper {

	private static Log log = LoggerFactory.make();

	private SerializationHelper() {
		//not allowed
	}

	public static byte[] toByteArray(Serializable instance) {
		//no need to close ByteArrayOutputStream
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			ObjectOutputStream stream = new ObjectOutputStream(out);
			stream.writeObject( instance );
			stream.close();
		}
		catch (IOException e) {
			throw log.failToSerializeObject( instance.getClass(), e );
		}
		return out.toByteArray();
	}

	public static <T> T toInstance(byte[] data, Class<T> clazz) {
		try {
			ByteArrayInputStream byteIn = new ByteArrayInputStream( data );
			final ObjectInputStream in = new ClassLoaderAwareObjectInputStream( byteIn, clazz.getClassLoader() );
			try {
				return (T) in.readObject();
			}
			finally {
				in.close();
			}
		}
		catch (IOException e) {
			throw log.failToDeserializeObject( e );
		}
		catch (ClassNotFoundException e) {
			throw log.failToDeserializeObject( e );
		}
	}

	public static Serializable toSerializable(byte[] data, ClassLoader loader) {
		try {
			ByteArrayInputStream byteIn = new ByteArrayInputStream( data );
			final ObjectInputStream in = new ClassLoaderAwareObjectInputStream( byteIn, loader );
			try {
				return (Serializable) in.readObject();
			}
			finally {
				in.close();
			}
		}
		catch (IOException e) {
			throw log.failToDeserializeObject( e );
		}
		catch (ClassNotFoundException e) {
			throw log.failToDeserializeObject( e );
		}
	}

	private static class ClassLoaderAwareObjectInputStream extends ObjectInputStream {

		private ClassLoader classLoader;

		public ClassLoaderAwareObjectInputStream(InputStream in, ClassLoader classLoader) throws IOException {
			super( in );
			this.classLoader = classLoader;
		}

		@Override
		protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
			try {
				Class<?> clazz = Class.forName( desc.getName(), false, classLoader );
				return clazz;
			}
			catch (ClassNotFoundException ex) {
				return super.resolveClass( desc );
			}
		}
	}
}
