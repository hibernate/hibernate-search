/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
 * Binds a constructor parameter to a projection to the identifier of the mapped entity,
 * i.e. the value of the property marked as {@code @DocumentId}.
 *
 * @see SearchProjectionFactory#id(Class)
 * @see org.hibernate.search.mapper.pojo.mapping.definition.annotation.IdProjection
 */
public final class IdProjectionBinder implements ProjectionBinder {
	private static final IdProjectionBinder INSTANCE = new IdProjectionBinder();

	/**
	 * Creates an {@link IdProjectionBinder} to be passed
	 * to {@link org.hibernate.search.mapper.pojo.mapping.definition.programmatic.MethodParameterMappingStep#projection(ProjectionBinder)}.
	 *
	 * @return The binder.
	 */
	public static IdProjectionBinder create() {
		return INSTANCE;
	}

	private IdProjectionBinder() {
	}

	@Override
	public void bind(ProjectionBindingContext context) {
		bind( context, context.constructorParameter().rawType() );
	}

	private <T> void bind(ProjectionBindingContext context, Class<T> constructorParameterType) {
		context.definition( constructorParameterType, new Definition<>( constructorParameterType ) );
	}

	private static class Definition<I> extends AbstractProjectionDefinition<I> {
		private final Class<I> requestedIdentifierType;

		private Definition(Class<I> requestedIdentifierType) {
			this.requestedIdentifierType = requestedIdentifierType;
		}

		@Override
		protected String type() {
			return "id";
		}

		@Override
		public void appendTo(ToStringTreeAppender appender) {
			super.appendTo( appender );
			appender.attribute( "requestedIdentifierType", requestedIdentifierType );
		}

		@Override
		public SearchProjection<I> create(SearchProjectionFactory<?, ?> factory,
				ProjectionDefinitionContext context) {
			return factory.id( requestedIdentifierType ).toProjection();
		}
	}

}
