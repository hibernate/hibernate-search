/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.test.backends.lucene;

import java.io.IOException;

import junit.framework.Assert;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.test.util.SearchFactoryHolder;
import org.hibernate.search.test.util.ManualTransactionContext;
import org.hibernate.search.test.util.TestForIssue;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2013 Red Hat Inc.
 */
@TestForIssue(jiraKey = "HSEARCH-1320")
@RunWith(BMUnitRunner.class)
public class SharedReleasesLocksTest {

	@Rule
	public SearchFactoryHolder sfHolder = new SearchFactoryHolder( Book.class )
		.withProperty( "hibernate.search.default.exclusive_index_use", "false" );

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
		Assert.assertFalse( "Index lock leaked!", IndexWriter.isLocked( directory ) );
	}

	private void writeABook(long id, String bookTitle) {
		Book book = new Book();
		book.id = id;
		book.title = bookTitle;
		Work work = new Work( book, book.id, WorkType.ADD, false );
		ManualTransactionContext tc = new ManualTransactionContext();
		sfHolder.getSearchFactory().getWorker().performWork( work, tc );
		tc.end();
	}

	@Indexed(index = "books")
	private static class Book {
		@DocumentId long id;
		@Field String title;
	}

}
