/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.search.definition.binding;

import java.util.List;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.search.projection.definition.ProjectionDefinition;
import org.hibernate.search.mapper.pojo.model.PojoModelValue;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The context returned by {@link ProjectionBindingContext#multi()}.
 * @see ProjectionBindingContext#multi()
 */
@Incubating
public interface ProjectionBindingMultiContext {

	/**
	 * Binds the constructor parameter to the given multi-valued projection definition.
	 *
	 * @param expectedValueType The expected type of elements of the constructor parameter,
	 * which must be compatible with the element type of lists returned by the projection definition.
	 * Hibernate Search will check that these expectations are met, and throw an exception if they are not.
	 * Note this is not the type of the constructor parameter, but of its elements;
	 * i.e. for a constructor parameter of type {@code List<String>},
	 * {@code expectedValueType} should be set to {@code String.class}.
	 * @param definition A definition of the projection to bind to the constructor parameter.
	 * @param <P> The type of values returned by the projection.
	 */
	<P> void definition(Class<P> expectedValueType, ProjectionDefinition<? extends List<? extends P>> definition);

	/**
	 * Binds the constructor parameter to the given multi-valued projection definition.
	 *
	 * @param expectedValueType The expected type of elements of the constructor parameter,
	 * which must be compatible with the element type of lists returned by the projection definition.
	 * Hibernate Search will check that these expectations are met, and throw an exception if they are not.
	 * Note this is not the type of the constructor parameter, but of its elements;
	 * i.e. for a constructor parameter of type {@code List<String>},
	 * {@code expectedValueType} should be set to {@code String.class}.
	 * @param definitionHolder A {@link BeanHolder} containing the definition of the projection
	 * to bind to the constructor parameter.
	 * @param <P> The type of values returned by the projection.
	 */
	<P> void definition(Class<P> expectedValueType,
			BeanHolder<? extends ProjectionDefinition<? extends List<? extends P>>> definitionHolder);

	/**
	 * @return An entry point allowing to inspect the constructor parameter being bound to a projection.
	 */
	@Incubating
	PojoModelValue<?> containerElement();

}
