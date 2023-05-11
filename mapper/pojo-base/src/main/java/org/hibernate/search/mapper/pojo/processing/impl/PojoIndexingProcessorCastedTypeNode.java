/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.impl;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.model.spi.PojoCaster;
import org.hibernate.search.mapper.pojo.processing.spi.PojoIndexingProcessorRootContext;
import org.hibernate.search.util.common.spi.ToStringTreeAppender;

/**
 * A node inside a {@link PojoIndexingProcessor} responsible for casting the value to a given type,
 * then applying processor property nodes
 * as well as {@link TypeBridge}s to the value.
 * <p>
 * This node will fail with an exception when values cannot be cast to type {@code U}.
 *
 * @param <T> The processed type received as input.
 * @param <U> The type the input objects will be casted to.
 */
public class PojoIndexingProcessorCastedTypeNode<T, U> extends PojoIndexingProcessor<T> {

	private final PojoCaster<? super U> caster;
	private final Iterable<IndexObjectFieldReference> parentIndexObjectReferences;
	private final PojoIndexingProcessor<? super U> nested;
	private final boolean isEntityType;

	public PojoIndexingProcessorCastedTypeNode(PojoCaster<? super U> caster,
			Iterable<IndexObjectFieldReference> parentIndexObjectReferences,
			PojoIndexingProcessor<? super U> nested,
			boolean isEntityType) {
		this.caster = caster;
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
		appender.attribute( "operation", "process type (with cast)" );
		appender.attribute( "caster", caster );
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
		// The caster can only cast to the raw type, beyond that we have to use an unchecked cast.
		@SuppressWarnings("unchecked")
		U castedSource = (U) caster.cast( source );
		// "isEntityType" is just an optimization to avoid unnecessary calls to isDeleted(),
		// which may be costly (reflection, ...)
		if ( isEntityType && context.isDeleted( castedSource ) ) {
			return;
		}
		DocumentElement parentObject = target;
		for ( IndexObjectFieldReference objectFieldReference : parentIndexObjectReferences ) {
			parentObject = parentObject.addObject( objectFieldReference );
		}
		nested.process( parentObject, castedSource, context );
	}

}
