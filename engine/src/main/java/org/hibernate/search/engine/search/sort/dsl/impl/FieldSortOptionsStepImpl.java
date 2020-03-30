/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.sort.dsl.impl;

import org.hibernate.search.engine.search.sort.dsl.FieldSortOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.FieldSortMissingValueBehaviorStep;
import org.hibernate.search.engine.search.sort.dsl.SortOrder;
import org.hibernate.search.engine.search.sort.dsl.spi.AbstractSortThenStep;
import org.hibernate.search.engine.search.sort.dsl.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.common.SortMode;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;

class FieldSortOptionsStepImpl<B>
		extends AbstractSortThenStep<B>
		implements FieldSortOptionsStep<FieldSortOptionsStepImpl<B>>,
	FieldSortMissingValueBehaviorStep<FieldSortOptionsStepImpl<B>> {

	private final FieldSortBuilder<B> builder;

	FieldSortOptionsStepImpl(SearchSortDslContext<?, B> dslContext,
			String absoluteFieldPath) {
		super( dslContext );
		this.builder = dslContext.getBuilderFactory().field( absoluteFieldPath );
	}

	@Override
	public FieldSortOptionsStepImpl<B> order(SortOrder order) {
		builder.order( order );
		return this;
	}

	@Override
	public FieldSortOptionsStepImpl<B> mode(SortMode mode) {
		builder.mode( mode );
		return this;
	}

	@Override
	public FieldSortMissingValueBehaviorStep<FieldSortOptionsStepImpl<B>> missing() {
		return this;
	}

	@Override
	public FieldSortOptionsStepImpl<B> first() {
		builder.missingFirst();
		return this;
	}

	@Override
	public FieldSortOptionsStepImpl<B> last() {
		builder.missingLast();
		return this;
	}

	@Override
	public FieldSortOptionsStepImpl<B> use(Object value, ValueConvert convert) {
		builder.missingAs( value, convert );
		return this;
	}

	@Override
	protected B toImplementation() {
		return builder.toImplementation();
	}

}
