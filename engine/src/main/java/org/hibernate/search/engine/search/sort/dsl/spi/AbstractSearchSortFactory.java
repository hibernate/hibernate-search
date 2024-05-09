/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.sort.dsl.spi;

import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.search.common.NamedValues;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.dsl.CompositeSortComponentsStep;
import org.hibernate.search.engine.search.sort.dsl.DistanceSortOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.ExtendedSearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.FieldSortOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.ScoreSortOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactoryExtension;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactoryExtensionIfSupportedStep;
import org.hibernate.search.engine.search.sort.dsl.SortFinalStep;
import org.hibernate.search.engine.search.sort.dsl.SortThenStep;
import org.hibernate.search.engine.search.sort.dsl.impl.CompositeSortComponentsStepImpl;
import org.hibernate.search.engine.search.sort.dsl.impl.DistanceSortOptionsStepImpl;
import org.hibernate.search.engine.search.sort.dsl.impl.FieldSortOptionsStepImpl;
import org.hibernate.search.engine.search.sort.dsl.impl.ScoreSortOptionsStepImpl;
import org.hibernate.search.engine.search.sort.dsl.impl.SearchSortFactoryExtensionStep;
import org.hibernate.search.engine.search.sort.dsl.impl.WithParametersSortFinalStep;
import org.hibernate.search.engine.search.sort.spi.SearchSortIndexScope;
import org.hibernate.search.engine.spatial.GeoPoint;

public abstract class AbstractSearchSortFactory<
		SR,
		S extends ExtendedSearchSortFactory<SR, S, PDF>,
		SC extends SearchSortIndexScope<?>,
		PDF extends SearchPredicateFactory<SR>>
		implements ExtendedSearchSortFactory<SR, S, PDF> {

	protected final SearchSortDslContext<SR, SC, PDF> dslContext;

	public AbstractSearchSortFactory(SearchSortDslContext<SR, SC, PDF> dslContext) {
		this.dslContext = dslContext;
	}

	@Override
	public ScoreSortOptionsStep<SR, ?> score() {
		return new ScoreSortOptionsStepImpl<>( dslContext );
	}

	@Override
	public SortThenStep<SR> indexOrder() {
		return staticThenStep( dslContext.scope().sortBuilders().indexOrder() );
	}

	@Override
	public FieldSortOptionsStep<SR, ?, PDF> field(String fieldPath) {
		return new FieldSortOptionsStepImpl<>( dslContext, fieldPath );
	}

	@Override
	public DistanceSortOptionsStep<SR, ?, PDF> distance(String fieldPath, GeoPoint location) {
		return new DistanceSortOptionsStepImpl<>(
				dslContext, fieldPath, location
		);
	}

	@Override
	public CompositeSortComponentsStep<SR, ?> composite() {
		return new CompositeSortComponentsStepImpl<>( dslContext );
	}

	@Override
	public SortThenStep<SR> composite(Consumer<? super CompositeSortComponentsStep<SR, ?>> elementContributor) {
		CompositeSortComponentsStep<SR, ?> next = composite();
		elementContributor.accept( next );
		return next;
	}

	@Override
	public SortThenStep<SR> withParameters(Function<? super NamedValues, ? extends SortFinalStep> sortCreator) {
		return new WithParametersSortFinalStep<SR>( dslContext, sortCreator );
	}

	@Override
	public <T> T extension(SearchSortFactoryExtension<SR, T> extension) {
		return DslExtensionState.returnIfSupported(
				extension, extension.extendOptional( this )
		);
	}

	@Override
	public SearchSortFactoryExtensionIfSupportedStep<SR> extension() {
		return new SearchSortFactoryExtensionStep<>( this, dslContext );
	}

	@Override
	public final String toAbsolutePath(String relativeFieldPath) {
		return dslContext.scope().toAbsolutePath( relativeFieldPath );
	}

	protected final SortThenStep<SR> staticThenStep(SearchSort sort) {
		return new StaticSortThenStep<>( dslContext, sort );
	}

}
