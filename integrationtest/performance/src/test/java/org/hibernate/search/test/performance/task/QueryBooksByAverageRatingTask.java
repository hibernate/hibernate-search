/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.performance.task;

import java.util.List;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.test.performance.model.Book;
import org.hibernate.search.test.performance.scenario.TestContext;

/**
 * @author Tomas Hradec
 */
public class QueryBooksByAverageRatingTask extends AbstractTask {

	public QueryBooksByAverageRatingTask(TestContext ctx) {
		super( ctx );
	}

	@Override
	@SuppressWarnings({ "unchecked", "unused" })
	protected void execute(FullTextSession fts) {
		Query q = fts.getSearchFactory()
				.buildQueryBuilder()
				.forEntity( Book.class )
				.get()
				.range()
				.onField( "rating" )
				.from( 40.0f )
				.to( 60.0f )
				.createQuery();

		List<Book> result = fts.createFullTextQuery( q, Book.class )
				.setSort( new Sort( new SortField( "rating", SortField.Type.FLOAT, true ) ) )
				.list();
	}

}
