/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.backend.lucene;

import java.io.IOException;

import org.apache.lucene.store.Directory;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.backend.LuceneBackendTestHelpers;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 * @author Sanne Grinovero (C) 2013 Red Hat Inc.
 */
@TestForIssue(jiraKey = "HSEARCH-1320")
@RunWith(BMUnitRunner.class)
@Category(SkipOnElasticsearch.class) // IndexWriters are specific to Lucene
public class SharedReleasesLocksTest {

	@Rule
	public final SearchFactoryHolder sfHolder = new SearchFactoryHolder( Book.class )
		.withProperty( "hibernate.search.default.exclusive_index_use", "false" );

	private final SearchITHelper helper = new SearchITHelper( sfHolder );

	@Test
	@BMRule(targetClass = "org.hibernate.search.backend.impl.lucene.IndexWriterHolder",
	targetMethod = "getIndexWriter(ErrorContextBuilder)",
	condition = "NOT flagged(\"failedPreviousWrite\")",
	action = "flag(\"failedPreviousWrite\"); \nreturn null; ",
	name = "SharedReleasesLocksTest")
	public void testPropertiesIndexing() throws IOException {
		//The first write operation is going to fail, simulating a lock acquisition timeout:
		writeABook( 1l, "lock contention" );
		//The second write will be successful:
		writeABook( 2l, "no contention" );
		//Now verify the index lock was properly released, not having the backend counters fooled by the initial failure:
		IndexManager indexManager = sfHolder.getSearchFactory().getIndexManagerHolder().getIndexManager( "books" );
		DirectoryBasedIndexManager dbim = (DirectoryBasedIndexManager) indexManager;
		Directory directory = dbim.getDirectoryProvider().getDirectory();
		Assert.assertFalse( "Index lock leaked!", LuceneBackendTestHelpers.isLocked( directory ) );
	}

	private void writeABook(long id, String bookTitle) {
		Book book = new Book();
		book.id = id;
		book.title = bookTitle;
		helper.add( book );
	}

	@Indexed(index = "books")
	private static class Book {
		@DocumentId long id;
		@Field String title;
	}

}
