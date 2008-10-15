package org.hibernate.search.backend.impl.lucene.works;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.hibernate.search.SearchException;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.lucene.IndexInteractionType;
import org.hibernate.search.engine.DocumentBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	
	private final Logger log = LoggerFactory.getLogger( PurgeAllWorkDelegate.class );

	PurgeAllWorkDelegate() {
	}

	public IndexInteractionType getIndexInteractionType() {
		return IndexInteractionType.NEEDS_INDEXREADER;
	}

	public void performWork(LuceneWork work, IndexWriter writer) {
		throw new UnsupportedOperationException();
	}

	public void performWork(LuceneWork work, IndexReader reader) {
		log.trace( "purgeAll Lucene index using IndexReader: {}", work.getEntityClass() );
		try {
			Term term = new Term( DocumentBuilder.CLASS_FIELDNAME, work.getEntityClass().getName() );
			reader.deleteDocuments( term );
		}
		catch (Exception e) {
			throw new SearchException( "Unable to purge all from Lucene index: " + work.getEntityClass(), e );
		}
	}
	
}
