/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.test.performance.util;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;

import org.apache.lucene.index.CheckIndex;
import org.apache.lucene.index.CheckIndex.Status;
import org.apache.lucene.store.Directory;

import org.hibernate.Session;

import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.test.performance.scenario.TestContext;

import static org.hibernate.search.test.performance.scenario.TestContext.CHECK_INDEX_STATE;
import static org.junit.Assert.assertTrue;

/**
 * @author Tomas Hradec
 */
public class CheckerLuceneIndex {

	private CheckerLuceneIndex() {
	}

	@SuppressWarnings("deprecation") // performance tests can be run against older hsearch versions, where isn't getIndexManagerHolder yet
	public static void printIndexReport(TestContext ctx, PrintStream out) {
		if ( !CHECK_INDEX_STATE ) {
			return;
		}

		out.println( "INDEX CHECK..." );
		out.println( "" );

		Session s = ctx.sf.openSession();
		FullTextSession fts = Search.getFullTextSession( s );
		SearchFactoryImplementor sfi = (SearchFactoryImplementor) fts.getSearchFactory();
		Collection<IndexManager> indexManagers = sfi.getIndexManagerHolder().getIndexManagers();

		for ( IndexManager indexManager : indexManagers ) {
			DirectoryBasedIndexManager directoryBasedIndexManager = (DirectoryBasedIndexManager) indexManager;
			DirectoryProvider<?> directoryProvider = directoryBasedIndexManager.getDirectoryProvider();
			Directory directory = directoryProvider.getDirectory();

			out.println( "directory : " + directory.toString() );
			out.println( "" );

			CheckIndex checkIndex = new CheckIndex( directory );
			checkIndex.setInfoStream( out );
			Status status;
			try {
				status = checkIndex.checkIndex();
			}
			catch (IOException e) {
				throw new RuntimeException( e );
			}

			assertTrue( status.clean );
		}

		out.println( "==================================================================" );
		out.flush();
	}

}
