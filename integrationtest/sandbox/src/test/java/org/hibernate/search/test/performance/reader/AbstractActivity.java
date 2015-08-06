/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.performance.reader;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.testsupport.TestConstants;

/**
 * @author Sanne Grinovero
 */
public abstract class AbstractActivity implements Runnable {

	private final ThreadLocal<QueryParser> parsers = new ThreadLocal<QueryParser>() {
		@Override
		protected QueryParser initialValue() {
			return new MultiFieldQueryParser(
					new String[] { "name", "physicalDescription", "suspectCharge" },
					TestConstants.standardAnalyzer
			);
		}
	};

	private final SessionFactory sf;
	private final AtomicInteger jobSeed = new AtomicInteger();
	private final CountDownLatch startSignal;

	AbstractActivity(SessionFactory sf, CountDownLatch startSignal) {
		this.startSignal = startSignal;
		this.sf = sf;
	}

	@Override
	public final void run() {
		try {
			startSignal.await();
		}
		catch (InterruptedException e) {
			e.printStackTrace();
			return;
		}
		Session s = sf.openSession();
		try {
			FullTextSession fts = Search.getFullTextSession( s );
			Transaction tx = s.beginTransaction();
			boolean ok = false;
			try {
				doAction( fts, jobSeed.getAndIncrement() );
				ok = true;
			}
			finally {
				if ( ok ) {
					tx.commit();
				}
				else {
					tx.rollback();
				}
			}
		}
		finally {
			s.close();
		}
	}

	protected FullTextQuery getQuery(String queryString, FullTextSession s, Class... classes) {
		Query luceneQuery = null;
		try {
			luceneQuery = parsers.get().parse( queryString );
		}
		catch (ParseException e) {
			e.printStackTrace();
		}
		return s.createFullTextQuery( luceneQuery, classes );
	}

	protected abstract void doAction(FullTextSession s, int jobSeed);

}
