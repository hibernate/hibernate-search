/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl;

/**
 * Keep the doors open for customization of the example
 * algorithm tuning like ignore zeros etc.
 *
 * @hsearch.experimental More Like This queries are considered experimental
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public interface MoreLikeThisToEntityContentAndTermination extends Termination<MoreLikeThisToEntityContentAndTermination> {
}
