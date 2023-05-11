/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.binding.projectionbinder.constructorparameter;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.definition.ProjectionDefinition;
import org.hibernate.search.engine.search.projection.definition.ProjectionDefinitionContext;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBinder;
import org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBindingContext;

//tag::include[]
public class MyFieldProjectionBinder implements ProjectionBinder {
	@Override
	public void bind(ProjectionBindingContext context) {
		var constructorParam = context.constructorParameter(); // <1>
		context.definition(
				String.class,
				new MyProjectionDefinition( constructorParam.name().orElseThrow() ) // <2>
		);
	}

	private static class MyProjectionDefinition
			implements ProjectionDefinition<String> {
		private final String fieldName;

		private MyProjectionDefinition(String fieldName) {
			this.fieldName = fieldName;
		}

		@Override
		public SearchProjection<String> create(SearchProjectionFactory<?, ?> factory,
				ProjectionDefinitionContext context) {
			return factory.field( fieldName, String.class ) // <3>
					.toProjection();
		}
	}
}
//end::include[]
