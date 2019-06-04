/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import java.util.function.Supplier;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoSessionContextImplementor;
import org.hibernate.search.mapper.pojo.mapping.context.spi.AbstractPojoMappingContextImplementor;
import org.hibernate.search.mapper.pojo.model.spi.PojoCaster;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;
import org.hibernate.search.util.common.impl.Closer;

/**
 * @author Yoann Rodiere
 */
public class PropertyIdentifierMapping<I, E> implements IdentifierMapping<I, E> {

	private final PojoCaster<? super I> caster;
	private final PropertyHandle<I> property;
	private final BeanHolder<? extends IdentifierBridge<I>> bridgeHolder;

	public PropertyIdentifierMapping(PojoCaster<? super I> caster, PropertyHandle<I> property,
			BeanHolder<? extends IdentifierBridge<I>> bridgeHolder) {
		this.caster = caster;
		this.property = property;
		this.bridgeHolder = bridgeHolder;
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( holder -> holder.get().close(), bridgeHolder );
			closer.push( BeanHolder::close, bridgeHolder );
		}
	}

	@Override
	@SuppressWarnings( "unchecked" ) // We can only cast to the raw type, if I is generic we need an unchecked cast
	public I getIdentifier(Object providedId) {
		return (I) caster.cast( providedId );
	}

	@Override
	public I getIdentifier(Object providedId, Supplier<? extends E> entitySupplier) {
		if ( providedId != null ) {
			return getIdentifier( providedId );
		}
		return property.get( entitySupplier.get() );
	}

	@Override
	public String toDocumentIdentifier(I identifier, AbstractPojoMappingContextImplementor context) {
		return bridgeHolder.get().toDocumentIdentifier( identifier, context.getIdentifierBridgeToDocumentIdentifierContext() );
	}

	@Override
	public I fromDocumentIdentifier(String documentId, AbstractPojoSessionContextImplementor context) {
		return bridgeHolder.get().fromDocumentIdentifier( documentId, context.getIdentifierBridgeFromDocumentIdentifierContext() );
	}

}
