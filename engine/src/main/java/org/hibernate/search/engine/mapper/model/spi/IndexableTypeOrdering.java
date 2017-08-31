/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.model.spi;

import java.util.stream.Stream;

/**
 * @author Yoann Rodiere
 */
public interface IndexableTypeOrdering {

	boolean isSubType(IndexedTypeIdentifier parent, IndexedTypeIdentifier subType);

	/**
	 * @param subType The type whose supertypes should be returned
	 * @return A stream containing the type and supertypes in ascending order.
	 * Each type must only appear once.
	 */
	Stream<? extends IndexedTypeIdentifier> getAscendingSuperTypes(IndexedTypeIdentifier subType);

	/**
	 * @param subType The type whose supertypes should be returned
	 * @return A stream containing the type and supertypes in descending order.
	 * Each type must only appear once.
	 */
	Stream<? extends IndexedTypeIdentifier> getDescendingSuperTypes(IndexedTypeIdentifier subType);

}
