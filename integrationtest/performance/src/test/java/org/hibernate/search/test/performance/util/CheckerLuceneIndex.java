/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
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
		ExtendedSearchIntegrator integrator = fts.getSearchFactory().unwrap( ExtendedSearchIntegrator.class );
		Collection<IndexManager> indexManagers = integrator.getIndexManagerHolder().getIndexManagers();

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
