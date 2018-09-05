/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.facet;

import org.apache.lucene.search.Query;

/**
 * A single facet (field value and count).
 *
 * @author Hardy Ferentschik
 */
public interface Facet {
	/**
	 * @return the faceting name this {@code Facet}	belongs to.
	 *
	 * @see org.hibernate.search.query.facet.FacetingRequest#getFacetingName()
	 */
	String getFacetingName();

	/**
	 * Return the {@code Document} field name this facet is targeting.
	 * The field needs to be indexed with {@code Analyze.NO}.
	 *
	 * @return the {@code Document} field name this facet is targeting.
	 */
	String getFieldName();

	/**
	 * @return the value of this facet. In case of a discrete facet it is the actual
	 *         {@code Document} field value. In case of a range query the value is a
	 *         string representation of the range.
	 */
	String getValue();

	/**
	 * @return the facet count.
	 */
	int getCount();

	/**
	 * @return a Lucene {@link Query} which which can be executed to retrieve all
	 *         documents matching the value of this facet.
	 */
	Query getFacetQuery();
}


