/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.backend.lucene;

import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 * Tests for exception during the {@link org.hibernate.search.backend.impl.lucene.SyncWorkProcessor}
 * shutdown.
 *
 * @author gustavonalle
 */
@RunWith(BMUnitRunner.class)
@Category(SkipOnElasticsearch.class) // SyncWorkProcessor is specific to Lucene
public class SyncWorkProcessorShutDownTest {

	@Rule
	public final SearchFactoryHolder sfAsyncExclusiveIndex = new SearchFactoryHolder( Quote.class )
			.withProperty( "hibernate.search.default.exclusive_index_use", "true" );

	private final SearchITHelper helper = new SearchITHelper( sfAsyncExclusiveIndex );

	@Test
	@BMRule(targetClass = "org.apache.lucene.index.IndexWriter",
			targetMethod = "commit",
			action = "throw new Error(\"error!\")",
			name = "commitError")
	public void testErrorHandlingDuringCommit() throws Exception {
		Quote quote = new Quote( 1, "description" );
		helper.add( quote );
		sfAsyncExclusiveIndex.getSearchFactory().close();
	}

}
