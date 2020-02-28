/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.schema.management.spi.IndexSchemaManager;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubSchemaManagementWork;

public class StubIndexSchemaManager implements IndexSchemaManager {

	private final String indexName;
	private final StubBackendBehavior behavior;

	public StubIndexSchemaManager(String indexName, StubBackendBehavior behavior) {
		this.indexName = indexName;
		this.behavior = behavior;
	}

	@Override
	public CompletableFuture<?> createIfMissing() {
		StubSchemaManagementWork work = StubSchemaManagementWork.builder( StubSchemaManagementWork.Type.CREATE_IF_MISSING ).build();
		return behavior.executeSchemaManagementWork( indexName, work, null );
	}

	@Override
	public CompletableFuture<?> createOrUpdate() {
		StubSchemaManagementWork work = StubSchemaManagementWork.builder( StubSchemaManagementWork.Type.CREATE_OR_UPDATE ).build();
		return behavior.executeSchemaManagementWork( indexName, work, null );
	}

	@Override
	public CompletableFuture<?> dropIfExisting() {
		StubSchemaManagementWork work = StubSchemaManagementWork.builder( StubSchemaManagementWork.Type.DROP_IF_EXISTING ).build();
		return behavior.executeSchemaManagementWork( indexName, work, null );
	}

	@Override
	public CompletableFuture<?> dropAndCreate() {
		StubSchemaManagementWork work = StubSchemaManagementWork.builder( StubSchemaManagementWork.Type.DROP_AND_CREATE ).build();
		return behavior.executeSchemaManagementWork( indexName, work, null );
	}

	@Override
	public CompletableFuture<?> validate(ContextualFailureCollector failureCollector) {
		StubSchemaManagementWork work = StubSchemaManagementWork.builder( StubSchemaManagementWork.Type.VALIDATE ).build();
		return behavior.executeSchemaManagementWork( indexName, work, failureCollector );
	}
}
