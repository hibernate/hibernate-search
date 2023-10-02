/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.massindexing;

import org.hibernate.search.mapper.pojo.massindexing.MassIndexingEntityFailureContext;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingFailureContext;

/**
 * A handler for failures occurring during mass indexing.
 * <p>
 * The handler should be used to report failures to application maintainers.
 * The default failure handler simply delegates to the configured {@link org.hibernate.search.engine.reporting.FailureHandler},
 * which by default logs failures at the {@code ERROR} level,
 * but it can be replaced with a custom implementations
 * by configuring the mass indexer.
 * <p>
 * Handlers can be called from multiple threads simultaneously: implementations must be thread-safe.
 * @deprecated move to {@link org.hibernate.search.mapper.pojo.massindexing.MassIndexingFailureHandler}.
 */
@Deprecated
public interface MassIndexingFailureHandler extends org.hibernate.search.mapper.pojo.massindexing.MassIndexingFailureHandler {

	/**
	 * Handle a generic failure.
	 * <p>
	 * This method is expected to report the failure somewhere (logs, ...),
	 * then return as quickly as possible.
	 * Heavy error processing (sending emails, ...), if any, should be done asynchronously.
	 * <p>
	 * Any error or exception thrown by this method will be caught by Hibernate Search and logged.
	 *
	 * @param context Contextual information about the failure (throwable, operation, ...)
	 * @deprecated move to {@link org.hibernate.search.mapper.pojo.massindexing.MassIndexingFailureHandler#handle(MassIndexingFailureContext)}.
	 */
	@Deprecated
	@Override
	void handle(MassIndexingFailureContext context);

	/**
	 * Handle a failure when indexing an entity.
	 * <p>
	 * This method is expected to report the failure somewhere (logs, ...),
	 * then return as quickly as possible.
	 * Heavy error processing (sending emails, ...), if any, should be done asynchronously.
	 * <p>
	 * Any error or exception thrown by this method will be caught by Hibernate Search and logged.
	 *
	 * @param context Contextual information about the failure (throwable, operation, ...)
	 * @deprecated move to {@link org.hibernate.search.mapper.pojo.massindexing.MassIndexingFailureHandler#handle(MassIndexingEntityFailureContext)}.
	 */
	@Deprecated
	@Override
	default void handle(MassIndexingEntityFailureContext context) {
		handle( (MassIndexingFailureContext) context );
	}

}
