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
package org.hibernate.search.test.batchindexing;

import junit.framework.Assert;

import org.hibernate.search.FullTextSession;
import org.hibernate.search.MassIndexer;
import org.hibernate.search.SearchException;
import org.hibernate.search.backend.impl.batchlucene.LuceneBatchBackend;
import org.hibernate.search.test.util.FullTextSessionBuilder;

import org.junit.Test;

/**
 * Verifies the batch backend is considering the configuration properties
 * @author Sanne Grinovero
 */
public class BatchBackendConfigurationTest {
	
	/**
	 * Verifies the batch configuration is read by the backend
	 */
	@Test
	public void testConfigurationIsRead() throws InterruptedException {
		FullTextSessionBuilder fsBuilder = new FullTextSessionBuilder()
			.addAnnotatedClass( Book.class )
			.addAnnotatedClass( Nation.class )
			// illegal option:
			.setProperty( LuceneBatchBackend.CONCURRENT_WRITERS, "0" )
			.build();
		
		FullTextSession fullTextSession = fsBuilder.openFullTextSession();
		MassIndexer massIndexer = fullTextSession.createIndexer();
		try {
			massIndexer.startAndWait();
			Assert.fail( "should have thrown an exception as configuration is illegal" );
		}
		catch (SearchException e) {
			// it's ok
		}
		fullTextSession.close();
		fsBuilder.close();
	}
	
	@Test
	public void testConfigurationIsOverriden() throws InterruptedException {
		FullTextSessionBuilder fsBuilder = new FullTextSessionBuilder()
			.addAnnotatedClass( Book.class )
			.addAnnotatedClass( Nation.class )
		// illegal option:
			.setProperty( LuceneBatchBackend.CONCURRENT_WRITERS, "0" ).build();
		try {
			FullTextSession fullTextSession = fsBuilder.openFullTextSession();
			MassIndexer massIndexer = fullTextSession.createIndexer();
			// "fixes" illegal option by override:
			massIndexer.threadsForIndexWriter( 2 );
			massIndexer.startAndWait();
			fullTextSession.close();
		}
		finally {
			fsBuilder.close();
		}
	}

}

