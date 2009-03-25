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
	ADD(true),
	UPDATE(true),
	DELETE(false),
	COLLECTION(true),
	/**
	 * Used to remove a specific instance
	 * of a class from an index.
	 */
	PURGE(false),
	/**
	 * Used to remove all instances of a
	 * class from an index.
	 */
	PURGE_ALL(false),
	
	/**
	 * This type is used for batch indexing.
	 */
	INDEX(true);

	private final boolean searchForContainers;

	private WorkType(boolean searchForContainers) {
		this.searchForContainers = searchForContainers;
	}

	/**
	 * When references are changed, either null or another one, we expect dirty checking to be triggered (both sides
	 * have to be updated)
	 * When the internal object is changed, we apply the {Add|Update}Work on containedIns
	 */
	public boolean searchForContainers() {
		return this.searchForContainers;
	}
}
