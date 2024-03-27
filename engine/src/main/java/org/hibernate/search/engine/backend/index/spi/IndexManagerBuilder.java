/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.index.spi;

import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexRootBuilder;

public interface IndexManagerBuilder {

	/**
	 * Close any allocated resource.
	 * <p>
	 * This method is called when an error occurs while starting up Hibernate Search.
	 * When this method is called, it is guaranteed to be the last call on the builder.
	 */
	void closeOnFailure();

	/**
	 * @return An {@link IndexRootBuilder} allowing to contribute metadata about the index schema.
	 * <p>
	 * Never called after {@link #build()}.
	 */
	IndexRootBuilder schemaRootNodeBuilder();

	/**
	 * Build the mapping based on the {@link #schemaRootNodeBuilder()} metadata contributed} so far.
	 * <p>
	 * May only be called once on a given object.
	 *
	 * @return The index manager.
	 */
	IndexManagerImplementor build();

}
