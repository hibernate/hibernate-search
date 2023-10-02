/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.automaticindexing.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.mapper.pojo.common.annotation.impl.SearchProcessingWithContextException;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.mapper.pojo.reporting.spi.PojoEventContexts;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;
import org.hibernate.search.util.common.spi.ToStringTreeAppender;

/**
 * A {@link PojoImplicitReindexingResolverNode} dealing with a specific property of a specific type,
 * getting the value from that property then applying nested resolvers to that value.
 * <p>
 * This node will only delegate to nested nodes for deeper resolution,
 * and will never contribute entities to reindex directly.
 * At the time of writing, nested nodes are either type nodes or container element nodes,
 * but we might allow other nodes in the future for optimization purposes.
 *
 * @param <T> The property holder type received as input.
 * @param <P> The property type.
 */
public class PojoImplicitReindexingResolverPropertyNode<T, P> extends PojoImplicitReindexingResolverNode<T> {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ValueReadHandle<P> handle;
	private final PojoImplicitReindexingResolverNode<? super P> nested;

	private final PojoModelPath modelPath;

	public PojoImplicitReindexingResolverPropertyNode(ValueReadHandle<P> handle,
			PojoImplicitReindexingResolverNode<? super P> nested,
			PojoModelPath modelPath) {
		this.handle = handle;
		this.nested = nested;
		this.modelPath = modelPath;
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( PojoImplicitReindexingResolverNode::close, nested );
		}
	}

	@Override
	public void appendTo(ToStringTreeAppender appender) {
		appender.attribute( "operation", "process property" );
		appender.attribute( "handle", handle );
		appender.attribute( "nested", nested );
	}

	@Override
	public void resolveEntitiesToReindex(PojoReindexingCollector collector,
			T dirty, PojoImplicitReindexingResolverRootContext context) {
		P propertyValue;
		try {
			try {
				propertyValue = handle.get( dirty );
			}
			catch (RuntimeException e) {
				context.propagateOrIgnorePropertyAccessException( e );
				return;
			}
			if ( propertyValue != null ) {
				nested.resolveEntitiesToReindex( collector, propertyValue, context );
			}
		}
		catch (SearchProcessingWithContextException e) {
			// The context was already added to the exception, just re-throw:
			throw e;
		}
		catch (RuntimeException e) {
			throw log.searchProcessingFailure( e, e.getMessage(), PojoEventContexts.fromPath( modelPath ) );
		}
	}
}
