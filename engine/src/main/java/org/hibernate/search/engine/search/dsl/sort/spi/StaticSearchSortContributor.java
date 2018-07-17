/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.sort.spi;

import java.util.function.Consumer;

class StaticSearchSortContributor<B> implements SearchSortContributor<B> {
	private final B builder;

	StaticSearchSortContributor(B builder) {
		this.builder = builder;
	}

	@Override
	public void contribute(Consumer<? super B> collector) {
		collector.accept( builder );
	}
}
