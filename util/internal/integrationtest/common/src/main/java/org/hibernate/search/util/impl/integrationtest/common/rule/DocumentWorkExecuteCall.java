/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.rule;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.StubDocumentWorkAssert.assertThatDocumentWork;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubDocumentWork;

class DocumentWorkExecuteCall extends Call<DocumentWorkExecuteCall> {

	private final DocumentKey documentKey;
	private final StubDocumentWork work;
	private final CompletableFuture<?> completableFuture;

	DocumentWorkExecuteCall(String indexName, StubDocumentWork work, CompletableFuture<?> completableFuture) {
		this.documentKey = new DocumentKey( indexName, work.getTenantIdentifier(), work.getIdentifier() );
		this.work = work;
		this.completableFuture = completableFuture;
	}

	DocumentWorkExecuteCall(String indexName, StubDocumentWork work) {
		this( indexName, work, null );
	}

	public DocumentKey documentKey() {
		return documentKey;
	}

	public CallBehavior<CompletableFuture<?>> verify(DocumentWorkExecuteCall actualCall) {
		assertThatDocumentWork( actualCall.work )
				.as( "Incorrect work when the execution of a work on document '" + documentKey + "' was expected:\n" )
				.matches( work );
		return () -> completableFuture;
	}

	@Override
	protected boolean isSimilarTo(DocumentWorkExecuteCall other) {
		return Objects.equals( documentKey, other.documentKey );
	}

	@Override
	public String toString() {
		return "execution of a work on document '" + documentKey + "'";
	}
}
