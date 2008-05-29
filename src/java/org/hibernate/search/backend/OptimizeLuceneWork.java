// $Id$
package org.hibernate.search.backend;

import java.io.Serializable;

/**
 * @author Andrew Hahn
 * @author Emmanuel Bernard
 */
public class OptimizeLuceneWork extends LuceneWork implements Serializable {
	public OptimizeLuceneWork(Class entity) {
		super( null, null, entity );
	}
}
