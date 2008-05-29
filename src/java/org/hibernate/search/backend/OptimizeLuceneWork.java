// $Id$
package org.hibernate.search.backend;

/**
 * @author Andrew Hahn
 * @author Emmanuel Bernard
 */
public class OptimizeLuceneWork extends LuceneWork {
	public OptimizeLuceneWork(Class entity) {
		super( null, null, entity );
	}
}
