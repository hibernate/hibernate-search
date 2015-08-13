/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl;

/**
 * Represents the next step after comparingField(s).
 * Additional fields can be defined.
 *
 * @hsearch.experimental More Like This queries are considered experimental
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public interface MoreLikeThisOpenedMatchingContext extends MoreLikeThisMatchingContext, FieldCustomization<MoreLikeThisOpenedMatchingContext> {

	/**
	 * Add one field to the fields selected to match the content.
	 *
	 * An exception is thrown if the field are neither storing the term vectors nor physically stored.
	 *
	 * We highly recommend to store the term vectors if you plan on using More Like This queries.
	 * @param fieldname the name of the field
	 * @return {@code this} for method chaining
	 */
	MoreLikeThisOpenedMatchingContext andField(String fieldname);
}
