/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.performance.task;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.lucene.search.Query;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.test.performance.model.Book;
import org.hibernate.search.test.performance.scenario.TestScenarioContext;

/**
 * @author Tomas Hradec
 */
public class QueryBooksByTitleTask extends AbstractTask {

	public QueryBooksByTitleTask(TestScenarioContext ctx) {
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

		if ( ctx.testContext.assertQueryResults ) {
			assertTitle( bookId, result );
		}
	}

	private void assertTitle(long bookId, List<Book> result) {
		long estimatedBooksCount = ctx.bookIdCounter.get();
		if ( bookId == 0 || bookId + 2 * ctx.testContext.threadCount > estimatedBooksCount ) {
			return; // noop
		}

		assertEquals( "QueryBooksByTitleTask: boodId=" + bookId + ", result=" + result, 1, result.size() );
		assertTrue( "QueryBooksByTitleTask: boodId=" + bookId + ", book=" + result.get( 0 ), result.get( 0 ).getTitle().contains( Long.toString( bookId ) ) );
	}

}
