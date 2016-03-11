/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.backend.lucene;

import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.setup.TransactionContextForTest;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for exception during the {@link org.hibernate.search.backend.impl.lucene.SyncWorkProcessor}
 * shutdown.
 *
 * @author gustavonalle
 */
@RunWith(BMUnitRunner.class)
public class SyncWorkProcessorShutDownTest {

	@Rule
	public SearchFactoryHolder sfAsyncExclusiveIndex = new SearchFactoryHolder( Quote.class )
			.withProperty( "hibernate.search.default.exclusive_index_use", "true" );

	@Test
	@BMRule(targetClass = "org.apache.lucene.index.IndexWriter",
			targetMethod = "commit",
			action = "throw new Error(\"error!\")",
			name = "commitError")
	public void testErrorHandlingDuringCommit() throws Exception {
		writeData( sfAsyncExclusiveIndex );
		sfAsyncExclusiveIndex.getSearchFactory().close();
	}

	private void writeData(SearchFactoryHolder sfHolder) {
		Quote quote = new Quote( 1, "description" );
		Work work = new Work( quote, 1, WorkType.ADD, false );
		TransactionContextForTest tc = new TransactionContextForTest();
		sfHolder.getSearchFactory().getWorker().performWork( work, tc );
		tc.end();
	}

}
