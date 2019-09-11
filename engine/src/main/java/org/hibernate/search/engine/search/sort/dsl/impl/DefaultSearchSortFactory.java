/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.sort.dsl.impl;

import java.util.function.Consumer;

import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.search.sort.dsl.CompositeSortComponentsStep;
import org.hibernate.search.engine.search.sort.dsl.DistanceSortOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.FieldSortOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.SortThenStep;
import org.hibernate.search.engine.search.sort.dsl.ScoreSortOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactoryExtension;
import org.hibernate.search.engine.search.sort.dsl.spi.StaticSortThenStep;
import org.hibernate.search.engine.search.sort.dsl.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactoryExtensionIfSupportedStep;
import org.hibernate.search.engine.spatial.GeoPoint;


public class DefaultSearchSortFactory<B> implements SearchSortFactory {

	private final SearchSortDslContext<?, B> dslContext;

	public DefaultSearchSortFactory(SearchSortDslContext<?, B> dslContext) {
		this.dslContext = dslContext;
	}

	@Override
	public ScoreSortOptionsStep score() {
		return new ScoreSortOptionsStepImpl<>( dslContext );
	}

	@Override
	public SortThenStep indexOrder() {
		return staticThenStep( dslContext.getBuilderFactory().indexOrder() );
	}

	@Override
	public FieldSortOptionsStep field(String absoluteFieldPath) {
		return new FieldSortOptionsStepImpl<>( dslContext, absoluteFieldPath );
	}

	@Override
	public DistanceSortOptionsStep distance(String absoluteFieldPath, GeoPoint location) {
		return new DistanceSortOptionsStepImpl<>(
				dslContext, absoluteFieldPath, location
		);
	}

	@Override
	public CompositeSortComponentsStep composite() {
		return new CompositeSortComponentsStepImpl<>( dslContext );
	}

	@Override
	public SortThenStep composite(Consumer<? super CompositeSortComponentsStep> elementContributor) {
		CompositeSortComponentsStep next = composite();
		elementContributor.accept( next );
		return next;
	}

	@Override
	public <T> T extension(SearchSortFactoryExtension<T> extension) {
		return DslExtensionState.returnIfSupported(
				extension, extension.extendOptional( this, dslContext )
		);
	}

	@Override
	public SearchSortFactoryExtensionIfSupportedStep extension() {
		return new SearchSortFactoryExtensionStep<>( this, dslContext );
	}

	private SortThenStep staticThenStep(B builder) {
		return new StaticSortThenStep<>( dslContext, builder );
	}

}
