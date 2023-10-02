/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.spi;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.engine.backend.index.spi.IndexManagerBuilder;
import org.hibernate.search.engine.backend.index.spi.IndexManagerStartContext;
import org.hibernate.search.engine.backend.mapping.spi.BackendMapperContext;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.IndexSettings;

public interface BackendImplementor {

	/**
	 * Start any resource necessary to operate the backend at runtime.
	 * <p>
	 * Called by the engine once after bootstrap, before
	 * {@link org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor#start(IndexManagerStartContext)}
	 * is called on the index managers.
	 *
	 * @param context The start context.
	 */
	void start(BackendStartContext context);

	/**
	 * Prepare for {@link #stop()}.
	 *
	 * @return A future that completes when ongoing works complete.
	 */
	CompletableFuture<?> preStop();

	/**
	 * Stop and release any resource necessary to operate the backend at runtime.
	 * <p>
	 * Called by the engine once before shutdown.
	 */
	void stop();

	/**
	 * @return The object that should be exposed as API to users.
	 */
	Backend toAPI();

	/**
	 * @param indexName The name of the index from the point of view of Hibernate Search.
	 * A slightly different name may be used by the backend internally,
	 * but {@code indexName} is the one that will appear everywhere the index is mentioned to the user.
	 * @param mappedTypeName The name of the type mapped to this index.
	 * This is the type name that will be assigned to search query hits for this index,
	 * allowing the mapper to resolve the type of each hit in multi-index searches.
	 * Each index is mapped to one and only one type.
	 * @param context The build context
	 * @param backendMapperContext
	 * @param propertySource A configuration property source, appropriately masked so that the backend
	 * doesn't need to care about Hibernate Search prefixes (hibernate.search.*, etc.). All the properties
	 * can be accessed at the root.
	 * <strong>CAUTION:</strong> the property keys listed in {@link IndexSettings}
	 * are reserved for use by the engine.
	 * @return A builder for index managers targeting this backend.
	 */
	IndexManagerBuilder createIndexManagerBuilder(String indexName, String mappedTypeName, BackendBuildContext context,
			BackendMapperContext backendMapperContext,
			ConfigurationPropertySource propertySource);

}
