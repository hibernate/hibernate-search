/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.projectionbinder.multi;

import java.util.List;
import java.util.Optional;

import org.hibernate.search.engine.search.projection.ProjectionCollector;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.definition.ProjectionDefinition;
import org.hibernate.search.engine.search.projection.definition.ProjectionDefinitionContext;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.mapper.pojo.model.PojoModelValue;
import org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBinder;
import org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBindingContext;

//tag::include[]
public class MyFieldProjectionBinder implements ProjectionBinder {
	@Override
	public void bind(ProjectionBindingContext context) {
		Optional<PojoModelValue<?>> containerElement = context.containerElement(); // <1>
		if ( containerElement.isPresent() ) {
			context.definition( String.class, new MyProjectionDefinition() ); // <2>
		}
		else {
			throw new RuntimeException( "This binder only supports container-wrapped constructor parameters" ); // <3>
		}
	}

	private static class MyProjectionDefinition
			implements ProjectionDefinition<List<String>> { // <4>
		@Override
		public SearchProjection<List<String>> create(SearchProjectionFactory<?, ?, ?> factory,
				ProjectionDefinitionContext context) {
			return factory.field( "tags", String.class )
					.collector( ProjectionCollector.list() ) // <4>
					.toProjection();
		}
	}
}
//end::include[]
