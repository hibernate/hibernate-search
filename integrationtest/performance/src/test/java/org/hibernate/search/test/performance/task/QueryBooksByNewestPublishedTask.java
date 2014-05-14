/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.performance.task;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.test.performance.model.Book;
import org.hibernate.search.test.performance.scenario.TestContext;

/**
 * @author Tomas Hradec
 */
public class QueryBooksByNewestPublishedTask extends AbstractTask {

	public QueryBooksByNewestPublishedTask(TestContext ctx) {
		super( ctx );
	}

	@Override
	protected void execute(FullTextSession fts) {
		Query q = fts.getSearchFactory()
				.buildQueryBuilder()
				.forEntity( Book.class )
				.get()
				.all()
				.createQuery();

		fts.createFullTextQuery( q, Book.class )
				.setSort( new Sort( new SortField( "publicationDate", SortField.Type.STRING, true ) ) )
				.setMaxResults( 100 )
				.list();
	}

}
