/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.loading;

import java.util.List;

import org.hibernate.search.engine.common.timing.Deadline;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A loader for loading a small selection of entities, used in particular during search.
 * <p>
 * Compared to {@link MassEntityLoader}, this loader:
 * <ul>
 *     <li>Receives batches of identifiers from the caller.</li>
 *     <li>Is expected to load a small number of entities, potentially in a single batch.</li>
 *     <li>Returns loaded entities as a list.</li>
 *     <li>Relies on a pre-existing loading context (a session, a transaction, ...).</li>
 *     <li>Must ensure entities remain usable (lazy-loading, ...) as long as the supporting context is active.</li>
 * </ul>
 *
 * @param <E> The type of loaded entities.
 */
@Incubating
public interface SelectionEntityLoader<E> {

	/**
	 * Loads the entities corresponding to the given identifiers, blocking the current thread while doing so.
	 *
	 * @param identifiers A list of identifiers for objects to load.
	 * @param deadline The deadline for loading the entities, or {@code null} if there is no deadline.
	 * Should be complied with on a best-effort basis: it's acceptable to ignore it,
	 * but it means some timeouts in Hibernate Search will not work properly.
	 * @return A list of entities, in the same order the identifiers were given.
	 * {@code null} is inserted when an object is not found or has the wrong concrete type.
	 */
	List<E> load(List<?> identifiers, Deadline deadline);

}
