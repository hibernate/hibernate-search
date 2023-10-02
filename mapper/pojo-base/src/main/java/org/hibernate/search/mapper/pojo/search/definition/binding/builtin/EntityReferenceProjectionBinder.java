/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.search.definition.binding.builtin;

import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.definition.ProjectionDefinitionContext;
import org.hibernate.search.engine.search.projection.definition.spi.AbstractProjectionDefinition;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBinder;
import org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBindingContext;

/**
 * Binds a constructor parameter to a projection to a reference to the entity that was originally indexed.
 * <p>
 * Entity references are instances of type {@link EntityReference}.
 *
 * @see SearchProjectionFactory#entityReference()
 * @see org.hibernate.search.mapper.pojo.mapping.definition.annotation.EntityReferenceProjection
 */
public final class EntityReferenceProjectionBinder implements ProjectionBinder {
	private static final EntityReferenceProjectionBinder INSTANCE = new EntityReferenceProjectionBinder();

	/**
	 * Creates an {@link EntityReferenceProjectionBinder} to be passed
	 * to {@link org.hibernate.search.mapper.pojo.mapping.definition.programmatic.MethodParameterMappingStep#projection(ProjectionBinder)}.
	 *
	 * @return The binder.
	 */
	public static EntityReferenceProjectionBinder create() {
		return INSTANCE;
	}

	private EntityReferenceProjectionBinder() {
	}

	@Override
	public void bind(ProjectionBindingContext context) {
		context.definition( EntityReference.class, Definition.INSTANCE );
	}

	private static final class Definition extends AbstractProjectionDefinition<EntityReference> {
		public static final Definition INSTANCE = new Definition();

		@Override
		protected String type() {
			return "entity-reference";
		}

		@Override
		// Mappers are required to have their entity reference type extend EntityReference.
		// The generic parameter R in SearchProjectionFactory is only there for backwards compatibility.
		@SuppressWarnings("unchecked")
		public SearchProjection<? extends EntityReference> create(SearchProjectionFactory<?, ?> factory,
				ProjectionDefinitionContext context) {
			return (SearchProjection<? extends EntityReference>) factory.entityReference().toProjection();
		}
	}

}
