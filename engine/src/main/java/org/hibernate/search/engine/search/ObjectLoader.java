/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads objects into memory using a reference and implementation-specific context.
 *
 * @param <R> The expected reference type (input)
 * @param <O> The resulting object type (output)
 */
public interface ObjectLoader<R, O> {

	/**
	 * @param reference A reference to the object to load.
	 * @return The loaded object, or {@code null} if not found.
	 */
	O load(R reference);

	/**
	 * @param references A list of references to the objects to load.
	 * @return A list of loaded objects, in the same order the references were given.
	 * {@code null} is inserted when an object is not found.
	 */
	default List<O> load(List<R> references) {
		List<O> result = new ArrayList<>( references.size() );
		Map<R, O> objectsByReference = new HashMap<>( references.size() );
		for ( R reference : references ) {
			objectsByReference.put( reference, null );
		}
		load( references );
		for ( R reference : references ) {
			result.add( objectsByReference.get( reference ) );
		}
		return result;
	}

	static <T> ObjectLoader<T, T> identity() {
		return IdentityObjectLoader.get();
	}

}
