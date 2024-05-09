/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.definition.spi;

import java.util.List;

import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.projection.ProjectionCollector;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.definition.ProjectionDefinitionContext;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.common.spi.ToStringTreeAppender;

@Incubating
public abstract class FieldProjectionDefinition<P, F> extends AbstractProjectionDefinition<P> {

	protected final String fieldPath;
	protected final Class<F> fieldType;
	protected final ValueModel valueModel;

	private FieldProjectionDefinition(String fieldPath, Class<F> fieldType, ValueModel valueModel) {
		this.fieldPath = fieldPath;
		this.fieldType = fieldType;
		this.valueModel = valueModel;
	}

	@Override
	protected String type() {
		return "field";
	}

	@Override
	public void appendTo(ToStringTreeAppender appender) {
		super.appendTo( appender );
		appender.attribute( "fieldPath", fieldPath )
				.attribute( "fieldType", fieldType )
				.attribute( "multi", multi() )
				.attribute( "valueModel", valueModel );
	}

	protected abstract boolean multi();

	@Deprecated(since = "8.0")
	@Incubating
	public static final class SingleValued<F> extends FieldProjectionDefinition<F, F> {
		public SingleValued(String fieldPath, Class<F> fieldType, ValueModel valueModel) {
			super( fieldPath, fieldType, valueModel );
		}

		@Override
		protected boolean multi() {
			return false;
		}

		@Override
		public SearchProjection<F> create(SearchProjectionFactory<?, ?, ?> factory,
				ProjectionDefinitionContext context) {
			return factory.field( fieldPath, fieldType, valueModel ).toProjection();
		}
	}

	@Deprecated(since = "8.0")
	@Incubating
	public static final class MultiValued<F> extends FieldProjectionDefinition<List<F>, F> {

		public MultiValued(String fieldPath, Class<F> fieldType, ValueModel valueModel) {
			super( fieldPath, fieldType, valueModel );
		}

		@Override
		protected boolean multi() {
			return true;
		}

		@Override
		public SearchProjection<List<F>> create(SearchProjectionFactory<?, ?, ?> factory, ProjectionDefinitionContext context) {
			return factory.field( fieldPath, fieldType, valueModel )
					.collector( ProjectionCollector.list() ).toProjection();
		}
	}

	@Incubating
	public static final class AccumulatedValued<C, F> extends FieldProjectionDefinition<C, F> {
		private final ProjectionCollector.Provider<F, C> collector;

		public AccumulatedValued(String fieldPath, Class<F> fieldType, ProjectionCollector.Provider<F, C> collector,
				ValueModel valueModel) {
			super( fieldPath, fieldType, valueModel );
			this.collector = collector;
		}

		@Override
		protected boolean multi() {
			return !collector.isSingleValued();
		}

		@Override
		public SearchProjection<C> create(SearchProjectionFactory<?, ?, ?> factory,
				ProjectionDefinitionContext context) {
			return factory.field( fieldPath, fieldType, valueModel )
					.collector( collector ).toProjection();
		}
	}
}
