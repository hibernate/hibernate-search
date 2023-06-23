/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.sort.dsl.spi;

import java.util.function.Consumer;

import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.dsl.CompositeSortComponentsStep;
import org.hibernate.search.engine.search.sort.dsl.DistanceSortOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.ExtendedSearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.FieldSortOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.ScoreSortOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactoryExtension;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactoryExtensionIfSupportedStep;
import org.hibernate.search.engine.search.sort.dsl.SortThenStep;
import org.hibernate.search.engine.search.sort.dsl.impl.CompositeSortComponentsStepImpl;
import org.hibernate.search.engine.search.sort.dsl.impl.DistanceSortOptionsStepImpl;
import org.hibernate.search.engine.search.sort.dsl.impl.FieldSortOptionsStepImpl;
import org.hibernate.search.engine.search.sort.dsl.impl.ScoreSortOptionsStepImpl;
import org.hibernate.search.engine.search.sort.dsl.impl.SearchSortFactoryExtensionStep;
import org.hibernate.search.engine.search.sort.spi.SearchSortIndexScope;
import org.hibernate.search.engine.spatial.GeoPoint;

public abstract class AbstractSearchSortFactory<
		S extends ExtendedSearchSortFactory<S, PDF>,
		SC extends SearchSortIndexScope<?>,
		PDF extends SearchPredicateFactory>
		implements ExtendedSearchSortFactory<S, PDF> {

	protected final SearchSortDslContext<SC, PDF> dslContext;

	public AbstractSearchSortFactory(SearchSortDslContext<SC, PDF> dslContext) {
		this.dslContext = dslContext;
	}

	@Override
	public ScoreSortOptionsStep<?> score() {
		return new ScoreSortOptionsStepImpl( dslContext );
	}

	@Override
	public SortThenStep indexOrder() {
		return staticThenStep( dslContext.scope().sortBuilders().indexOrder() );
	}

	@Override
	public FieldSortOptionsStep<?, PDF> field(String fieldPath) {
		return new FieldSortOptionsStepImpl<>( dslContext, fieldPath );
	}

	@Override
	public DistanceSortOptionsStep<?, PDF> distance(String fieldPath, GeoPoint location) {
		return new DistanceSortOptionsStepImpl<>(
				dslContext, fieldPath, location
		);
	}

	@Override
	public CompositeSortComponentsStep<?> composite() {
		return new CompositeSortComponentsStepImpl( dslContext );
	}

	@Override
	public SortThenStep composite(Consumer<? super CompositeSortComponentsStep<?>> elementContributor) {
		CompositeSortComponentsStep<?> next = composite();
		elementContributor.accept( next );
		return next;
	}

	@Override
	public <T> T extension(SearchSortFactoryExtension<T> extension) {
		return DslExtensionState.returnIfSupported(
				extension, extension.extendOptional( this )
		);
	}

	@Override
	public SearchSortFactoryExtensionIfSupportedStep extension() {
		return new SearchSortFactoryExtensionStep( this, dslContext );
	}

	@Override
	public final String toAbsolutePath(String relativeFieldPath) {
		return dslContext.scope().toAbsolutePath( relativeFieldPath );
	}

	protected final SortThenStep staticThenStep(SearchSort sort) {
		return new StaticSortThenStep( dslContext, sort );
	}

}
