/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.search.definition.binding.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.engine.common.tree.spi.TreeNestingContext;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.search.projection.definition.ProjectionDefinition;
import org.hibernate.search.engine.search.projection.definition.spi.ConstantProjectionDefinition;
import org.hibernate.search.mapper.pojo.logging.impl.ProjectionLog;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMappingHelper;
import org.hibernate.search.mapper.pojo.model.spi.PojoConstructorIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoConstructorModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoMethodParameterModel;
import org.hibernate.search.mapper.pojo.reporting.spi.PojoEventContexts;
import org.hibernate.search.mapper.pojo.search.definition.impl.PojoConstructorProjectionDefinition;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.common.reporting.spi.EventContextProvider;

public class ProjectionConstructorBinder<T> implements EventContextProvider {

	private final PojoMappingHelper mappingHelper;
	private final ProjectionBindingContextImpl<?> parentBindingContext;
	final TreeNestingContext nestingContext;
	final PojoConstructorModel<T> constructor;
	final PojoConstructorIdentifier constructorIdentifier;

	public ProjectionConstructorBinder(PojoMappingHelper mappingHelper, PojoConstructorModel<T> constructor) {
		this( mappingHelper, constructor, null, TreeNestingContext.root() );
	}

	public ProjectionConstructorBinder(PojoMappingHelper mappingHelper, PojoConstructorModel<T> constructor,
			ProjectionBindingContextImpl<?> parentBindingContext, TreeNestingContext nestingContext) {
		this.mappingHelper = mappingHelper;
		this.parentBindingContext = parentBindingContext;
		this.nestingContext = nestingContext;
		this.constructor = constructor;
		this.constructorIdentifier = new PojoConstructorIdentifier( constructor );
	}

	@Override
	public EventContext eventContext() {
		return EventContext.concat(
				parentBindingContext == null ? null : parentBindingContext.parameterBinder.eventContext(),
				PojoEventContexts.fromType( constructor.typeModel() ),
				PojoEventContexts.fromConstructor( constructor )
		);
	}

	public PojoConstructorProjectionDefinition<T> bind() {
		if ( constructor.typeModel().isAbstract() ) {
			throw ProjectionLog.INSTANCE.invalidAbstractTypeForProjectionConstructor(
					constructor.typeModel() );
		}
		List<BeanHolder<? extends ProjectionDefinition<?>>> parameterDefinitions = new ArrayList<>();
		for ( PojoMethodParameterModel<?> parameter : constructor.declaredParameters() ) {
			ProjectionConstructorParameterBinder<?> parameterBinder =
					new ProjectionConstructorParameterBinder<>( mappingHelper, this, parameter );
			BeanHolder<? extends ProjectionDefinition<?>> parameterDefinition;
			try {
				parameterDefinition = parameterBinder.bind();
			}
			catch (RuntimeException e) {
				mappingHelper.failureCollector()
						.withContext( parameterBinder.eventContext() )
						.add( e );
				// Here the result no longer matters, bootstrap will fail anyway.
				// We just pick a neutral value that won't trigger cascading failures.
				parameterDefinition = ConstantProjectionDefinition.nullValue();
			}
			parameterDefinitions.add( parameterDefinition );
		}
		return new PojoConstructorProjectionDefinition<>( constructorIdentifier, constructor.handle(), parameterDefinitions );
	}

}
