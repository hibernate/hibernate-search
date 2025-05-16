/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.sort.dsl.spi;

import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.search.common.NamedValues;
import org.hibernate.search.engine.search.common.NonStaticMetamodelScope;
import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;
import org.hibernate.search.engine.search.reference.sort.FieldSortFieldReference;
import org.hibernate.search.engine.search.sort.dsl.CompositeSortComponentsStep;
import org.hibernate.search.engine.search.sort.dsl.DistanceSortOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.FieldSortOptionsGenericStep;
import org.hibernate.search.engine.search.sort.dsl.FieldSortOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.ScoreSortOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactoryExtension;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactoryExtensionIfSupportedStep;
import org.hibernate.search.engine.search.sort.dsl.SortFinalStep;
import org.hibernate.search.engine.search.sort.dsl.SortThenStep;
import org.hibernate.search.engine.search.sort.dsl.TypedSearchSortFactory;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public record SearchSortFactoryDelegate(TypedSearchSortFactory<NonStaticMetamodelScope> delegate) implements SearchSortFactory {
	@Override
	public ScoreSortOptionsStep<NonStaticMetamodelScope, ?> score() {
		return delegate.score();
	}

	@Override
	public SortThenStep<NonStaticMetamodelScope> indexOrder() {
		return delegate.indexOrder();
	}

	@Override
	public FieldSortOptionsStep<NonStaticMetamodelScope,
			?,
			? extends TypedSearchPredicateFactory<NonStaticMetamodelScope>> field(String fieldPath) {
		return delegate.field( fieldPath );
	}

	@Override
	public <T> FieldSortOptionsGenericStep<NonStaticMetamodelScope,
			T,
			?,
			?,
			? extends TypedSearchPredicateFactory<NonStaticMetamodelScope>> field(
					FieldSortFieldReference<? super NonStaticMetamodelScope, T> fieldReference) {
		return delegate.field( fieldReference );
	}

	@Override
	public DistanceSortOptionsStep<NonStaticMetamodelScope,
			?,
			? extends TypedSearchPredicateFactory<NonStaticMetamodelScope>> distance(String fieldPath, GeoPoint location) {
		return delegate.distance( fieldPath, location );
	}

	@Override
	public CompositeSortComponentsStep<NonStaticMetamodelScope, ?> composite() {
		return delegate.composite();
	}

	@Override
	public SortThenStep<NonStaticMetamodelScope> composite(
			Consumer<? super CompositeSortComponentsStep<NonStaticMetamodelScope, ?>> elementContributor) {
		return delegate.composite( elementContributor );
	}

	@Override
	public SortThenStep<NonStaticMetamodelScope> withParameters(
			Function<? super NamedValues, ? extends SortFinalStep> sortCreator) {
		return delegate.withParameters( sortCreator );
	}

	@Override
	public <T> T extension(SearchSortFactoryExtension<NonStaticMetamodelScope, T> extension) {
		return delegate.extension( extension );
	}

	@Override
	public SearchSortFactoryExtensionIfSupportedStep<NonStaticMetamodelScope> extension() {
		return delegate.extension();
	}

	@Override
	public SearchSortFactory withRoot(String objectFieldPath) {
		return new SearchSortFactoryDelegate( delegate.withRoot( objectFieldPath ) );
	}

	@Override
	public String toAbsolutePath(String relativeFieldPath) {
		return delegate.toAbsolutePath( relativeFieldPath );
	}
}
