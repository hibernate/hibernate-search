/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.util.common.rule;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.integrationtest.util.common.stub.backend.index.StubIndexWork;

import junit.framework.AssertionFailedError;

import static org.hibernate.search.integrationtest.util.common.assertion.StubIndexWorkAssert.assertThat;

class IndexWorkCall {

	enum Operation {
		PREPARE,
		EXECUTE
	}

	private final String indexName;
	private final Operation operation;
	private final StubIndexWork work;

	IndexWorkCall(String indexName, Operation operation, StubIndexWork work) {
		this.indexName = indexName;
		this.operation = operation;
		this.work = work;
	}

	public CompletableFuture<?> verify(IndexWorkCall actualCall) {
		String whenThisWorkWasExpected = "when a " + operation + " call for a work on index '" + indexName
				+ "', identifier '" + work.getIdentifier() + "' was expected";
		if ( !Objects.equals( operation, actualCall.operation ) ) {
			throw new AssertionFailedError( "Incorrect work operation " + whenThisWorkWasExpected + ".\n\tExpected: "
					+ operation + ", actual: " + actualCall.operation
					+ ".\n\tExpected work: " + work + "\n\tActual work: " + actualCall.work );
		}
		assertThat( actualCall.work )
				.as( "Incorrect work " + whenThisWorkWasExpected + ": " )
				.matches( work );
		return CompletableFuture.completedFuture( null );
	}
	@Override
	public String toString() {
		return operation + " call for a work on index '" + indexName + "', identifier '" + work.getIdentifier()
				+ "'; work = " + work;
	}
}
