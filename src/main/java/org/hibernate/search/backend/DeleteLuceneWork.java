//$Id$
package org.hibernate.search.backend;

import java.io.Serializable;

/**
 * @author Emmanuel Bernard
 */
public class DeleteLuceneWork extends LuceneWork implements Serializable {
	
	private static final long serialVersionUID = -854604138119230246L;

	public DeleteLuceneWork(Serializable id, String idInString, Class entity) {
		super( id, idInString, entity );
	}

	@Override
	public <T> T getWorkDelegate(final WorkVisitor<T> visitor) {
		return visitor.getDelegate( this );
	}
	
}
