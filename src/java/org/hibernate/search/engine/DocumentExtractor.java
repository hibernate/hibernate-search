//$Id$
package org.hibernate.search.engine;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Hits;
import org.hibernate.search.engine.EntityInfo;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.ProjectionConstants;

/**
 * @author Emmanuel Bernard
 * @author John Griffin
 */
public class DocumentExtractor {
	private SearchFactoryImplementor searchFactoryImplementor;
	private String[] projection;

	public DocumentExtractor(SearchFactoryImplementor searchFactoryImplementor, String... projection) {
		this.searchFactoryImplementor = searchFactoryImplementor;
		this.projection = projection;
	}

	private EntityInfo extract(Document document) {
		EntityInfo entityInfo = new EntityInfo();
		entityInfo.clazz = DocumentBuilder.getDocumentClass( document );
		entityInfo.id = DocumentBuilder.getDocumentId( searchFactoryImplementor, entityInfo.clazz, document );
		if ( projection != null && projection.length > 0 ) {
			entityInfo.projection = DocumentBuilder.getDocumentFields( searchFactoryImplementor, entityInfo.clazz, document, projection );
		}
		return entityInfo;
	}

	public EntityInfo extract(Hits hits, int index) throws IOException {
		Document doc = hits.doc( index );
		//TODO if we are lonly looking for score (unlikely), avoid accessing doc (lazy load)
		EntityInfo entityInfo = extract( doc );
		Object[] eip = entityInfo.projection;

		if ( eip != null && eip.length > 0 ) {
			for (int x = 0; x < projection.length; x++) {
				if ( ProjectionConstants.SCORE.equals( projection[x] ) ) {
					eip[x] = hits.score( index );
				}
				else if ( ProjectionConstants.ID.equals( projection[x] ) ) {
					eip[x] = entityInfo.id;
				}
				else if ( ProjectionConstants.DOCUMENT.equals( projection[x] ) ) {
					eip[x] = doc;
				}
				else if ( ProjectionConstants.BOOST.equals( projection[x] ) ) {
					eip[x] = doc.getBoost();
				}
				else if ( ProjectionConstants.THIS.equals( projection[x] ) ) {
					//THIS could be projected more than once
					//THIS loading delayed to the Loader phase
					if (entityInfo.indexesOfThis == null) {
						entityInfo.indexesOfThis = new ArrayList<Integer>(1);
					}
					entityInfo.indexesOfThis.add(x);
				}
			}
		}
		return entityInfo;
	}
}
