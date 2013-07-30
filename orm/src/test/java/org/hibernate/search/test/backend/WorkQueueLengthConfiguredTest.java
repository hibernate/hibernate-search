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

import org.hibernate.search.backend.impl.lucene.LuceneBackendQueueProcessor;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.impl.MutableSearchFactory;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.test.SearchTestCaseJUnit4;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Verifies the is <code>max_queue_length</code> parameter for Lucene backend is read.
 * (see HSEARCH-520)
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class WorkQueueLengthConfiguredTest extends SearchTestCaseJUnit4 {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Clock.class };
	}

	@Test
	public void testNothingTest() {
		MutableSearchFactory searchFactory = (MutableSearchFactory) getSearchFactory();
		EntityIndexBinding indexBindingForEntity = searchFactory.getIndexBinding( Clock.class );
		IndexManager[] indexManagers = indexBindingForEntity.getIndexManagers();
		assertEquals( 1, indexManagers.length );
		DirectoryBasedIndexManager indexManager = (DirectoryBasedIndexManager) indexManagers[0];
		LuceneBackendQueueProcessor backend = (LuceneBackendQueueProcessor) indexManager.getBackendQueueProcessor();
		assertEquals( 5, backend.getIndexResources().getMaxQueueLength() );
	}

	@Override
	protected void configure(org.hibernate.cfg.Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( "hibernate.search.default.max_queue_length", "5" );
	}

}
