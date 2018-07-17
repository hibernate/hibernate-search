/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.spi;

/**
 * A {@link SearchPredicateContributor} contributing a pre-determined builder.
 */
class StaticSearchPredicateContributor<B> implements SearchPredicateContributor<B> {
	private final B value;

	StaticSearchPredicateContributor(B value) {
		this.value = value;
	}

	@Override
	public B contribute() {
		return value;
	}
}
