//$Id$
package org.hibernate.search.backend;

/**
 * Enumeration of the different types of Lucene work. This enumeration is used to specify the type
 * of index operation to be executed. 
 * 
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author John Griffin
 */
public enum WorkType {
	ADD,
	UPDATE,
	DELETE,
	/**
	 * Used to remove a specific instance
	 * of a class from an index.
	 */
	PURGE,
	/**
	 * Used to remove all instances of a
	 * class from an index.
	 */
	PURGE_ALL,
	
	/**
	 * This type is used for batch indexing.
	 */
	INDEX 
}
