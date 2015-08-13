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
public interface AllContext extends QueryCustomization<AllContext>, Termination<AllContext> {
	/**
	 * Exclude the documents matching these queries
	 * @param queriesMatchingExcludedDocuments the queries to use for excluding documents
	 * @return {@code this} for method chaining
	 */
	AllContext except(Query... queriesMatchingExcludedDocuments);
}
