/*
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

package org.hibernate.search.test.backend;

import org.hibernate.search.impl.MutableSearchFactory;
import org.hibernate.search.spi.internals.DirectoryProviderData;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.test.Clock;
import org.hibernate.search.test.SearchTestCase;
import org.junit.Test;

/**
 * Verifies the is <code>max_queue_length</code> parameter for Lucene backend is read.
 * (see HSEARCH-520)
 * 
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class WorkQueueLengthConfiguredTest extends SearchTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Clock.class };
	}

	@Test
	public void testNothingTest() {
		MutableSearchFactory searchFactory = (MutableSearchFactory) getSearchFactory();
		DirectoryProvider[] directoryProviders = searchFactory.getDirectoryProviders( Clock.class );
		assertEquals( 1, directoryProviders.length );
		DirectoryProviderData directoryProviderData = searchFactory.getDirectoryProviderData()
			.get( directoryProviders[0] );
		assertEquals( 5, directoryProviderData.getMaxQueueLength() );
	}

	@Override
	protected void configure(org.hibernate.cfg.Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( "hibernate.search.default.max_queue_length", "5" );
	}

}
