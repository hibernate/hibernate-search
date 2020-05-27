/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.impl;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.mapper.pojo.processing.spi.PojoIndexingProcessorSessionContext;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;

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

	public PojoIndexingProcessorOriginalTypeNode(Iterable<IndexObjectFieldReference> parentIndexObjectReferences,
			PojoIndexingProcessor<? super T> nested) {
		this.parentIndexObjectReferences = parentIndexObjectReferences;
		this.nested = nested;
	}

	@Override
	public void close() {
		nested.close();
	}

	@Override
	public void appendTo(ToStringTreeBuilder builder) {
		builder.attribute( "operation", "process type" );
		builder.attribute( "objectFieldsToCreate", parentIndexObjectReferences );
		builder.attribute( "nested", nested );
	}

	@Override
	@SuppressWarnings("unchecked") // As long as T is not a proxy-specific interface, it will also be implemented by the unproxified object
	public final void process(DocumentElement target, T source, PojoIndexingProcessorSessionContext sessionContext) {
		if ( source == null ) {
			return;
		}
		source = (T) sessionContext.runtimeIntrospector().unproxy( source );
		DocumentElement parentObject = target;
		for ( IndexObjectFieldReference objectFieldReference : parentIndexObjectReferences ) {
			parentObject = parentObject.addObject( objectFieldReference );
		}
		nested.process( parentObject, source, sessionContext );
	}

}
