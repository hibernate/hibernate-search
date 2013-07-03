/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.test.util;

import org.junit.Test;

import org.hibernate.search.SearchException;
import org.hibernate.search.indexes.serialization.avro.impl.AvroSerializationProvider;
import org.hibernate.search.util.impl.FileHelper;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;

/**
 * @author Hardy Ferentschik
 */
public class ResourceLoadingTest {

	@Test
	public void testOpenKnownResource() throws Exception {
		// using a known resource for testing
		String resource = "org/hibernate/search/remote/codex/avro/v1_0/Message.avro";
		String resourceContent = FileHelper.readResourceAsString( resource, AvroSerializationProvider.class.getClassLoader() );
		assertNotNull( resourceContent );
		assertFalse( resourceContent.isEmpty() );
	}

	@Test
	public void testUnKnownResource() throws Exception {
		// using a known resource for testing
		String resource = "foo";
		try {
			FileHelper.readResourceAsString( resource, AvroSerializationProvider.class.getClassLoader() );
		}
		catch (SearchException e) {
			assertEquals( "Wrong error message", "HSEARCH000114: Could not load resource: 'foo'", e.getMessage() );
		}
	}
}
