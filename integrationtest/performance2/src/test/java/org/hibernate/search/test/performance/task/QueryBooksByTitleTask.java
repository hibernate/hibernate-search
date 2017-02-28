/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.test.performance.task;

import static org.hibernate.search.test.performance.scenario.TestContext.ASSERT_QUERY_RESULTS;
import static org.hibernate.search.test.performance.scenario.TestContext.THREADS_COUNT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.lucene.search.Query;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.test.performance.model.Book;
import org.hibernate.search.test.performance.scenario.TestContext;

/**
 * @author Tomas Hradec
 */
public class QueryBooksByTitleTask extends AbstractTask {

	public QueryBooksByTitleTask(TestContext ctx) {
		super( ctx );
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void execute(FullTextSession fts) {
		long bookId = ctx.getRandomBookId();

		Query q = fts.getSearchFactory()
				.buildQueryBuilder()
				.forEntity( Book.class )
				.get()
				.keyword()
				.onField( "title" )
				.matching( "title" + bookId )
				.createQuery();

		List<Book> result = fts.createFullTextQuery( q, Book.class ).list();

		if ( ASSERT_QUERY_RESULTS ) {
			assertTitle( bookId, result );
		}
	}

	private void assertTitle(long bookId, List<Book> result) {
		long estimatedBooksCount = ctx.bookIdCounter.get();
		if ( bookId == 0 || bookId + 2 * THREADS_COUNT > estimatedBooksCount ) {
			return; // noop
		}

		assertEquals( "QueryBooksByTitleTask: boodId=" + bookId + ", result=" + result, 1, result.size() );
		assertTrue( "QueryBooksByTitleTask: boodId=" + bookId + ", book=" + result.get( 0 ), result.get( 0 ).getTitle().contains( Long.toString( bookId ) ) );
	}

}
