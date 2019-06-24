/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.sort.impl;

import org.hibernate.search.engine.search.dsl.sort.FieldSortOptionsStep;
import org.hibernate.search.engine.search.dsl.sort.FieldSortMissingValueBehaviorStep;
import org.hibernate.search.engine.search.dsl.sort.SortOrder;
import org.hibernate.search.engine.search.dsl.sort.spi.AbstractSortThenStep;
import org.hibernate.search.engine.search.dsl.sort.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.predicate.DslConverter;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;

class FieldSortOptionsStepImpl<B>
		extends AbstractSortThenStep<B>
		implements FieldSortOptionsStep, FieldSortMissingValueBehaviorStep<FieldSortOptionsStep> {

	private final FieldSortBuilder<B> builder;

	FieldSortOptionsStepImpl(SearchSortDslContext<?, B> dslContext,
			String absoluteFieldPath) {
		super( dslContext );
		this.builder = dslContext.getBuilderFactory().field( absoluteFieldPath );
	}

	@Override
	public FieldSortOptionsStep order(SortOrder order) {
		builder.order( order );
		return this;
	}

	@Override
	public FieldSortMissingValueBehaviorStep<FieldSortOptionsStep> onMissingValue() {
		return this;
	}

	@Override
	public FieldSortOptionsStep sortFirst() {
		builder.missingFirst();
		return this;
	}

	@Override
	public FieldSortOptionsStep sortLast() {
		builder.missingLast();
		return this;
	}

	@Override
	public FieldSortOptionsStep use(Object value, DslConverter dslConverter) {
		builder.missingAs( value, dslConverter );
		return this;
	}

	@Override
	protected B toImplementation() {
		return builder.toImplementation();
	}

}
