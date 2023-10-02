/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.search.definition.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.definition.ProjectionDefinition;
import org.hibernate.search.engine.search.projection.definition.ProjectionDefinitionContext;
import org.hibernate.search.engine.search.projection.definition.spi.CompositeProjectionDefinition;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionInnerStep;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionValueStep;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.path.spi.ProjectionConstructorPath;
import org.hibernate.search.mapper.pojo.model.spi.PojoConstructorIdentifier;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reflect.spi.ValueCreateHandle;
import org.hibernate.search.util.common.spi.ToStringTreeAppendable;
import org.hibernate.search.util.common.spi.ToStringTreeAppender;

public final class PojoConstructorProjectionDefinition<T>
		implements CompositeProjectionDefinition<T>, ToStringTreeAppendable {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoConstructorIdentifier constructor;
	private final ValueCreateHandle<? extends T> handle;
	private final List<BeanHolder<? extends ProjectionDefinition<?>>> parameters;

	public PojoConstructorProjectionDefinition(PojoConstructorIdentifier constructor,
			ValueCreateHandle<? extends T> valueCreateHandle,
			List<BeanHolder<? extends ProjectionDefinition<?>>> parameters) {
		this.constructor = constructor;
		this.handle = valueCreateHandle;
		this.parameters = parameters;
	}

	@Override
	public String toString() {
		return "PojoConstructorProjectionDefinition["
				+ "constructor=" + constructor
				+ ']';
	}

	@Override
	public void appendTo(ToStringTreeAppender appender) {
		appender.attribute( "constructor", constructor );
		appender.startList( "parameters" );
		for ( BeanHolder<? extends ProjectionDefinition<?>> innerDefinition : parameters ) {
			appender.value( innerDefinition.get() );
		}
		appender.endList();
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( BeanHolder::close, parameters );
		}
	}

	@Override
	public CompositeProjectionValueStep<?, T> apply(SearchProjectionFactory<?, ?> projectionFactory,
			CompositeProjectionInnerStep initialStep, ProjectionDefinitionContext context) {
		int i = -1;
		try {
			SearchProjection<?>[] innerProjections = new SearchProjection<?>[parameters.size()];
			for ( i = 0; i < parameters.size(); i++ ) {
				innerProjections[i] = parameters.get( i ).get().create( projectionFactory, context );
			}
			return initialStep.from( innerProjections ).asArray( handle );
		}
		catch (ConstructorProjectionApplicationException e) {
			// We already know what prevented from applying a projection constructor correctly,
			// just add a parent constructor and re-throw:
			ProjectionConstructorPath path = new ProjectionConstructorPath( constructor, e.projectionConstructorPath(), i );
			throw log.errorApplyingProjectionConstructor(
					e.getCause().getMessage(), e, path
			);
		}
		catch (SearchException e) {
			ProjectionConstructorPath path = new ProjectionConstructorPath( constructor );
			throw log.errorApplyingProjectionConstructor( e.getMessage(), e, path );
		}
	}

}
