/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.rule;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubIndexWork;
import org.hibernate.search.util.impl.integrationtest.common.assertion.StubIndexWorkAssert;

import org.junit.Assert;

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
			Assert.fail( "Incorrect work operation " + whenThisWorkWasExpected + ".\n\tExpected: "
					+ operation + ", actual: " + actualCall.operation
					+ ".\n\tExpected work: " + work + "\n\tActual work: " + actualCall.work );
		}
		StubIndexWorkAssert.assertThat( actualCall.work )
				.as( "Incorrect work " + whenThisWorkWasExpected + ":\n" )
				.matches( work );
		return CompletableFuture.completedFuture( null );
	}
	@Override
	public String toString() {
		return operation + " call for a work on index '" + indexName + "', identifier '" + work.getIdentifier()
				+ "'; work = " + work;
	}
}
