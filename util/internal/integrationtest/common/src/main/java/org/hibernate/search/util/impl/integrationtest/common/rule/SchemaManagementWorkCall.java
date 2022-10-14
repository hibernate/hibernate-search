/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.rule;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.StubSchemaManagementWorkAssert.assertThatSchemaManagementWork;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubSchemaManagementWork;

class SchemaManagementWorkCall extends Call<SchemaManagementWorkCall> {

	private final String indexName;
	private final StubSchemaManagementWork work;
	private final ContextualFailureCollector failureCollector;

	private final SchemaManagementWorkBehavior behavior;

	SchemaManagementWorkCall(String indexName, StubSchemaManagementWork work,
			SchemaManagementWorkBehavior behavior) {
		this.indexName = indexName;
		this.work = work;
		this.behavior = behavior;
		this.failureCollector = null;
	}

	SchemaManagementWorkCall(String indexName, StubSchemaManagementWork work, ContextualFailureCollector failureCollector) {
		this.indexName = indexName;
		this.work = work;
		this.behavior = null;
		this.failureCollector = failureCollector;
	}

	public CallBehavior<CompletableFuture<?>> verify(SchemaManagementWorkCall actualCall) {
		String whenThisWorkWasExpected = "when a schema management work on index '" + indexName
				+ "' was expected";
		assertThatSchemaManagementWork( actualCall.work )
				.as( "Incorrect work " + whenThisWorkWasExpected + ":\n" )
				.matches( work );
		return () -> behavior.execute( actualCall.failureCollector );
	}

	@Override
	protected boolean isSimilarTo(SchemaManagementWorkCall other) {
		return Objects.equals( indexName, other.indexName );
	}

	@Override
	protected String summary() {
		return "schema management work on index '" + indexName + "'; work = " + work;
	}
}
