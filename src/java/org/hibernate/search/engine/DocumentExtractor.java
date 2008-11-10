//$Id$
package org.hibernate.search.engine;

import java.io.IOException;
import java.io.Serializable;

import org.apache.lucene.document.Document;

import org.hibernate.search.ProjectionConstants;
import org.hibernate.search.query.QueryHits;

/**
 * @author Emmanuel Bernard
 * @author John Griffin
 * @author Hardy Ferentschik
 */
public class DocumentExtractor {
	private final SearchFactoryImplementor searchFactoryImplementor;
	private final String[] projection;
	private final QueryHits queryHits;

	public DocumentExtractor(QueryHits queryHits, SearchFactoryImplementor searchFactoryImplementor, String... projection) {
		this.searchFactoryImplementor = searchFactoryImplementor;
		this.projection = projection;
		this.queryHits = queryHits;
	}

	private EntityInfo extract(Document document) {
		Class clazz = DocumentBuilder.getDocumentClass( document );
		Serializable id = DocumentBuilder.getDocumentId( searchFactoryImplementor, clazz, document );
		Object[] projected = null;
		if ( projection != null && projection.length > 0 ) {
			projected = DocumentBuilder.getDocumentFields( searchFactoryImplementor, clazz, document, projection );
		}
		return new EntityInfo( clazz, id, projected );
	}

	public EntityInfo extract(int index) throws IOException {
		Document doc = queryHits.doc( index );
		//TODO if we are only looking for score (unlikely), avoid accessing doc (lazy load)
		EntityInfo entityInfo = extract( doc );
		Object[] eip = entityInfo.projection;

		if ( eip != null && eip.length > 0 ) {
			for ( int x = 0; x < projection.length; x++ ) {
				if ( ProjectionConstants.SCORE.equals( projection[x] ) ) {
					eip[x] = queryHits.score( index );
				}
				else if ( ProjectionConstants.ID.equals( projection[x] ) ) {
					eip[x] = entityInfo.id;
				}
				else if ( ProjectionConstants.DOCUMENT.equals( projection[x] ) ) {
					eip[x] = doc;
				}
				else if ( ProjectionConstants.DOCUMENT_ID.equals( projection[x] ) ) {
					eip[x] = queryHits.docId( index );
				}
				else if ( ProjectionConstants.BOOST.equals( projection[x] ) ) {
					eip[x] = doc.getBoost();
				}
				else if ( ProjectionConstants.EXPLANATION.equals( projection[x] ) ) {
					eip[x] = queryHits.explain( index );
				}
				else if ( ProjectionConstants.THIS.equals( projection[x] ) ) {
					//THIS could be projected more than once
					//THIS loading delayed to the Loader phase
					entityInfo.indexesOfThis.add( x );
				}
			}
		}
		return entityInfo;
	}
}
