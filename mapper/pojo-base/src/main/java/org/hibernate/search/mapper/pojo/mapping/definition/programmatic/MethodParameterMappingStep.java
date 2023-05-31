/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

import java.util.Collections;
import java.util.Map;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.search.projection.definition.ProjectionDefinition;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBinder;

/**
 * The step in a mapping definition where a method parameter can be mapped.
 */
public interface MethodParameterMappingStep {

	/**
	 * Maps a constructor parameter to a projection definition picked by the given binder.
	 *
	 * @param binder A {@link ProjectionBinder} responsible for creating a projection definition.
	 * @return {@code this}, for method chaining.
	 * @see ProjectionBinder
	 * @see ProjectionDefinition
	 * @see SearchProjectionFactory
	 * @see org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionBinding
	 * @see org.hibernate.search.mapper.pojo.search.definition.binding.builtin.IdProjectionBinder
	 * @see org.hibernate.search.mapper.pojo.search.definition.binding.builtin.FieldProjectionBinder
	 * @see org.hibernate.search.mapper.pojo.search.definition.binding.builtin.ScoreProjectionBinder
	 * @see org.hibernate.search.mapper.pojo.search.definition.binding.builtin.DocumentReferenceProjectionBinder
	 * @see org.hibernate.search.mapper.pojo.search.definition.binding.builtin.EntityProjectionBinder
	 * @see org.hibernate.search.mapper.pojo.search.definition.binding.builtin.EntityReferenceProjectionBinder
	 * @see org.hibernate.search.mapper.pojo.search.definition.binding.builtin.ObjectProjectionBinder
	 * @see org.hibernate.search.mapper.pojo.search.definition.binding.builtin.CompositeProjectionBinder
	 */
	default MethodParameterMappingStep projection(ProjectionBinder binder) {
		return projection( BeanReference.ofInstance( binder ), Collections.emptyMap() );
	}

	/**
	 * Maps a constructor parameter to a projection definition picked by the given binder
	 * with the given parameters.
	 *
	 * @param binder A {@link ProjectionBinder} responsible for creating a projection definition.
	 * * @param params The parameters to make available to the binder through
	 * {@link org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBindingContext#param(String)}
	 * or {@link org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBindingContext#paramOptional(String)}.
	 * @return {@code this}, for method chaining.
	 * @see ProjectionDefinition
	 * @see SearchProjectionFactory
	 * @see org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionBinding
	 */
	default MethodParameterMappingStep projection(ProjectionBinder binder, Map<String, Object> params) {
		return projection( BeanReference.ofInstance( binder ), params );
	}

	/**
	 * Maps a constructor parameter to a projection definition picked by the given binder.
	 *
	 * @param binder A {@link BeanReference} to a {@link ProjectionBinder} responsible for creating a projection definition.
	 * @return {@code this}, for method chaining.
	 * @see ProjectionBinder
	 * @see ProjectionDefinition
	 * @see SearchProjectionFactory
	 * @see org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionBinding
	 */
	default MethodParameterMappingStep projection(BeanReference<? extends ProjectionBinder> binder) {
		return projection( binder, Collections.emptyMap() );
	}

	/**
	 * Maps a constructor parameter to a projection definition picked by the given binder
	 * with the given parameters.
	 *
	 * @param binder A {@link BeanReference} to a {@link ProjectionBinder} responsible for creating a projection definition.
	 * @param params The parameters to make available to the binder through
	 * {@link org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBindingContext#param(String)}
	 * or {@link org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBindingContext#paramOptional(String)}.
	 * @return {@code this}, for method chaining.
	 * @see ProjectionDefinition
	 * @see SearchProjectionFactory
	 * @see org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionBinding
	 */
	MethodParameterMappingStep projection(BeanReference<? extends ProjectionBinder> binder, Map<String, Object> params);

}
