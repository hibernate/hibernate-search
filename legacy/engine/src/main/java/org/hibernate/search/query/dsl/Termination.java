/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl;

import org.apache.lucene.search.Query;

/**
 * @author Emmanuel Bernard
 */
public interface Termination<T> {
	/**
	 * Return the lucene query representing the operation
	 * @return the lucene query representing the operation
	 */
	Query createQuery();
}
