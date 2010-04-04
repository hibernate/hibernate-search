/* $Id$
 * 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.test.errorhandling;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.hibernate.search.Environment;
import org.hibernate.search.SearchException;
import org.hibernate.search.backend.BackendQueueProcessorFactory;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.exception.impl.RethrowErrorHandler;
import org.hibernate.search.impl.SearchFactoryImpl;

/**
 * Verifies the RethrowErrorHandler is able to propagate exceptions back to the
 * committing thread.	
 * 
 * @author Sanne Grinovero
 */
public class RethrowErrorHandlingTest extends LuceneErrorHandlingTest {
	
	@Override
	public void testErrorHandling(){
		SearchFactoryImpl searchFactoryImpl = getSearchFactoryImpl();
		ErrorHandler errorHandler = searchFactoryImpl.getErrorHandler();
		Assert.assertTrue( errorHandler instanceof RethrowErrorHandler );
		BackendQueueProcessorFactory queueProcessorFactory = searchFactoryImpl.getBackendQueueProcessorFactory();
		List<LuceneWork> queue = new ArrayList<LuceneWork>();
		queue.add( new HarmlessWork( "firstWork" ) );
		queue.add( new HarmlessWork( "secondWork" ) );
		Runnable processor = queueProcessorFactory.getProcessor( queue );
		workcounter.set( 0 ); // reset work counter
		processor.run();
		Assert.assertEquals( 2, workcounter.get() );
		
		workcounter.set( 0 ); // reset work counter
		queue.add( new FailingWork( "firstFailure" ) );
		queue.add( new HarmlessWork( "thirdWork" ) );
		queue.add( new HarmlessWork( "fourthWork" ) );
		processor = queueProcessorFactory.getProcessor( queue );
		try {
			processor.run();
			Assert.fail( "should have thrown a SearchException" );
		}
		catch (SearchException se) {
			//expected
		}
	}
	
	protected void configure(org.hibernate.cfg.Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.ERROR_HANDLER, "rethrow" );
	}

}

