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
import org.hibernate.search.backend.impl.lucene.WorkspaceHolder;
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

	public static void printIndexReport(TestContext ctx, PrintStream out) throws IOException {
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
			stopBackend( directoryBasedIndexManager );
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

	/**
	 * This essentially kills the backend: needed to release the IndexWriter lock
	 * so that the CheckIndex task can be run.
	 * You'll need to ignore further issues if indexing is attempted, and even shutdown
	 * might not be graceful (the shutdown is not designed to be recoverable).
	 */
	private static void stopBackend(DirectoryBasedIndexManager directoryBasedIndexManager) {
		WorkspaceHolder backendQueueProcessor = (WorkspaceHolder) directoryBasedIndexManager.getWorkspaceHolder();
		backendQueueProcessor.getIndexResources().shutdown();
	}

}
