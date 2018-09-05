/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.performance.task;

import org.hibernate.search.FullTextSession;
import org.hibernate.search.test.performance.model.Book;
import org.hibernate.search.test.performance.scenario.TestScenarioContext;

public class SessionMassIndexerTask extends AbstractTask {
	public SessionMassIndexerTask(TestScenarioContext ctx) {
		super( ctx );
	}

	@Override
	protected void execute(FullTextSession fts) {
		try {
			fts.createIndexer( Book.class ).startAndWait();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException( e );
		}
	}
}
