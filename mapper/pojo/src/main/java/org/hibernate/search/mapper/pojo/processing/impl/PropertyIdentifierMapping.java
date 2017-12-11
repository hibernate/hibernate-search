/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.impl;

import java.util.function.Supplier;

import org.hibernate.search.mapper.pojo.bridge.spi.IdentifierBridge;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;
import org.hibernate.search.util.SearchException;

/**
 * @author Yoann Rodiere
 */
public class PropertyIdentifierMapping<I, E> implements IdentifierMapping<I, E> {

	private final Class<I> type;
	private final PropertyHandle property;
	private final IdentifierBridge<I> bridge;

	@SuppressWarnings("unchecked")
	public PropertyIdentifierMapping(PropertyHandle property, IdentifierBridge<I> bridge) {
		this.type = (Class<I>) property.getType();
		this.property = property;
		this.bridge = bridge;
	}

	@Override
	public I getIdentifier(Object providedId, Supplier<? extends E> entitySupplier) {
		if ( providedId != null ) {
			return type.cast( providedId );
		}
		else if ( property != null ) {
			Object id = property.get( entitySupplier.get() );
			return type.cast( id );
		}
		else {
			throw new SearchException( "No identifier was provided, and this mapping does not define"
					+ " how to extract the identifier from the entity" );
		}
	}

	@Override
	public String toDocumentIdentifier(I identifier) {
		return bridge.toString( identifier );
	}

	@Override
	public I fromDocumentIdentifier(String documentId) {
		return bridge.fromString( documentId );
	}

}
