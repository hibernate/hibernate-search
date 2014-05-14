/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.performance.task;

import static org.hibernate.search.test.performance.scenario.TestContext.ASSERT_QUERY_RESULTS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;

import org.apache.lucene.search.Query;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.test.performance.model.Book;
import org.hibernate.search.test.performance.scenario.TestContext;

/**
 * @author Tomas Hradec
 */
public class QueryBooksByAuthorTask extends AbstractTask {

	public QueryBooksByAuthorTask(TestContext ctx) {
		super( ctx );
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void execute(FullTextSession fts) {
		long authorId = ctx.getRandomAutorId();

		Query q = fts.getSearchFactory()
				.buildQueryBuilder()
				.forEntity( Book.class )
				.get()
				.keyword()
				.onField( "authors.name" )
				.matching( "author" + authorId )
				.createQuery();

		Criteria fetchAuthors = fts.createCriteria( Book.class ).setFetchMode( "authors", FetchMode.JOIN );

		List<Book> result = fts.createFullTextQuery( q, Book.class ).setCriteriaQuery( fetchAuthors ).list();

		if ( ASSERT_QUERY_RESULTS ) {
			assertResult( authorId, result );
			assertResultSize( authorId, result );
		}
	}

	private void assertResult(long authorId, List<Book> result) {
		for ( Book book : result ) {
			assertEquals( book.getAuthors().size(), 1 );
			assertEquals( book.getAuthors().iterator().next().getId().longValue(), authorId );
		}
	}

	private void assertResultSize(long authorId, List<Book> result) {
		long estimatedBooksCount = ctx.bookIdCounter.get();
		long estimatedBooksCountPerAuthor = estimatedBooksCount / TestContext.MAX_AUTHORS;
		long tolerance = 2 * TestContext.THREADS_COUNT;

		if ( result.size() < ( estimatedBooksCountPerAuthor - tolerance )
				|| result.size() > ( estimatedBooksCountPerAuthor + tolerance ) ) {
			fail( "QueryBooksByAuthorTask: authorId=" + authorId + ", actual=" + result.size() + ", estimate=" + estimatedBooksCountPerAuthor );
		}
	}

}
