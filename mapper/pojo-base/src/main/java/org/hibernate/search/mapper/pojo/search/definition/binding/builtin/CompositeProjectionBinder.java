/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.search.definition.binding.builtin;

import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;
import org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBinder;
import org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBindingContext;

/**
 * Binds a constructor parameter to a composite projection,
 * which will combine multiple inner projections.
 * <p>
 * The content of the composite projection is defined in the constructor parameter type
 * by another {@link ProjectionConstructor}.
 * <p>
 * On contrary to the {@link ObjectProjectionBinder object projection},
 * a composite projection is not bound to a specific object field,
 * and thus it will always yield one and only one value.
 *
 * @see SearchProjectionFactory#composite()
 * @see org.hibernate.search.engine.search.projection.dsl.CompositeProjectionInnerStep#as(Class)
 * @see org.hibernate.search.mapper.pojo.mapping.definition.annotation.CompositeProjection
 */
public final class CompositeProjectionBinder implements ProjectionBinder {
	private static final CompositeProjectionBinder INSTANCE = new CompositeProjectionBinder();

	/**
	 * Creates an {@link CompositeProjectionBinder} to be passed
	 * to {@link org.hibernate.search.mapper.pojo.mapping.definition.programmatic.MethodParameterMappingStep#projection(ProjectionBinder)}.
	 *
	 * @return The binder.
	 */
	public static CompositeProjectionBinder create() {
		return INSTANCE;
	}

	private CompositeProjectionBinder() {
	}

	@Override
	public void bind(ProjectionBindingContext context) {
		bind( context, context.constructorParameter().rawType() );
	}

	private <T> void bind(ProjectionBindingContext context, Class<T> constructorParameterType) {
		context.definition(
				constructorParameterType,
				context.createCompositeDefinition( constructorParameterType )
		);
	}
}
