/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
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

package org.hibernate.search.test.concurrency;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.lucene.LuceneBackendQueueProcessor;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.util.TestForIssue;
import org.junit.Assert;
import org.junit.Test;

@TestForIssue(jiraKey = "HSEARCH-1623")
public class ConcurrentFlushTest extends SearchTestCase {

	private static final int STORED_ENTRIES = 150;
	private static final AtomicInteger indexedElements = new AtomicInteger();

	public class InsertEntityJob implements Runnable {
		private final SessionFactory sessionFactory;
		private final int jobNumber;

		public InsertEntityJob(SessionFactory sessionFactory, int jobNumber) {
			this.sessionFactory = sessionFactory;
			this.jobNumber = jobNumber;
		}

		public void run() {
			Session session = sessionFactory.openSession();
			try {
				FlushedStuff stuff = new FlushedStuff();
				stuff.id = jobNumber;
				stuff.name = "Some job code #" + jobNumber;
				session.save( stuff );
				session.flush();
			}
			catch (HibernateException e) {
				e.printStackTrace();
			}
			finally {
				session.close();
			}
		}
	}

	@Test
	public void testPropertiesIndexing() {
		ExecutorService executorService = Executors.newFixedThreadPool( 10 );

		for ( int i = 0; i < STORED_ENTRIES; i++ ) {
			executorService.execute( new InsertEntityJob( getSessionFactory(), i ) );
		}
		try {
			executorService.shutdown();
			executorService.awaitTermination( 10, TimeUnit.MINUTES );
		}
		catch (InterruptedException e) {
			Assert.fail( "unexpected error " + e.getMessage() );
		}

		Assert.assertEquals( STORED_ENTRIES, indexedElements.get() );
	}

	@Indexed
	@Entity
	@Table(name = "FLUSHEDSTUFF")
	public static class FlushedStuff {

		@Id
		public int id;

		public String name;

	}

	public static class SlowCountingBackend extends LuceneBackendQueueProcessor {
		@Override
		public void applyWork(List<LuceneWork> workList, IndexingMonitor monitor) {
			// Increment counter
			indexedElements.incrementAndGet();
			// Then sleep
			try {
				Thread.sleep( 10 );
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{ FlushedStuff.class };
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( "hibernate.search.default.worker.backend", SlowCountingBackend.class.getName() );
	}

}
