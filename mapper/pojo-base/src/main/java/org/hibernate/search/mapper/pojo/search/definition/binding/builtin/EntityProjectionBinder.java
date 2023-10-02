/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.search.definition.binding.builtin;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.definition.ProjectionDefinitionContext;
import org.hibernate.search.engine.search.projection.definition.spi.AbstractProjectionDefinition;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBinder;
import org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBindingContext;
import org.hibernate.search.util.common.spi.ToStringTreeAppender;

/**
 * Binds a constructor parameter to a projection to the entity that was originally indexed,
 * which for the Hibernate ORM mapper is a managed entity loaded from the database.
 *
 * @see SearchProjectionFactory#entity(Class)
 * @see org.hibernate.search.mapper.pojo.mapping.definition.annotation.EntityProjection
 */
public final class EntityProjectionBinder implements ProjectionBinder {
	private static final EntityProjectionBinder INSTANCE = new EntityProjectionBinder();

	/**
	 * Creates an {@link EntityProjectionBinder} to be passed
	 * to {@link org.hibernate.search.mapper.pojo.mapping.definition.programmatic.MethodParameterMappingStep#projection(ProjectionBinder)}.
	 *
	 * @return The binder.
	 */
	public static EntityProjectionBinder create() {
		return INSTANCE;
	}

	private EntityProjectionBinder() {
	}

	@Override
	public void bind(ProjectionBindingContext context) {
		bind( context, context.constructorParameter().rawType() );
	}

	private <T> void bind(ProjectionBindingContext context, Class<T> constructorParameterType) {
		context.definition( constructorParameterType, new Definition<>( constructorParameterType ) );
	}

	private static class Definition<I> extends AbstractProjectionDefinition<I> {
		private final Class<I> requestedEntityType;

		private Definition(Class<I> requestedEntityType) {
			this.requestedEntityType = requestedEntityType;
		}

		@Override
		protected String type() {
			return "entity";
		}

		@Override
		public void appendTo(ToStringTreeAppender appender) {
			super.appendTo( appender );
			appender.attribute( "requestedEntityType", requestedEntityType );
		}

		@Override
		public SearchProjection<I> create(SearchProjectionFactory<?, ?> factory,
				ProjectionDefinitionContext context) {
			return factory.entity( requestedEntityType ).toProjection();
		}
	}

}
