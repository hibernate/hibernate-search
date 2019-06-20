/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.search.spi;

import java.util.Objects;

import org.hibernate.search.mapper.pojo.search.PojoReference;



public class PojoReferenceImpl implements PojoReference {

	private final Class<?> type;

	private final Object id;

	public PojoReferenceImpl(Class<?> type, Object id) {
		this.type = type;
		this.id = id;
	}

	@Override
	public Class<?> getType() {
		return type;
	}

	@Override
	public Object getId() {
		return id;
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj == null || obj.getClass() != getClass() ) {
			return false;
		}
		PojoReferenceImpl other = (PojoReferenceImpl) obj;
		return type.equals( other.type ) && Objects.equals( id, other.id );
	}

	@Override
	public int hashCode() {
		return Objects.hash( type, id );
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "type=" ).append( type )
				.append( ", id=" ).append( id )
				.append( "]" )
				.toString();
	}

}
