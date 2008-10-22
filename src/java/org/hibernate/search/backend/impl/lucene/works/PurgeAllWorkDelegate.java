package org.hibernate.search.backend.impl.lucene.works;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.slf4j.Logger;

import org.hibernate.search.SearchException;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.lucene.IndexInteractionType;
import org.hibernate.search.engine.DocumentBuilder;
import org.hibernate.search.util.LoggerFactory;

/**
* Stateless implementation that performs a PurgeAllLuceneWork.
* @see LuceneWorkVisitor
* @see LuceneWorkDelegate
* @author Emmanuel Bernard
* @author Hardy Ferentschik
* @author John Griffin
* @author Sanne Grinovero
*/
class PurgeAllWorkDelegate implements LuceneWorkDelegate {
	
	private static final Logger log = LoggerFactory.make();

	PurgeAllWorkDelegate() {
	}

	public IndexInteractionType getIndexInteractionType() {
		return IndexInteractionType.PREFER_INDEXREADER;
	}

	public void performWork(LuceneWork work, IndexWriter writer) {
		log.trace( "purgeAll Lucene index using IndexWriter for type: {}", work.getEntityClass() );
		try {
			Term term = new Term( DocumentBuilder.CLASS_FIELDNAME, work.getEntityClass().getName() );
			writer.deleteDocuments( term );
		}
		catch (Exception e) {
			throw new SearchException( "Unable to purge all from Lucene index: " + work.getEntityClass(), e );
		}
	}

	public void performWork(LuceneWork work, IndexReader reader) {
		log.trace( "purgeAll Lucene index using IndexReader for type: {}", work.getEntityClass() );
		try {
			Term term = new Term( DocumentBuilder.CLASS_FIELDNAME, work.getEntityClass().getName() );
			reader.deleteDocuments( term );
		}
		catch (Exception e) {
			throw new SearchException( "Unable to purge all from Lucene index: " + work.getEntityClass(), e );
		}
	}
	
}
