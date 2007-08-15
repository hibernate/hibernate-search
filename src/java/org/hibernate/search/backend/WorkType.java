//$Id$
package org.hibernate.search.backend;

/**
 * Enumeration of the different types of Lucene work. This enumeration is used to specify the type
 * of index operation to be executed. 
 * 
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public enum WorkType {
	ADD,
	UPDATE,
	DELETE,
	
	/**
	 * This type is used for batch indexing.
	 */
	INDEX 
}
