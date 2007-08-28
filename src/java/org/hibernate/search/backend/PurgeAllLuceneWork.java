package org.hibernate.search.backend;

import java.io.Serializable;

/**
 * A unit of work used to purge an entire index.
 *
 * @author John Griffin
 */
public class PurgeAllLuceneWork extends LuceneWork {
	public PurgeAllLuceneWork(Class entity) {
		super( null, null, entity, null );
	}
}
