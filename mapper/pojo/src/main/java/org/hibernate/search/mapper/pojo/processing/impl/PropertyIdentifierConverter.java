/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.impl;

import org.hibernate.search.engine.bridge.spi.IdentifierBridge;
import org.hibernate.search.mapper.pojo.model.spi.ReadableProperty;
import org.hibernate.search.util.SearchException;

/**
 * @author Yoann Rodiere
 */
public class PropertyIdentifierConverter<I, E> implements IdentifierConverter<I, E> {

	private final Class<I> type;
	private final ReadableProperty property;
	private final IdentifierBridge<I> bridge;

	@SuppressWarnings("unchecked")
	public PropertyIdentifierConverter(ReadableProperty property, IdentifierBridge<I> bridge) {
		this.type = (Class<I>) property.getType();
		this.property = property;
		this.bridge = bridge;
	}

	@Override
	public String toDocumentId(Object providedId, E entity) {
		if ( providedId != null ) {
			return bridge.toString( type.cast( providedId ) );
		}
		else if ( property != null ) {
			Object id = property.invoke( entity );
			return bridge.toString( type.cast( id ) );
		}
		else {
			throw new SearchException( "No identifier was provided, and this mapping does not define"
					+ " how to extract the identifier from the entity" );
		}
	}

	@Override
	public I fromDocumentId(String id) {
		return bridge.fromString( id );
	}

}
