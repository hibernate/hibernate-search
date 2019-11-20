/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.spi;

import org.hibernate.search.util.common.impl.Contracts;

/**
 * An identifier for POJO types.
 * <p>
 * On contrary to type models, type identifiers can be manipulated at runtime (after bootstrap),
 * but they do not provide any reflection capabilities.
 *
 * @see PojoRawTypeModel
 */
public final class PojoRawTypeIdentifier<T> {

	public static <T> PojoRawTypeIdentifier<T> of(Class<T> javaClass) {
		return new PojoRawTypeIdentifier<>( javaClass );
	}

	private final Class<T> javaClass;

	private PojoRawTypeIdentifier(Class<T> javaClass) {
		Contracts.assertNotNull( javaClass, "javaClass" );
		this.javaClass = javaClass;
	}

	@Override
	public String toString() {
		return javaClass.getName();
	}

	@Override
	public boolean equals(Object obj) {
		if ( ! ( obj instanceof PojoRawTypeIdentifier ) ) {
			return false;
		}
		PojoRawTypeIdentifier other = (PojoRawTypeIdentifier) obj;
		return javaClass.equals( other.javaClass );
	}

	@Override
	public int hashCode() {
		return javaClass.hashCode();
	}

	/**
	 * @return The exact Java {@link Class} for this type.
	 */
	public Class<T> getJavaClass() {
		return javaClass;
	}
}
