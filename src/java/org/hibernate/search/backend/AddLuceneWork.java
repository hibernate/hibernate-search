//$Id$
package org.hibernate.search.backend;

import java.io.Serializable;

import org.apache.lucene.document.Document;

/**
 * @author Emmanuel Bernard
 */
public class AddLuceneWork extends LuceneWork {
	public AddLuceneWork(Serializable id, String idInString, Class entity, Document document) {
		super( id, idInString, entity, document );
	}
}
