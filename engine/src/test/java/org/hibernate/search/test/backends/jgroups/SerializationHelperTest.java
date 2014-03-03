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
package org.hibernate.search.test.backends.jgroups;

import java.util.Arrays;

import junit.framework.Assert;

import org.hibernate.search.SearchException;
import org.hibernate.search.backend.impl.jgroups.MessageSerializationHelper;
import org.junit.Test;


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
		System.arraycopy( buffer, 0, mixed, 2, buffer.length );
		Assert.assertEquals( indexName, MessageSerializationHelper.extractIndexName( 0, buffer ) );
		Assert.assertTrue( Arrays.equals( someRandom, MessageSerializationHelper.extractSerializedQueue( 0, buffer.length, buffer ) ) );
	}

}
