//$Id$
package org.hibernate.search.backend;

import java.io.Serializable;

/**
 * A unit of work used to purge an entire index.
 *
 * @author John Griffin
 */
public class PurgeAllLuceneWork extends LuceneWork implements Serializable {
	
	private static final long serialVersionUID = 8124091288284011715L;

	public PurgeAllLuceneWork(Class entity) {
		super( null, null, entity, null );
	}
	
	@Override
	public <T> T getWorkDelegate(final WorkVisitor<T> visitor) {
		return visitor.getDelegate( this );
	}
	
}
