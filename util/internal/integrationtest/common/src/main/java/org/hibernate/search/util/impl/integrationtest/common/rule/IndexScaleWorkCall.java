/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.rule;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.StubIndexScaleWorkAssert.assertThatIndexScaleWork;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubIndexScaleWork;

class IndexScaleWorkCall extends Call<IndexScaleWorkCall> {

	private final String indexName;
	private final StubIndexScaleWork work;
	private final CompletableFuture<?> completableFuture;

	IndexScaleWorkCall(String indexName, StubIndexScaleWork work,
			CompletableFuture<?> completableFuture) {
		this.indexName = indexName;
		this.work = work;
		this.completableFuture = completableFuture;
	}

	IndexScaleWorkCall(String indexName, StubIndexScaleWork work) {
		this.indexName = indexName;
		this.work = work;
		this.completableFuture = null;
	}

	public CallBehavior<CompletableFuture<?>> verify(IndexScaleWorkCall actualCall) {
		String whenThisWorkWasExpected = "when index-scale work of type '" + work.getType() + "' on index '" + indexName
				+ "' was expected";
		assertThatIndexScaleWork( actualCall.work )
				.as( "Incorrect work " + whenThisWorkWasExpected + ":\n" )
				.matches( work );
		return () -> completableFuture;
	}

	@Override
	protected boolean isSimilarTo(IndexScaleWorkCall other) {
		return Objects.equals( indexName, other.indexName )
				&& Objects.equals( work.getTenantIdentifiers(), other.work.getTenantIdentifiers() );
	}

	@Override
	protected String summary() {
		return "index-scale work on index '" + indexName + "'; work = " + work;
	}
}
