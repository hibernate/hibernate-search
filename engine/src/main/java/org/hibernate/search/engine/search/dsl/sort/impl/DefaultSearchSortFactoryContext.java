/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.sort.impl;

import java.util.function.Consumer;

import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.search.dsl.sort.CompositeSortComponentsStep;
import org.hibernate.search.engine.search.dsl.sort.DistanceSortOptionsStep;
import org.hibernate.search.engine.search.dsl.sort.FieldSortOptionsStep;
import org.hibernate.search.engine.search.dsl.sort.SortThenStep;
import org.hibernate.search.engine.search.dsl.sort.ScoreSortOptionsStep;
import org.hibernate.search.engine.search.dsl.sort.SearchSortFactoryContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortFactoryContextExtension;
import org.hibernate.search.engine.search.dsl.sort.SearchSortFactoryContextExtensionStep;
import org.hibernate.search.engine.search.dsl.sort.spi.StaticSortThenStep;
import org.hibernate.search.engine.search.dsl.sort.spi.SearchSortDslContext;
import org.hibernate.search.engine.spatial.GeoPoint;


public class DefaultSearchSortFactoryContext<B> implements SearchSortFactoryContext {

	private final SearchSortDslContext<?, B> dslContext;

	public DefaultSearchSortFactoryContext(SearchSortDslContext<?, B> dslContext) {
		this.dslContext = dslContext;
	}

	@Override
	public ScoreSortOptionsStep byScore() {
		return new ScoreSortOptionsStepImpl<>( dslContext );
	}

	@Override
	public SortThenStep byIndexOrder() {
		return staticThenStep( dslContext.getFactory().indexOrder() );
	}

	@Override
	public FieldSortOptionsStep byField(String absoluteFieldPath) {
		return new FieldSortOptionsStepImpl<>( dslContext, absoluteFieldPath );
	}

	@Override
	public DistanceSortOptionsStep byDistance(String absoluteFieldPath, GeoPoint location) {
		return new DistanceSortOptionsStepImpl<>(
				dslContext, absoluteFieldPath, location
		);
	}

	@Override
	public CompositeSortComponentsStep byComposite() {
		return new CompositeSortComponentsStepImpl<>( dslContext );
	}

	@Override
	public SortThenStep byComposite(Consumer<? super CompositeSortComponentsStep> elementContributor) {
		CompositeSortComponentsStep next = byComposite();
		elementContributor.accept( next );
		return next;
	}

	@Override
	public <T> T extension(SearchSortFactoryContextExtension<T> extension) {
		return DslExtensionState.returnIfSupported(
				extension, extension.extendOptional( this, dslContext )
		);
	}

	@Override
	public SearchSortFactoryContextExtensionStep extension() {
		return new SearchSortFactoryContextExtensionStepImpl<>( this, dslContext );
	}

	private SortThenStep staticThenStep(B builder) {
		return new StaticSortThenStep<>( dslContext, builder );
	}

}
