package org.hibernate.search.backend;

/**
 * interface for Serializable Queries that can be passed safely between VMs (we cannot rely on
 * Lucene's queries here because of that).
 * 
 * @author Martin Braun
 */
public interface DeletionQuery {

	/**
	 * used to identify the type of query faster (no need for instanceof checks)
	 * in the Delegate
	 * 
	 * @return the unique query key
	 */
	public int getQueryKey();

}
