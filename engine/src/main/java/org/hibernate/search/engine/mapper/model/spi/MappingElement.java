/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.model.spi;

import org.hibernate.search.util.common.reporting.spi.EventContextProvider;

/**
 * A unique representation of an element in the mapping.
 * <p>
 * Mainly used as a key when some behavior must span all "occurrences" of a same mapping element
 * (e.g. all effective uses of an inherited {@code @IndexedEmbedded} across an entity hierarchy).
 */
public interface MappingElement extends EventContextProvider {

	/**
	 * @return A human-readable description of this element.
	 */
	@Override
	String toString();

	/**
	 * @return {@code true} if {@code obj} is a {@link MappingElement} referencing the exact same mapping element.
	 */
	@Override
	boolean equals(Object obj);

	/*
	 * Note to implementors: you must override hashCode to be consistent with equals().
	 */
	@Override
	int hashCode();

}
