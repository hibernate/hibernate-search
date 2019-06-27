/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.rule;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.util.impl.integrationtest.common.assertion.StubIndexScopeWorkAssert;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubIndexScopeWork;

class IndexScopeWorkCall extends Call<IndexScopeWorkCall> {

	private final Set<String> indexNames;
	private final StubIndexScopeWork work;
	private final CompletableFuture<?> completableFuture;

	IndexScopeWorkCall(Set<String> indexNames, StubIndexScopeWork work,
			CompletableFuture<?> completableFuture) {
		this.indexNames = indexNames;
		this.work = work;
		this.completableFuture = completableFuture;
	}

	IndexScopeWorkCall(Set<String> indexNames, StubIndexScopeWork work) {
		this.indexNames = indexNames;
		this.work = work;
		this.completableFuture = null;
	}

	public CallBehavior<CompletableFuture<?>> verify(IndexScopeWorkCall actualCall) {
		String whenThisWorkWasExpected = "when an index-scope work on indexes '" + indexNames
				+ "' was expected";
		StubIndexScopeWorkAssert.assertThat( actualCall.work )
				.as( "Incorrect work " + whenThisWorkWasExpected + ":\n" )
				.matches( work );
		return () -> completableFuture;
	}

	@Override
	protected boolean isSimilarTo(IndexScopeWorkCall other) {
		return Objects.equals( indexNames, other.indexNames )
				&& Objects.equals( work.getTenantIdentifier(), other.work.getTenantIdentifier() );
	}

	@Override
	public String toString() {
		return "index-scope work on indexes " + indexNames
				+ "'; work = " + work;
	}
}
