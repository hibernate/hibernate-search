/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.index.spi;

import java.util.concurrent.CompletableFuture;

/**
 * A worker that accumulates works in a list (called a changeset),
 * and executes them only when {@link #execute()} is called.
 * <p>
 * Relative ordering of works within a changeset will be preserved.
 * <p>
 * Implementations may not be thread-safe.
 *
 * @author Yoann Rodiere
 */
public interface ChangesetIndexWorker<D> extends IndexWorker<D> {

	/**
	 * Prepare the changeset execution, i.e. execute as much as possible without writing to the index.
	 * <p>
	 * Calling this method is optional: the {@link #execute()} method
	 * will perform the preparation if necessary.
	 */
	void prepare();

	CompletableFuture<?> execute();

}
