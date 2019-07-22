/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.aggregation.impl;

import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.search.dsl.aggregation.RangeAggregationFieldStep;
import org.hibernate.search.engine.search.dsl.aggregation.SearchAggregationFactory;
import org.hibernate.search.engine.search.dsl.aggregation.SearchAggregationFactoryExtension;
import org.hibernate.search.engine.search.dsl.aggregation.TermsAggregationFieldStep;
import org.hibernate.search.engine.search.dsl.aggregation.spi.SearchAggregationDslContext;

public class DefaultSearchAggregationFactory implements SearchAggregationFactory {

	private final SearchAggregationDslContext<?> dslContext;

	public DefaultSearchAggregationFactory(SearchAggregationDslContext<?> dslContext) {
		this.dslContext = dslContext;
	}

	@Override
	public RangeAggregationFieldStep range() {
		return new RangeAggregationFieldStepImpl( dslContext );
	}

	@Override
	public TermsAggregationFieldStep terms() {
		return new TermsAggregationFieldStepImpl( dslContext );
	}

	@Override
	public <T> T extension(SearchAggregationFactoryExtension<T> extension) {
		return DslExtensionState.returnIfSupported(
				extension, extension.extendOptional( this, dslContext )
		);
	}
}
