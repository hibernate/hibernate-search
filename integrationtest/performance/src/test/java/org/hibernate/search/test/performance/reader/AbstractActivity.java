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
package org.hibernate.search.test.performance.reader;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.TestConstants;

/**
 * @author Sanne Grinovero
 */
public abstract class AbstractActivity implements Runnable {

	private final ThreadLocal<QueryParser> parsers = new ThreadLocal<QueryParser>() {
		@Override
		protected QueryParser initialValue() {
			return new MultiFieldQueryParser(
					TestConstants.getTargetLuceneVersion(),
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
