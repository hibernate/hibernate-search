/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.identity.impl;

import java.lang.invoke.MethodHandles;
import java.util.function.Supplier;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeMappingContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeSessionContext;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.spi.PojoCaster;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;

public final class PropertyIdentifierMapping<I, E> implements IdentifierMappingImplementor<I, E> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoCaster<? super I> caster;
	private final ValueReadHandle<I> property;
	private final BeanHolder<? extends IdentifierBridge<I>> bridgeHolder;

	public PropertyIdentifierMapping(PojoCaster<? super I> caster, ValueReadHandle<I> property,
			BeanHolder<? extends IdentifierBridge<I>> bridgeHolder) {
		this.caster = caster;
		this.property = property;
		this.bridgeHolder = bridgeHolder;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "handle=" + property
				+ ", bridgeHolder=" + bridgeHolder
				+ "]";
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( IdentifierBridge::close, bridgeHolder, BeanHolder::get );
			closer.push( BeanHolder::close, bridgeHolder );
		}
	}

	@Override
	@SuppressWarnings("unchecked") // We can only cast to the raw type, if I is generic we need an unchecked cast
	public I getIdentifier(Object providedId, Supplier<? extends E> entitySupplierOrNull) {
		if ( providedId != null ) {
			return (I) caster.cast( providedId );
		}
		if ( entitySupplierOrNull == null ) {
			throw log.nullProvidedIdentifierAndEntity();
		}
		return property.get( entitySupplierOrNull.get() );
	}

	@Override
	public I getIdentifierOrNull(E entity) {
		return property.get( entity );
	}

	@Override
	public String toDocumentIdentifier(I identifier, BridgeMappingContext context) {
		return bridgeHolder.get().toDocumentIdentifier( identifier, context.identifierBridgeToDocumentIdentifierContext() );
	}

	@Override
	public I fromDocumentIdentifier(String documentId, BridgeSessionContext context) {
		return bridgeHolder.get().fromDocumentIdentifier( documentId, context.identifierBridgeFromDocumentIdentifierContext() );
	}

}
