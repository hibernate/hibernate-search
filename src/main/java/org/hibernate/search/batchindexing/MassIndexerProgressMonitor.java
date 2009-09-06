package org.hibernate.search.batchindexing;

/**
 * As a MassIndexer can take some time to finish it's job,
 * a MassIndexerProgressMonitor can be defined in the configuration
 * property hibernate.search.worker.indexing.monitor
 * implementing this interface to track indexing performance.
 * 
 * Implementors must:
 * 	be threadsafe
 *  have a no-arg constructor.
 * 
 * @author Sanne Grinovero
 */
public interface MassIndexerProgressMonitor {

	/**
	 * The number of documents sent to the backend;
	 * This is called several times during
	 * the indexing process.
	 * @param increment
	 */
	void documentsAdded(long increment);

	/**
	 * The number of Documents built;
	 * This is called several times and concurrently during
	 * the indexing process.
	 * @param number
	 */
	void documentsBuilt(int number);

	/**
	 * The number of entities loaded from database;
	 * This is called several times and concurrently during
	 * the indexing process.
	 * @param size
	 */
	void entitiesLoaded(int size);

	/**
	 * The total count of entities to be indexed is
	 * added here; It could be called more than once,
	 * the implementation should add them up.
	 * This is called several times and concurrently during
	 * the indexing process.
	 * @param count
	 */
	void addToTotalCount(long count);

}
