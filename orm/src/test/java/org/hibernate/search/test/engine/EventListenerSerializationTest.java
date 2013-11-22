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
package org.hibernate.search.test.engine;

import java.io.IOException;

import org.hibernate.search.event.impl.FullTextIndexEventListener;
import org.hibernate.search.test.SerializationTestHelper;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

/**
 * Tests that the {@code FullTextIndexEventListener} is {@code Serializable}.
 *
 * @author Sanne Grinovero
 */
public class EventListenerSerializationTest {

	@Test
	public void testEventListenerSerializable() throws IOException, ClassNotFoundException {
		FullTextIndexEventListener eventListener = new FullTextIndexEventListener();
		eventListener.addSynchronization( null, null );

		Object secondListener = SerializationTestHelper
				.duplicateBySerialization( eventListener );

		assertNotNull( secondListener );
		assertTrue( secondListener != eventListener );
	}
}
