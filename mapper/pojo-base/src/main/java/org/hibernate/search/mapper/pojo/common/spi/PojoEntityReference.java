/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.common.spi;

import java.util.Objects;

import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

/**
 * A simple, default implementation for {@link EntityReference} for POJO-based mappers.
 * <p>
 * Should be used instead of custom implementations, whose support is going to be removed in the future.
 */
public final class PojoEntityReference implements EntityReference {

	public static EntityReference withDefaultName(Class<?> javaClass, Object id) {
		return withName( javaClass, javaClass.getSimpleName(), id );
	}

	public static EntityReference withName(Class<?> javaClass, String entityName, Object id) {
		return new PojoEntityReference( PojoRawTypeIdentifier.of( javaClass ), entityName, id );
	}

	private final PojoRawTypeIdentifier<?> typeIdentifier;

	private final String name;

	private final Object id;

	public PojoEntityReference(PojoRawTypeIdentifier<?> typeIdentifier, String name, Object id) {
		this.typeIdentifier = typeIdentifier;
		this.name = name;
		this.id = id;
	}

	@Override
	public Class<?> type() {
		return typeIdentifier.javaClass();
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public Object id() {
		return id;
	}

	@Override
	public boolean equals(Object obj) {
		if ( !( obj instanceof EntityReference ) ) {
			return false;
		}
		EntityReference other = (EntityReference) obj;
		return name.equals( other.name() ) && Objects.equals( id, other.id() );
	}

	@Override
	public int hashCode() {
		return Objects.hash( name, id );
	}

	@Override
	public String toString() {
		// Apparently this is the usual format for references to Hibernate ORM entities.
		return name + "#" + id;
	}

}
