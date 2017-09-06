/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.spi;

import java.util.Set;

/**
 * Represents a set of IndexedTypeIdentifier instances.
 * Could be an empty set, a singleton set or actually multiple type identifiers.
 */
public interface IndexedTypeSet extends Iterable<IndexedTypeIdentifier> {

	int size();

	boolean isEmpty();

	/**
	 * Return the set of the underlying POJOs.
	 * @deprecated This only exists to facilitate an iterative integration, and will be removed ASAP.
	 */
	@Deprecated
	Set<Class<?>> toPojosSet();

	/**
	 * @param subsetCandidate
	 * @return
	 */
	boolean containsAll(IndexedTypeSet subsetCandidate);

}
