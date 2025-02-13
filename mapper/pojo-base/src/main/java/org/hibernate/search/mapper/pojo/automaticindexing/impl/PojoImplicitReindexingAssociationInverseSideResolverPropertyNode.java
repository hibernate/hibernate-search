/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.automaticindexing.impl;

import org.hibernate.search.mapper.pojo.common.annotation.impl.SearchProcessingWithContextException;
import org.hibernate.search.mapper.pojo.logging.impl.IndexingLog;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.mapper.pojo.reporting.spi.PojoEventContexts;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;
import org.hibernate.search.util.common.spi.ToStringTreeAppender;

/**
 * A {@link PojoImplicitReindexingAssociationInverseSideResolverNode} dealing with a specific property of a specific type,
 * getting the value from that property then applying nested resolvers to that value.
 *
 * @param <T> The property holder type received as input.
 * @param <P> The property type.
 */
class PojoImplicitReindexingAssociationInverseSideResolverPropertyNode<T, P>
		extends PojoImplicitReindexingAssociationInverseSideResolverNode<T> {

	private final ValueReadHandle<P> handle;
	private final PojoImplicitReindexingAssociationInverseSideResolverNode<? super P> nested;

	private final PojoModelPath modelPath;

	public PojoImplicitReindexingAssociationInverseSideResolverPropertyNode(ValueReadHandle<P> handle,
			PojoImplicitReindexingAssociationInverseSideResolverNode<? super P> nested,
			PojoModelPath modelPath) {
		this.handle = handle;
		this.nested = nested;
		this.modelPath = modelPath;
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( PojoImplicitReindexingAssociationInverseSideResolverNode::close, nested );
		}
	}

	@Override
	public void appendTo(ToStringTreeAppender appender) {
		appender.attribute( "operation", "process property" );
		appender.attribute( "handle", handle );
		appender.attribute( "nested", nested );
	}

	@Override
	void resolveEntitiesToReindex(PojoReindexingAssociationInverseSideCollector collector, T state,
			PojoImplicitReindexingAssociationInverseSideResolverRootContext context) {
		P propertyValue;
		try {
			try {
				propertyValue = handle.get( state );
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
			throw IndexingLog.INSTANCE.searchProcessingFailure( e, e.getMessage(), PojoEventContexts.fromPath( modelPath ) );
		}
	}

}
