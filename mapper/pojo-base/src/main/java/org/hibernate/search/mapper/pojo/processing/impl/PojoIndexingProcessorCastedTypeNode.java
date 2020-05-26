/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.impl;

import java.util.Collection;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.model.spi.PojoCaster;
import org.hibernate.search.mapper.pojo.processing.spi.PojoIndexingProcessorSessionContext;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;

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

	private final PojoCaster<U> caster;
	private final Iterable<IndexObjectFieldReference> parentIndexObjectReferences;
	private final Collection<BeanHolder<? extends TypeBridge>> bridgeHolders;
	private final PojoIndexingProcessor<? super U> nested;

	public PojoIndexingProcessorCastedTypeNode(PojoCaster<U> caster,
			Iterable<IndexObjectFieldReference> parentIndexObjectReferences,
			Collection<BeanHolder<? extends TypeBridge>> bridgeHolders,
			PojoIndexingProcessor<? super U> nested) {
		this.caster = caster;
		this.parentIndexObjectReferences = parentIndexObjectReferences;
		this.bridgeHolders = bridgeHolders;
		this.nested = nested;
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( holder -> holder.get().close(), bridgeHolders );
			closer.pushAll( BeanHolder::close, bridgeHolders );
			closer.push( PojoIndexingProcessor::close, nested );
		}
	}

	@Override
	public void appendTo(ToStringTreeBuilder builder) {
		builder.attribute( "operation", "process type (with cast)" );
		builder.attribute( "caster", caster );
		builder.attribute( "objectFieldsToCreate", parentIndexObjectReferences );
		builder.startList( "bridges" );
		for ( BeanHolder<? extends TypeBridge> bridgeHolder : bridgeHolders ) {
			builder.value( bridgeHolder.get() );
		}
		builder.endList();
		builder.attribute( "nested", nested );
	}

	@Override
	public final void process(DocumentElement target, T source, PojoIndexingProcessorSessionContext sessionContext) {
		if ( source == null ) {
			return;
		}
		U castedSource = caster.cast( sessionContext.runtimeIntrospector().unproxy( source ) );
		DocumentElement parentObject = target;
		for ( IndexObjectFieldReference objectFieldReference : parentIndexObjectReferences ) {
			parentObject = parentObject.addObject( objectFieldReference );
		}
		for ( BeanHolder<? extends TypeBridge> bridgeHolder : bridgeHolders ) {
			bridgeHolder.get().write( parentObject, castedSource, sessionContext.typeBridgeWriteContext() );
		}
		nested.process( parentObject, castedSource, sessionContext );
	}

}
