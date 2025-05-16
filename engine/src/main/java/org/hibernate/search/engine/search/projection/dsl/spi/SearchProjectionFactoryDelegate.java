/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl.spi;

import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.search.common.NamedValues;
import org.hibernate.search.engine.search.common.NonStaticMetamodelScope;
import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionInnerStep;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionValueStep;
import org.hibernate.search.engine.search.projection.dsl.DistanceToFieldProjectionValueStep;
import org.hibernate.search.engine.search.projection.dsl.DocumentReferenceProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.EntityProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.EntityReferenceProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.FieldProjectionValueStep;
import org.hibernate.search.engine.search.projection.dsl.HighlightProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.IdProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.engine.search.projection.dsl.ScoreProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactoryExtension;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactoryExtensionIfSupportedStep;
import org.hibernate.search.engine.search.projection.dsl.TypedSearchProjectionFactory;
import org.hibernate.search.engine.spatial.GeoPoint;

@SuppressWarnings({ "deprecation", "removal" })
public record SearchProjectionFactoryDelegate<E, R>(TypedSearchProjectionFactory<NonStaticMetamodelScope, R, E> delegate)
		implements SearchProjectionFactory<R, E> {

	@Override
	public DocumentReferenceProjectionOptionsStep<?> documentReference() {
		return delegate.documentReference();
	}

	@Override
	public EntityReferenceProjectionOptionsStep<?, R> entityReference() {
		return delegate.entityReference();
	}

	@Override
	public <I> IdProjectionOptionsStep<?, I> id(Class<I> requestedIdentifierType) {
		return delegate.id( requestedIdentifierType );
	}

	@Override
	public EntityProjectionOptionsStep<?, E> entity() {
		return delegate.entity();
	}

	@Override
	public <T> EntityProjectionOptionsStep<?, T> entity(Class<T> requestedEntityType) {
		return delegate.entity( requestedEntityType );
	}

	@Override
	public <T> FieldProjectionValueStep<?, T> field(String fieldPath, Class<T> type, ValueModel valueModel) {
		return delegate.field( fieldPath, type, valueModel );
	}

	@Override
	public FieldProjectionValueStep<?, Object> field(String fieldPath, ValueModel valueModel) {
		return delegate.field( fieldPath, valueModel );
	}

	@Override
	public ScoreProjectionOptionsStep<?> score() {
		return delegate.score();
	}

	@Override
	public DistanceToFieldProjectionValueStep<?, Double> distance(String fieldPath, GeoPoint center) {
		return delegate.distance( fieldPath, center );
	}

	@Override
	public CompositeProjectionInnerStep object(String objectFieldPath) {
		return delegate.object( objectFieldPath );
	}

	@Override
	public CompositeProjectionInnerStep composite() {
		return delegate.composite();
	}

	@Override
	public CompositeProjectionValueStep<?, List<?>> composite(SearchProjection<?>... projections) {
		return delegate.composite( projections );
	}

	@Override
	public <T> ProjectionFinalStep<T> constant(T value) {
		return delegate.constant( value );
	}

	@Override
	public <T> ProjectionFinalStep<T> withParameters(
			Function<? super NamedValues, ? extends ProjectionFinalStep<T>> projectionCreator) {
		return delegate.withParameters( projectionCreator );
	}

	@Override
	public <T> T extension(SearchProjectionFactoryExtension<NonStaticMetamodelScope, T, R, E> extension) {
		return delegate.extension( extension );
	}

	@Override
	public <T> SearchProjectionFactoryExtensionIfSupportedStep<NonStaticMetamodelScope, T, R, E> extension() {
		return delegate.extension();
	}

	@Override
	public SearchProjectionFactory<R, E> withRoot(String objectFieldPath) {
		return new SearchProjectionFactoryDelegate<>( delegate.withRoot( objectFieldPath ) );
	}

	@Override
	public String toAbsolutePath(String relativeFieldPath) {
		return delegate.toAbsolutePath( relativeFieldPath );
	}

	@Override
	public HighlightProjectionOptionsStep highlight(String fieldPath) {
		return delegate.highlight( fieldPath );
	}
}
