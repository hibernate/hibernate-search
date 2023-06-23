/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.identity.impl;

import java.lang.invoke.MethodHandles;
import java.util.function.Supplier;

import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeMappingContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeSessionContext;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.spi.PojoCaster;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;

public final class UnmappedPropertyIdentifierMapping<I, E> implements IdentifierMappingImplementor<I, E> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoCaster<? super I> caster;
	private final ValueReadHandle<I> property;
	private final PojoRawTypeIdentifier<E> typeIdentifier;

	public UnmappedPropertyIdentifierMapping(PojoRawTypeIdentifier<E> typeIdentifier, PojoCaster<? super I> caster,
			ValueReadHandle<I> property) {
		this.caster = caster;
		this.property = property;
		this.typeIdentifier = typeIdentifier;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[entityType = " + typeIdentifier + "]";
	}

	@Override
	public void close() {
		// Nothing to close
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
	public String toDocumentIdentifier(Object identifier, BridgeMappingContext context) {
		throw log.cannotWorkWithIdentifierBecauseUnconfiguredIdentifierMapping( typeIdentifier );
	}

	@Override
	public I fromDocumentIdentifier(String documentId, BridgeSessionContext sessionContext) {
		throw log.cannotWorkWithIdentifierBecauseUnconfiguredIdentifierMapping( typeIdentifier );
	}
}
