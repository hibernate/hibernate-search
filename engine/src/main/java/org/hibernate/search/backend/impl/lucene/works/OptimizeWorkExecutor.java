/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene.works;

import org.hibernate.search.store.Workspace;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.lucene.IndexWriterDelegate;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Stateless implementation that performs a OptimizeLuceneWork.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author John Griffin
 * @author Sanne Grinovero
 * @see IndexUpdateVisitor
 * @see LuceneWorkExecutor
 */
class OptimizeWorkExecutor implements LuceneWorkExecutor {

	private static final Log log = LoggerFactory.make();

	private final Workspace workspace;

	OptimizeWorkExecutor(Workspace workspace) {
		this.workspace = workspace;
	}

	@Override
	public void performWork(LuceneWork work, IndexWriterDelegate delegate, IndexingMonitor monitor) {
		final Class<?> entityType = work.getEntityClass();
		log.tracef( "optimize Lucene index: %s", entityType );
		workspace.performOptimization( delegate.getIndexWriter() );
	}

}
