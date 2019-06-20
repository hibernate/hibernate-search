/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common.reflect.spi;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;



public abstract class AbstractTypeOrdering<T> {

	protected AbstractTypeOrdering() {
	}

	public Stream<? extends T> getAscendingSuperTypes(T subType) {
		// Use a LinkedHashSet to preserve order while still providing efficient element lookup
		Set<T> result = new LinkedHashSet<>();
		collectSuperTypesAscending( result, subType );
		return result.stream();
	}

	public Stream<? extends T> getDescendingSuperTypes(T subType) {
		// Use a LinkedHashSet to preserve order while still providing efficient element lookup
		Set<T> result = new LinkedHashSet<>();
		collectSuperTypesDescending( result, subType );
		return result.stream();
	}

	/**
	 * @param subType A type (non-null)
	 * @return The superclass of the given type, or null if there is none.
	 */
	protected abstract T getSuperClass(T subType);

	/**
	 * @param subType A type (non-null)
	 * @return A stream of all the interfaces of the given type, possibly empty.
	 */
	protected abstract Stream<T> getDeclaredInterfaces(T subType);

	private void collectSuperTypesAscending(Set<T> result, T subType) {
		if ( subType == null ) {
			// Reached the superclass of Object or of an interface
			return;
		}
		if ( ! result.add( subType ) ) {
			// We've already seen this type, skip the rest of this method
			return;
		}
		getDeclaredInterfaces( subType )
				.forEach( interfaze -> collectSuperTypesAscending( result, interfaze ) );
		collectSuperTypesAscending( result, getSuperClass( subType ) );
	}

	private void collectSuperTypesDescending(Set<T> result, T subType) {
		if ( subType == null ) {
			// Reached the superclass of Object or of an interface
			return;
		}
		if ( result.contains( subType ) ) {
			// We've already seen this type, skip the rest of this method
			return;
		}
		collectSuperTypesDescending( result, getSuperClass( subType ) );
		getDeclaredInterfaces( subType )
				.forEach( interfaze -> collectSuperTypesDescending( result, interfaze ) );
		result.add( subType );
	}

}
