/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.impl;

import org.hibernate.search.engine.mapper.model.spi.IndexableModel;
import org.hibernate.search.engine.mapper.model.spi.IndexableReference;
import org.hibernate.search.mapper.pojo.model.spi.PojoIntrospector;
import org.hibernate.search.util.SearchException;


/**
 * @author Yoann Rodiere
 */
public class PojoRootIndexableModel implements IndexableModel {

	private final PojoIntrospector introspector;

	private final Class<?> type;

	public PojoRootIndexableModel(PojoIntrospector introspector, Class<?> type) {
		this.introspector = introspector;
		this.type = type;
	}

	@Override
	public <T> IndexableReference<T> asReference(Class<T> requestedType) {
		if ( !requestedType.isAssignableFrom( this.type ) ) {
			throw new SearchException( "Requested incompatible type for '" + asReference() + "': '" + requestedType + "'" );
		}
		return new PojoRootIndexableReference<>( requestedType );
	}

	@Override
	public PojoIndexableReference<?> asReference() {
		return new PojoRootIndexableReference<>( this.type );
	}

	@Override
	public IndexableModel property(String relativeName) {
		return new PojoPropertyNameIndexableModel( introspector, asReference(), relativeName );
	}
}
