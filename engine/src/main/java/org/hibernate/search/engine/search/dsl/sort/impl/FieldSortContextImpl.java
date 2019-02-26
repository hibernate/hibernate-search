/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.sort.impl;

import java.util.function.Consumer;

import org.hibernate.search.engine.search.dsl.sort.FieldSortContext;
import org.hibernate.search.engine.search.dsl.sort.FieldSortMissingValueContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.engine.search.dsl.sort.SortOrder;
import org.hibernate.search.engine.search.dsl.sort.spi.NonEmptySortContextImpl;
import org.hibernate.search.engine.search.dsl.sort.spi.SearchSortContributor;
import org.hibernate.search.engine.search.dsl.sort.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.predicate.DslConverter;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;
import org.hibernate.search.engine.search.sort.spi.SearchSortBuilderFactory;

class FieldSortContextImpl<B>
		extends NonEmptySortContextImpl
		implements FieldSortContext, FieldSortMissingValueContext<FieldSortContext>, SearchSortContributor<B> {

	private final FieldSortBuilder<B> builder;

	FieldSortContextImpl(SearchSortContainerContext containerContext,
			SearchSortBuilderFactory<?, B> factory, SearchSortDslContext<?> dslContext,
			String absoluteFieldPath, DslConverter dslConverter) {
		super( containerContext, dslContext );
		this.builder = factory.field( absoluteFieldPath, dslConverter );
	}

	@Override
	public FieldSortContext order(SortOrder order) {
		builder.order( order );
		return this;
	}

	@Override
	public FieldSortMissingValueContext<FieldSortContext> onMissingValue() {
		return this;
	}

	@Override
	public FieldSortContext sortFirst() {
		builder.missingFirst();
		return this;
	}

	@Override
	public FieldSortContext sortLast() {
		builder.missingLast();
		return this;
	}

	@Override
	public FieldSortContext use(Object value) {
		builder.missingAs( value );
		return this;
	}

	@Override
	public void contribute(Consumer<? super B> collector) {
		collector.accept( builder.toImplementation() );
	}
}
