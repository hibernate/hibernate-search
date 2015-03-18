/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend;

/**
 * interface for Serializable Queries that can be passed safely between VMs (we cannot rely on Lucene's queries here
 * because of that).
 *
 * @author Martin Braun
 */
public interface DeletionQuery {

	/**
	 * used to identify the type of query faster (no need for instanceof checks) in the Delegate
	 *
	 * @return the unique query key
	 */
	int getQueryKey();

}
