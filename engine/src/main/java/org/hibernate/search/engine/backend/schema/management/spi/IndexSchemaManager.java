/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.schema.management.spi;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A manager for the schema of a single index,
 * i.e. the data structure in which index data is stored.
 * It includes the core index files, but also any additional configuration required
 * by the backend such as field metadata or analyzer definitions.
 * <p>
 * A schema has to be created before indexing and searching can happen.
 * <p>
 * A schema can become obsolete, in which case it needs to be updated (not always possible)
 * or re-created (will drop any indexed data).
 */
public interface IndexSchemaManager {

	/**
	 * Creates the schema if it doesn't already exist.
	 * <p>
	 * Does not change or validate anything if the schema already exists.
	 *
	 * @param operationSubmitter How to handle request to submit operation when the queue is full.
	 * @return A future.
	 */
	CompletableFuture<?> createIfMissing(OperationSubmitter operationSubmitter);

	/**
	 * Creates the schema if it doesn't already exist,
	 * or validates the existing schema against requirements expressed by the mapper.
	 * <p>
	 * If the schema exists and validation happens, validation failures do not trigger an exception,
	 * but instead are pushed to the given collector.
	 *
	 * @param failureCollector A collector for validation failures.
	 * @param operationSubmitter How to handle request to submit operation when the queue is full.
	 * @return A future.
	 */
	CompletableFuture<?> createOrValidate(ContextualFailureCollector failureCollector,
			OperationSubmitter operationSubmitter);

	/**
	 * Creates the schema if it doesn't already exist,
	 * or updates the existing schema to match requirements expressed by the mapper.
	 * <p>
	 * Updating the schema may be impossible (for example if the type of a field changed).
	 * In this case, the future will ultimately be completed with a {@link SearchException}.
	 *
	 * @param operationSubmitter How to handle request to submit operation when the queue is full.
	 * @return A future.
	 */
	CompletableFuture<?> createOrUpdate(OperationSubmitter operationSubmitter);

	/**
	 * Drops the schema and all indexed data if it exists.
	 * <p>
	 * Does not change anything if the schema does not exist.
	 *
	 * @param operationSubmitter How to handle request to submit operation when the queue is full.
	 *
	 * @return A future.
	 */
	CompletableFuture<?> dropIfExisting(OperationSubmitter operationSubmitter);

	/**
	 * Drops the schema and all indexed data if it exists,
	 * then creates the schema.
	 *
	 * @param operationSubmitter How to handle request to submit operation when the queue is full.
	 *
	 * @return A future.
	 */
	CompletableFuture<?> dropAndCreate(OperationSubmitter operationSubmitter);

	/**
	 * Validates the existing schema against requirements expressed by the mapper.
	 * <p>
	 * If the schema does not exist, a failure is pushed to the given collector.
	 * <p>
	 * If the index exists and validation happens, validation failures do not trigger an exception,
	 * but instead are pushed to the given collector.
	 *
	 * @param failureCollector A collector for validation failures.
	 * @param operationSubmitter How to handle request to submit operation when the queue is full.
	 *
	 * @return A future.
	 */
	CompletableFuture<?> validate(ContextualFailureCollector failureCollector, OperationSubmitter operationSubmitter);

	/**
	 * Exports the generated schema based on the requirements expressed by the mapper into provided target directory.
	 * <p>
	 * Export results are backend specific.
	 *
	 * @param targetDirectory The directory to export to. Should be an empty directory.
	 * Exporting to a non-empty directory might result in an exception.
	 * @param name The name of the folder that will contain all files for the index represented by this schema manager.
	 */
	@Incubating
	void exportSchema(Path targetDirectory, String name);
}
