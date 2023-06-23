/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.identity.impl;

import java.lang.invoke.MethodHandles;
import java.util.function.Supplier;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeMappingContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeSessionContext;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class ProvidedIdentifierMapping implements IdentifierMappingImplementor<Object, Object> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final BeanHolder<? extends IdentifierBridge<Object>> bridgeHolder;

	@SuppressWarnings("unchecked") // This class is bivariant in E
	public static <E> IdentifierMappingImplementor<Object, E> get(BeanHolder<? extends IdentifierBridge<Object>> bridgeHolder) {
		return (IdentifierMappingImplementor<Object, E>) new ProvidedIdentifierMapping( bridgeHolder );
	}

	private ProvidedIdentifierMapping(BeanHolder<? extends IdentifierBridge<Object>> bridgeHolder) {
		this.bridgeHolder = bridgeHolder;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "bridgeHolder=" + bridgeHolder
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
	public Object getIdentifier(Object providedId, Supplier<?> entityProvider) {
		if ( providedId == null ) {
			throw log.nullProvidedIdentifier();
		}
		return providedId;
	}

	@Override
	public Object getIdentifierOrNull(Object entity) {
		return null;
	}

	@Override
	public String toDocumentIdentifier(Object identifier, BridgeMappingContext context) {
		return bridgeHolder.get().toDocumentIdentifier( identifier, context.identifierBridgeToDocumentIdentifierContext() );
	}

	@Override
	public Object fromDocumentIdentifier(String documentId, BridgeSessionContext context) {
		return bridgeHolder.get().fromDocumentIdentifier( documentId, context.identifierBridgeFromDocumentIdentifierContext() );
	}

}
