package org.hibernate.search.backend.impl.lucene.works;

import java.io.IOException;

import org.apache.lucene.index.IndexWriter;
import org.slf4j.Logger;

import org.hibernate.search.SearchException;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.Workspace;
import org.hibernate.search.batchindexing.IndexerProgressMonitor;
import org.hibernate.search.util.LoggerFactory;

/**
 * Stateless implementation that performs a OptimizeLuceneWork.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author John Griffin
 * @author Sanne Grinovero
 * @see LuceneWorkVisitor
 * @see LuceneWorkDelegate
 */
class OptimizeWorkDelegate implements LuceneWorkDelegate {

	private static final Logger log = LoggerFactory.make();

	private final Workspace workspace;

	OptimizeWorkDelegate(Workspace workspace) {
		this.workspace = workspace;
	}

	public void performWork(LuceneWork work, IndexWriter writer) {
		final Class<?> entityType = work.getEntityClass();
		log.trace( "optimize Lucene index: {}", entityType );
		try {
			writer.optimize();
			workspace.optimize();
		}
		catch ( IOException e ) {
			throw new SearchException( "Unable to optimize Lucene index: " + entityType, e );
		}
	}

	public void logWorkDone(LuceneWork work, IndexerProgressMonitor monitor) {
		// TODO Auto-generated method stub
		
	}

}
