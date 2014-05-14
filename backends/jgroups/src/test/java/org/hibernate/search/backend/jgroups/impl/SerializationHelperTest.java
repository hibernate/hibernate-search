/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.jgroups.impl;

import java.util.Arrays;

import org.hibernate.search.exception.SearchException;
import org.junit.Test;
import org.junit.Assert;


/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
public class SerializationHelperTest {

	@Test
	public void testIntEncoding() {
		for ( int i = 0; i < 256; i++ ) {
			byte byte1 = MessageSerializationHelper.fromIntToByte( i );
			int int1 = MessageSerializationHelper.fromByteToInt( byte1 );
			Assert.assertEquals( i, int1 );
		}
	}

	@Test(expected = SearchException.class)
	public void testIntTooLargeEncoding() {
		MessageSerializationHelper.fromIntToByte( 256 );
	}

	@Test
	public void exampleEncoding() {
		byte[] someRandom = "Some random string to test payload".getBytes();
		String indexName = "this is my favourite index";
		byte[] buffer = MessageSerializationHelper.prependString( indexName, someRandom );
		Assert.assertEquals( indexName, MessageSerializationHelper.extractIndexName( 0, buffer ) );
		Assert.assertTrue( Arrays.equals( someRandom, MessageSerializationHelper.extractSerializedQueue( 0, buffer.length, buffer ) ) );
	}

	@Test
	public void partialBufferEncoding() {
		byte[] someRandom = "Some random string to test payload".getBytes();
		String indexName = "this is my favourite index";
		byte[] buffer = MessageSerializationHelper.prependString( indexName, someRandom );
		byte[] mixed = new byte[ buffer.length + 7 ];
		final int offset = 2;
		System.arraycopy( buffer, 0, mixed, offset, buffer.length );
		Assert.assertEquals( indexName, MessageSerializationHelper.extractIndexName( offset, mixed ) );
		Assert.assertTrue( Arrays.equals( someRandom, MessageSerializationHelper.extractSerializedQueue( offset, buffer.length, mixed ) ) );
	}

}
