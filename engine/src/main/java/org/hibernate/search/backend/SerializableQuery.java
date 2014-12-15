package org.hibernate.search.backend;

import java.io.Serializable;

/**
 * interface for Serializable Queries that can be passed safely between VMs (we cannot rely on
 * Lucene's queries here because of that).
 * 
 * @author Martin Braun
 */
public interface SerializableQuery extends Serializable {

	/**
	 * used to identify the type of query faster (no need for instanceof checks)
	 * in the Delegate
	 * 
	 * @return
	 */
	public int getQueryKey();

}
