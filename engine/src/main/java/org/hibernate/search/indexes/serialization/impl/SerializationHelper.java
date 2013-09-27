/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
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
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
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
			ObjectInputStream in = new ClassLoaderAwareObjectInputStream( byteIn, clazz.getClassLoader() );
			return (T) in.readObject();
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
			ObjectInputStream in = new ClassLoaderAwareObjectInputStream( byteIn, loader );
			return (Serializable) in.readObject();
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
