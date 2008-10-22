package org.hibernate.search.backend.impl.lucene.works;

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.Similarity;
import org.slf4j.Logger;

import org.hibernate.search.SearchException;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.Workspace;
import org.hibernate.search.backend.impl.lucene.IndexInteractionType;
import org.hibernate.search.engine.DocumentBuilder;
import org.hibernate.search.util.LoggerFactory;

/**
 * Stateless implementation that performs a AddLuceneWork.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author John Griffin
 * @author Sanne Grinovero
 * @see LuceneWorkVisitor
 * @see LuceneWorkDelegate
 */
class AddWorkDelegate implements LuceneWorkDelegate {

	private static final Logger log = LoggerFactory.make();

	private final Workspace workspace;

	AddWorkDelegate(Workspace workspace) {
		this.workspace = workspace;
	}

	public IndexInteractionType getIndexInteractionType() {
		return IndexInteractionType.NEEDS_INDEXWRITER;
	}

	public void performWork(LuceneWork work, IndexWriter writer) {
		DocumentBuilder documentBuilder = workspace.getDocumentBuilder( work.getEntityClass() );
		Analyzer analyzer = documentBuilder.getAnalyzer();
		Similarity similarity = documentBuilder.getSimilarity();
		if ( log.isTraceEnabled() ) {
			log.trace(
					"add to Lucene index: {}#{}:{}",
					new Object[] { work.getEntityClass(), work.getId(), work.getDocument() }
			);
		}
		try {
			//TODO the next two operations should be atomic to enable concurrent usage of IndexWriter
			// make a wrapping Similarity based on ThreadLocals? or having it autoselect implementation basing on entity?
			writer.setSimilarity( similarity );
			writer.addDocument( work.getDocument(), analyzer );
			workspace.incrementModificationCounter( 1 );
		}
		catch ( IOException e ) {
			throw new SearchException(
					"Unable to add to Lucene index: "
							+ work.getEntityClass() + "#" + work.getId(), e
			);
		}
	}

	public void performWork(LuceneWork work, IndexReader reader) {
		throw new UnsupportedOperationException();
	}

}
