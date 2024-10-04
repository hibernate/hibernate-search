/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.definition.spi;

import java.util.List;

import org.hibernate.search.engine.search.projection.ProjectionAccumulator;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.definition.ProjectionDefinitionContext;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.common.spi.ToStringTreeAppender;

@Incubating
public abstract class ObjectProjectionDefinition<P, T>
		extends AbstractProjectionDefinition<P>
		implements AutoCloseable {

	protected final String fieldPath;
	protected final CompositeProjectionDefinition<T> delegate;

	private ObjectProjectionDefinition(String fieldPath, CompositeProjectionDefinition<T> delegate) {
		this.fieldPath = fieldPath;
		this.delegate = delegate;
	}

	@Override
	protected String type() {
		return "object";
	}

	@Override
	public void appendTo(ToStringTreeAppender appender) {
		super.appendTo( appender );
		appender.attribute( "fieldPath", fieldPath )
				.attribute( "multi", multi() )
				.attribute( "composite", delegate );
	}

	protected abstract boolean multi();

	@Override
	public void close() throws Exception {
		delegate.close();
	}

	@Deprecated(since = "8.0")
	@Incubating
	public static final class SingleValued<T> extends ObjectProjectionDefinition<T, T> {
		public SingleValued(String fieldPath, CompositeProjectionDefinition<T> delegate) {
			super( fieldPath, delegate );
		}

		@Override
		protected boolean multi() {
			return false;
		}

		@Override
		public SearchProjection<T> create(SearchProjectionFactory<?, ?> factory,
				ProjectionDefinitionContext context) {
			return delegate.apply( factory.withRoot( fieldPath ), factory.object( fieldPath ), context )
					.toProjection();
		}
	}

	@Deprecated(since = "8.0")
	@Incubating
	public static final class MultiValued<T> extends ObjectProjectionDefinition<List<T>, T> {

		public MultiValued(String fieldPath, CompositeProjectionDefinition<T> delegate) {
			super( fieldPath, delegate );
		}

		@Override
		protected boolean multi() {
			return true;
		}

		@Override
		public SearchProjection<List<T>> create(SearchProjectionFactory<?, ?> factory,
				ProjectionDefinitionContext context) {
			return delegate.apply( factory.withRoot( fieldPath ), factory.object( fieldPath ), context )
					.accumulator( ProjectionAccumulator.list() ).toProjection();
		}
	}

	@Incubating
	public static final class WrappedValued<C, T> extends ObjectProjectionDefinition<C, T> {
		private final ProjectionAccumulator.Provider<T, C> accumulator;

		public WrappedValued(String fieldPath, CompositeProjectionDefinition<T> delegate,
				ProjectionAccumulator.Provider<T, C> accumulator) {
			super( fieldPath, delegate );
			this.accumulator = accumulator;
		}

		@Override
		protected boolean multi() {
			return !accumulator.isSingleValued();
		}

		@Override
		public SearchProjection<C> create(SearchProjectionFactory<?, ?> factory,
				ProjectionDefinitionContext context) {
			return delegate.apply( factory.withRoot( fieldPath ), factory.object( fieldPath ), context )
					.accumulator( accumulator ).toProjection();
		}
	}
}
