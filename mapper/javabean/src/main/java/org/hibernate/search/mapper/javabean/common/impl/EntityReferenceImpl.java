/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.common.impl;

import java.util.Objects;

import org.hibernate.search.mapper.javabean.common.EntityReference;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

public class EntityReferenceImpl implements EntityReference {

	public static EntityReference withDefaultName(Class<?> type, Object id) {
		return new EntityReferenceImpl( PojoRawTypeIdentifier.of( type ), type.getSimpleName(), id );
	}

	private final PojoRawTypeIdentifier<?> typeIdentifier;

	private final String name;

	private final Object id;

	public EntityReferenceImpl(PojoRawTypeIdentifier<?> typeIdentifier, String name, Object id) {
		this.typeIdentifier = typeIdentifier;
		this.name = name;
		this.id = id;
	}

	@Override
	public Class<?> getType() {
		return typeIdentifier.getJavaClass();
	}

	@Override
	public String getName() {
		return name;
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
		EntityReferenceImpl other = (EntityReferenceImpl) obj;
		return name.equals( other.name ) && Objects.equals( id, other.id );
	}

	@Override
	public int hashCode() {
		return Objects.hash( name, id );
	}

	@Override
	public String toString() {
		// Apparently this is the usual format for references to Hibernate ORM entities.
		// Let's use the same format here, even if we're not using Hibernate ORM: it's good enough.
		return name + "#" + id;
	}

}
