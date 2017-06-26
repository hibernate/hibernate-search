/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.performance.task;

import org.hibernate.search.FullTextSession;
import org.hibernate.search.test.performance.model.Book;
import org.hibernate.search.test.performance.scenario.TestContext;

/**
 * @author Tomas Hradec
 */
public class UpdateBookTotalSoldTask extends AbstractTask {

	public UpdateBookTotalSoldTask(TestContext ctx) {
		super( ctx );
	}

	@Override
	protected void execute(FullTextSession fts) {
		long bookId = ctx.getRandomBookId();
		Book book = (Book) fts.get( Book.class, bookId );
		if ( book != null ) {
			book.setTotalSold( book.getTotalSold() + 1 );
		}
	}

}
