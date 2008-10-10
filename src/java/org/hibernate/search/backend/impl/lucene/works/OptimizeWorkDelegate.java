package org.hibernate.search.backend.impl.lucene.works;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.hibernate.search.SearchException;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.Workspace;
import org.hibernate.search.backend.impl.lucene.IndexInteractionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* Stateless implementation that performs a OptimizeLuceneWork.
* @see LuceneWorkVisitor
* @see LuceneWorkDelegate
* @author Emmanuel Bernard
* @author Hardy Ferentschik
* @author John Griffin
* @author Sanne Grinovero
*/
class OptimizeWorkDelegate implements LuceneWorkDelegate {
	
	private final Workspace workspace;
	private final Logger log = LoggerFactory.getLogger( AddWorkDelegate.class );

	OptimizeWorkDelegate(Workspace workspace) {
		this.workspace = workspace;
	}

	public IndexInteractionType getIndexInteractionType() {
		return IndexInteractionType.NEEDS_INDEXWRITER;
	}

	public void performWork(LuceneWork work, IndexWriter writer) {
		log.trace( "optimize Lucene index: {}", work.getEntityClass() );
		try {
			writer.optimize();
			workspace.optimize();
		}
		catch (IOException e) {
			throw new SearchException( "Unable to optimize Lucene index: " + work.getEntityClass(), e );
		}
	}

	public void performWork(LuceneWork work, IndexReader reader) {
		throw new UnsupportedOperationException();
	}
	
}
