/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.spi;

import java.util.concurrent.CompletableFuture;

/**
 * Interface used by the engine to manipulate mappings
 * <p>
 * Publicly exposed mapping interfaces do not have to extend this interface;
 * only the implementations have to implement it.
 *
 * @param <M> The concrete type for this implementor.
 */
public interface MappingImplementor<M> {

	M toConcreteType();

	/**
	 * Start any resource necessary to operate the mapping at runtime.
	 * <p>
	 * Called by the engine once during bootstrap,
	 * after backends and index managers were started.
	 *
	 * @param context The start context.
	 * @return A future that completes when the mapper is completely started.
	 */
	CompletableFuture<?> start(MappingStartContext context);

	/**
	 * Prepare for {@link #stop()},
	 * executing any operations that needs to be executed before shutdown.
	 *
	 * @param context The pre-stop context.
	 * @return A future that completes when pre-stop operations complete.
	 */
	CompletableFuture<?> preStop(MappingPreStopContext context);

	/**
	 * Stop and release any resource necessary to operate the mapping at runtime.
	 * <p>
	 * Called by the engine once before shutdown.
	 */
	void stop();

}
