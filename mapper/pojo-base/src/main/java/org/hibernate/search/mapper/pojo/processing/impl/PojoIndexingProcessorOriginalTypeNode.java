/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.processing.impl;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.mapper.pojo.processing.spi.PojoIndexingProcessorRootContext;
import org.hibernate.search.util.common.spi.ToStringTreeAppender;

/**
 * A node inside a {@link PojoIndexingProcessor} responsible for applying processor nodes
 * ({@link PojoIndexingProcessorTypeBridgeNode}, {@link PojoIndexingProcessorPropertyNode}, etc.).
 * without casting the value first (on contrary to {@link PojoIndexingProcessorCastedTypeNode}).
 *
 * @param <T> The processed type
 */
public class PojoIndexingProcessorOriginalTypeNode<T> extends PojoIndexingProcessor<T> {

	private final Iterable<IndexObjectFieldReference> parentIndexObjectReferences;
	private final PojoIndexingProcessor<? super T> nested;
	private final boolean isEntityType;

	public PojoIndexingProcessorOriginalTypeNode(Iterable<IndexObjectFieldReference> parentIndexObjectReferences,
			PojoIndexingProcessor<? super T> nested, boolean isEntityType) {
		this.parentIndexObjectReferences = parentIndexObjectReferences;
		this.nested = nested;
		this.isEntityType = isEntityType;
	}

	@Override
	public void close() {
		nested.close();
	}

	@Override
	public void appendTo(ToStringTreeAppender appender) {
		appender.attribute( "operation", "process type" );
		appender.attribute( "objectFieldsToCreate", parentIndexObjectReferences );
		appender.attribute( "nested", nested );
		appender.attribute( "isEntityType", isEntityType );
	}

	@Override
	@SuppressWarnings("unchecked") // As long as T is not a proxy-specific interface, it will also be implemented by the unproxified object
	public final void process(DocumentElement target, T source, PojoIndexingProcessorRootContext context) {
		if ( source == null ) {
			return;
		}
		source = (T) context.sessionContext().runtimeIntrospector().unproxy( source );
		// "isEntityType" is just an optimization to avoid unnecessary calls to isDeleted(),
		// which may be costly (reflection, ...)
		if ( isEntityType && context.isDeleted( source ) ) {
			return;
		}
		DocumentElement parentObject = target;
		for ( IndexObjectFieldReference objectFieldReference : parentIndexObjectReferences ) {
			parentObject = parentObject.addObject( objectFieldReference );
		}
		nested.process( parentObject, source, context );
	}

}
