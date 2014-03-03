/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.search.backend.impl.jgroups;

import java.nio.charset.Charset;

import org.hibernate.search.SearchException;


/**
 * While we use the configured LuceneWorkSerializer to serialize the Work queue,
 * the JGroups backend needs to prefix the stream with the index name.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
public final class MessageSerializationHelper {

	private static final Charset STRING_ENCODING = Charset.forName( "UTF-8" );

	private MessageSerializationHelper() {
		//not allowed
	}

	/**
	 * Byte encodes a String as a prefix for an existing byte buffer
	 *
	 * @param name the string to encode
	 * @param data the existing buffer
	 * @return a new buffer containing the length of the string buffer, the string buffer and then the data.
	 */
	public static byte[] prependString(final String name, final byte[] data) {
		byte[] string = name.getBytes( STRING_ENCODING );
		if ( string.length > 255 ) {
			throw new SearchException( "Index name is too long to be encoded" );
		}
		byte[] result = new byte[ data.length + string.length + 1 ];
		result[0] = fromIntToByte( string.length );
		System.arraycopy( string, 0, result, 1, string.length );
		System.arraycopy( data, 0, result, 1 + string.length, data.length );
		return result;
	}

	/**
	 * Extracts the string only from the header of a byte array.
	 * Is the reverse operation of {@link #prependString(String, byte[])}
	 * The buffer is not altered.
	 *
	 * @param startingOffset the starting offset of our message in the larger network buffer
	 * @param rawBuffer an array of byte.
	 * @return the String, assuming it's encoded by this same class.
	 */
	public static String extractIndexName(final int startingOffset, final byte[] rawBuffer) {
		int indexNameByteLength = fromByteToInt( rawBuffer[startingOffset] );
		return new String( rawBuffer, startingOffset + 1, indexNameByteLength, STRING_ENCODING );
	}

	/**
	 * Inverse operation of {@link #prependString(String, byte[]): extracts
	 * the original buffer discarding the prefixed string.
	 * The buffer is not altered.
	 *
	 * @param startingOffset the starting offset of our message in the larger network buffer
	 * @param bufferLength we won't attempt to access the buffer beyond this index
	 * @param rawBuffer an array of byte.
	 * @return the smaller byte buffer
	 */
	public static byte[] extractSerializedQueue(final int startingOffset, final int bufferLength, final byte[] rawBuffer) {
		final int indexNameByteLength = fromByteToInt( rawBuffer[startingOffset] );
		final int relevantStartingOffset = startingOffset + 1 + indexNameByteLength;
		byte[] serializedQueue = new byte[ bufferLength - 1 - indexNameByteLength ];
		System.arraycopy( rawBuffer, relevantStartingOffset, serializedQueue, 0, serializedQueue.length );
		return serializedQueue;
	}

	public static int fromByteToInt(byte b) {
		return b & 0xFF;
	}

	public static byte fromIntToByte(int i) {
		if ( i > 255 ) {
			throw new SearchException( "Int is too long to be encoded" );
		}
		return (byte) i;
	}

}
