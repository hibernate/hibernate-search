/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.predicate.impl;

import org.hibernate.search.engine.search.predicate.SearchPredicate;

public class StubSearchPredicate implements SearchPredicate {
	private final StubPredicateBuilder builder;

	StubSearchPredicate(
			StubPredicateBuilder builder) {
		this.builder = builder;
	}

	StubPredicateBuilder get() {
		return builder;
	}
}
