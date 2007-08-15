//$Id$
package org.hibernate.search.backend;

import java.io.Serializable;

/**
 * @author Emmanuel Bernard
 */
public class DeleteLuceneWork extends LuceneWork {
	public DeleteLuceneWork(Serializable id, String idInString, Class entity) {
		super( id, idInString, entity );
	}
}
