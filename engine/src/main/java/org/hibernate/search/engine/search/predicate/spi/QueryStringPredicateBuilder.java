/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.spi;

import org.hibernate.search.engine.search.common.RewriteMethod;

public interface QueryStringPredicateBuilder extends CommonQueryStringPredicateBuilder {

	void allowLeadingWildcard(boolean allowLeadingWildcard);

	void enablePositionIncrements(boolean enablePositionIncrements);

	void phraseSlop(Integer phraseSlop);

	void rewriteMethod(RewriteMethod rewriteMethod, Integer n);
}
