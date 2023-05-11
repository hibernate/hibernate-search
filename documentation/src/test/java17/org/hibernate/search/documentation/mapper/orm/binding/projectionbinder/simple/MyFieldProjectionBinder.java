/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.binding.projectionbinder.simple;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.definition.ProjectionDefinition;
import org.hibernate.search.engine.search.projection.definition.ProjectionDefinitionContext;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBinder;
import org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBindingContext;

//tag::binder[]
public class MyFieldProjectionBinder implements ProjectionBinder { // <1>
	@Override
	public void bind(ProjectionBindingContext context) { // <2>
		context.definition( // <3>
				String.class, // <4>
				new MyProjectionDefinition() // <5>
		);
	}

	// ... class continues below
	//end::binder[]
	//tag::definition[]
	// ... class MyFieldProjectionBinder (continued)

	private static class MyProjectionDefinition // <1>
			implements ProjectionDefinition<String> { // <2>
		@Override
		public SearchProjection<String> create(SearchProjectionFactory<?, ?> factory,
				ProjectionDefinitionContext context) {
			return factory.field( "title", String.class ) // <3>
					.toProjection(); // <4>
		}
	}
}
//end::definition[]
