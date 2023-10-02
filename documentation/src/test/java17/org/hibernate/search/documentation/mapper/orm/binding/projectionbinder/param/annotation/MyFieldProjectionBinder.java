/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.projectionbinder.param.annotation;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.definition.ProjectionDefinition;
import org.hibernate.search.engine.search.projection.definition.ProjectionDefinitionContext;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBinder;
import org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBindingContext;

//tag::include[]
public class MyFieldProjectionBinder implements ProjectionBinder {

	private String fieldName = "name";

	public MyFieldProjectionBinder fieldName(String fieldName) { // <1>
		this.fieldName = fieldName;
		return this;
	}

	@Override
	public void bind(ProjectionBindingContext context) {
		context.definition(
				String.class,
				new MyProjectionDefinition( fieldName ) // <2>
		);
	}

	private static class MyProjectionDefinition
			implements ProjectionDefinition<String> {

		private final String fieldName;

		public MyProjectionDefinition(String fieldName) { // <2>
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
