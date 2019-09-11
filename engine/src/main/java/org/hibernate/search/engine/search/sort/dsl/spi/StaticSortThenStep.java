/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.sort.dsl.spi;

public final class StaticSortThenStep<B> extends AbstractSortThenStep<B> {
	final B builder;

	public StaticSortThenStep(SearchSortDslContext<?, B> parentDslContext,
			B builder) {
		super( parentDslContext );
		this.builder = builder;
	}

	@Override
	protected B toImplementation() {
		return builder;
	}
}
