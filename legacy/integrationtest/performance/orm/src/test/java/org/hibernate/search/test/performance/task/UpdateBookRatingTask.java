/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.performance.task;

import java.util.Random;

import org.hibernate.LockMode;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.test.performance.model.Book;
import org.hibernate.search.test.performance.scenario.TestScenarioContext;

/**
 * @author Tomas Hradec
 */
public class UpdateBookRatingTask extends AbstractTask {

	private static final float MAX_RATING = 100;
	private static final Random RANDOM_RATING = new Random();

	public UpdateBookRatingTask(TestScenarioContext ctx) {
		super( ctx );
	}

	@Override
	protected void execute(FullTextSession fts) {
		long bookId = ctx.getRandomBookId();
		// See HSEARCH-2916, we often had concurrent write errors here
		Book book = (Book) fts.get( Book.class, bookId, LockMode.PESSIMISTIC_WRITE );
		if ( book != null ) {
			book.setRating( Math.abs( RANDOM_RATING.nextFloat() ) * MAX_RATING );
		}
	}

}
