/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jpa;

import java.io.Serializable;
import java.util.List;

/**
 * Implementors define a strategy for transforming query results into the
 * actual application-visible query result list.
 *
 * copy from Hibernate ORM
 *
 * @author Gavin King
 */
public interface ResultTransformer extends Serializable {
	/**
	 * Tuples are the elements making up each "row" of the query result.
	 * The contract here is to transform these elements into the final
	 * row.
	 *
	 * @param tuple The result elements
	 * @param aliases The result aliases ("parallel" array to tuple)
	 * @return The transformed row.
	 */
	Object transformTuple(Object[] tuple, String[] aliases);

	/**
	 * Here we have an opportunity to perform transformation on the
	 * query result as a whole.  This might be useful to convert from
	 * one collection type to another or to remove duplicates from the
	 * result, etc.
	 *
	 * @param collection The result.
	 * @return The transformed result.
	 */
	List transformList(List collection);
}