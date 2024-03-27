/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.processing.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.mapper.pojo.common.annotation.impl.SearchProcessingWithContextException;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.mapper.pojo.processing.spi.PojoIndexingProcessorRootContext;
import org.hibernate.search.mapper.pojo.reporting.spi.PojoEventContexts;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;
import org.hibernate.search.util.common.spi.ToStringTreeAppender;

/**
 * A node inside a {@link PojoIndexingProcessor} responsible for extracting the value of a property,
 * and applying nested processor nodes
 * ({@link PojoIndexingProcessorPropertyBridgeNode}, {@link PojoIndexingProcessorValueBridgeNode}, etc.).
 *
 * @param <T> The property holder type
 * @param <P> The property type
 */
public class PojoIndexingProcessorPropertyNode<T, P> extends PojoIndexingProcessor<T> {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ValueReadHandle<P> handle;
	private final PojoIndexingProcessor<? super P> nested;

	private final PojoModelPath modelPath;

	public PojoIndexingProcessorPropertyNode(ValueReadHandle<P> handle, PojoIndexingProcessor<? super P> nested,
			PojoModelPath modelPath) {
		this.handle = handle;
		this.nested = nested;
		this.modelPath = modelPath;
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( PojoIndexingProcessor::close, nested );
		}
	}

	@Override
	public void appendTo(ToStringTreeAppender appender) {
		appender.attribute( "operation", "process property" );
		appender.attribute( "handle", handle );
		appender.attribute( "nested", nested );
	}

	@Override
	public final void process(DocumentElement target, T source, PojoIndexingProcessorRootContext context) {
		try {
			P propertyValue = handle.get( source );
			nested.process( target, propertyValue, context );
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
