/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.sort.impl;

import org.hibernate.search.engine.search.dsl.sort.FieldSortContext;
import org.hibernate.search.engine.search.dsl.sort.FieldSortMissingValueContext;
import org.hibernate.search.engine.search.dsl.sort.SortOrder;
import org.hibernate.search.engine.search.dsl.sort.spi.AbstractNonEmptySortContext;
import org.hibernate.search.engine.search.dsl.sort.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.predicate.DslConverter;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;

class FieldSortContextImpl<B>
		extends AbstractNonEmptySortContext<B>
		implements FieldSortContext, FieldSortMissingValueContext<FieldSortContext> {

	private final FieldSortBuilder<B> builder;

	FieldSortContextImpl(SearchSortDslContext<?, B> dslContext,
			String absoluteFieldPath) {
		super( dslContext );
		this.builder = dslContext.getFactory().field( absoluteFieldPath );
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
	public FieldSortContext use(Object value, DslConverter dslConverter) {
		builder.missingAs( value, dslConverter );
		return this;
	}

	@Override
	protected B toImplementation() {
		return builder.toImplementation();
	}

}
