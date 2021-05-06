/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.loading.spi;

import java.util.List;
import java.util.Set;

import org.hibernate.search.engine.common.timing.spi.Deadline;

/**
 * A loader for loading a small selection of entities, used in particular during search.
 * <p>
 * Compared to {@link PojoMassEntityLoader}, this loader:
 * <ul>
 *     <li>Receives batches of identifiers from the caller.</li>
 *     <li>Is expected to load a small number of entities, potentially in a single batch.</li>
 *     <li>Returns loaded entities as a list.</li>
 *     <li>Relies on a pre-existing loading context (a session, a transaction, ...).</li>
 *     <li>Must ensure entities remain usable (lazy-loading, ...) as long as the supporting context is active.</li>
 * </ul>
 *
 * @param <E> A supertype of the type of loaded entities.
 */
public interface PojoLoader<E> {

	/**
	 * Loads the entities corresponding to the given identifiers, blocking the current thread while doing so.
	 *
	 * @param identifiers A list of identifiers for objects to load.
	 * @param deadline The deadline for loading the entities, or null if there is no deadline.
	 * @return A list of entities, in the same order the references were given.
	 * {@code null} is inserted when an object is not found or has an excluded types
	 * (see {@link org.hibernate.search.mapper.pojo.loading.spi.PojoLoadingContext#createLoader(Set)}).
	 */
	List<?> loadBlocking(List<?> identifiers, Deadline deadline);

}
