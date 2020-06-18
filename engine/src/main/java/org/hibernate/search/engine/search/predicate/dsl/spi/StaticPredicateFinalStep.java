/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl.spi;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;

public final class StaticPredicateFinalStep implements PredicateFinalStep {
	private final SearchPredicate predicate;

	public StaticPredicateFinalStep(SearchPredicate predicate) {
		this.predicate = predicate;
	}

	@Override
	public SearchPredicate toPredicate() {
		return predicate;
	}
}
