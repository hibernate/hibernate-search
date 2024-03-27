/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.hibernate.search.engine.common.timing.Deadline;
import org.hibernate.search.engine.common.timing.spi.TimingSource;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContext;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.search.query.SearchScroll;
import org.hibernate.search.engine.search.query.SearchScrollResult;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl.StubIndexModel;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubDocumentWork;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubIndexScaleWork;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubSchemaManagementWork;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl.StubBackendBuildContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl.StubIndexCreateContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjection;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjectionContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.query.impl.StubSearchWork;

public abstract class StubBackendBehavior {

	protected StubBackendBehavior() {
	}

	public abstract void onCreateBackend(StubBackendBuildContext context,
			CompletionStage<BackendMappingHandle> mappingHandlePromise);

	public abstract void onStopBackend();

	public abstract void onCreateIndex(StubIndexCreateContext context);

	public abstract void onAddField(String indexName, String absoluteFieldPath);

	public abstract void defineSchema(String indexName, StubIndexModel indexModel);

	public abstract CompletableFuture<?> executeSchemaManagementWork(String indexName, StubSchemaManagementWork work,
			ContextualFailureCollector failureCollector);

	public abstract void createDocumentWork(String indexName, StubDocumentWork work);

	public abstract void discardDocumentWork(String indexName, StubDocumentWork work);

	public abstract CompletableFuture<?> executeDocumentWork(String indexName, StubDocumentWork work);

	public abstract <T> SearchResult<T> executeSearchWork(Set<String> indexNames, StubSearchWork work,
			StubSearchProjectionContext projectionContext, SearchLoadingContext<?> loadingContext,
			StubSearchProjection<T> rootProjection, Deadline deadline);

	public abstract CompletableFuture<?> executeIndexScaleWork(String indexName, StubIndexScaleWork work);

	public abstract long executeCountWork(Set<String> indexNames);

	public abstract <T> SearchScroll<T> executeScrollWork(Set<String> indexNames, StubSearchWork work, int chunkSize,
			StubSearchProjectionContext projectionContext, SearchLoadingContext<?> loadingContext,
			StubSearchProjection<T> rootProjection, TimingSource timingSource);

	public abstract void executeCloseScrollWork(Set<String> indexNames);

	public abstract <T> SearchScrollResult<T> executeNextScrollWork(Set<String> indexNames, StubSearchWork work,
			StubSearchProjectionContext projectionContext, SearchLoadingContext<?> loadingContext,
			StubSearchProjection<T> rootProjection, Deadline deadline);

}
