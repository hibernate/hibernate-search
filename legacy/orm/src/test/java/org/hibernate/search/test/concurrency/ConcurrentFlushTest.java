/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.concurrency;

import java.util.List;
import java.util.Map;
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
import org.hibernate.Transaction;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.LocalBackendQueueProcessor;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;
import org.junit.Assert;
import org.junit.Test;

@TestForIssue(jiraKey = "HSEARCH-1623")
public class ConcurrentFlushTest extends SearchTestBase {

	private static final int STORED_ENTRIES = 150;
	private static final AtomicInteger indexedElements = new AtomicInteger();

	public class InsertEntityJob implements Runnable {
		private final SessionFactory sessionFactory;
		private final int jobNumber;

		public InsertEntityJob(SessionFactory sessionFactory, int jobNumber) {
			this.sessionFactory = sessionFactory;
			this.jobNumber = jobNumber;
		}

		@Override
		public void run() {
			Session session = sessionFactory.openSession();
			try {
				Transaction transaction = session.beginTransaction();
				try {
					FlushedStuff stuff = new FlushedStuff();
					stuff.id = jobNumber;
					stuff.name = "Some job code #" + jobNumber;
					session.save( stuff );
					session.flush();
				}
				finally {
					transaction.commit();
				}
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

	public static class SlowCountingBackend extends LocalBackendQueueProcessor {
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
	public Class<?>[] getAnnotatedClasses() {
		return new Class[]{ FlushedStuff.class };
	}

	@Override
	public void configure(Map<String,Object> cfg) {
		cfg.put( "hibernate.search.default.worker.backend", SlowCountingBackend.class.getName() );
	}

}
