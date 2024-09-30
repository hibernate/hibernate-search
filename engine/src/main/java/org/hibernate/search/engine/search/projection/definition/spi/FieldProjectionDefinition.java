/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.definition.spi;

import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.projection.ProjectionAccumulator;
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
		public SearchProjection<F> create(SearchProjectionFactory<?, ?> factory,
				ProjectionDefinitionContext context) {
			return factory.field( fieldPath, fieldType, valueModel ).toProjection();
		}
	}

	@Incubating
	public static final class MultiValued<C, F> extends FieldProjectionDefinition<C, F> {
		private final ProjectionAccumulator.Provider<F, C> accumulator;

		public MultiValued(String fieldPath, Class<F> fieldType, ProjectionAccumulator.Provider<F, C> accumulator,
				ValueModel valueModel) {
			super( fieldPath, fieldType, valueModel );
			this.accumulator = accumulator;
		}

		@Override
		protected boolean multi() {
			return accumulator.isSingleValued();
		}

		@Override
		public SearchProjection<C> create(SearchProjectionFactory<?, ?> factory, ProjectionDefinitionContext context) {
			return factory.field( fieldPath, fieldType, valueModel )
					.accumulator( accumulator ).toProjection();
		}
	}
}
