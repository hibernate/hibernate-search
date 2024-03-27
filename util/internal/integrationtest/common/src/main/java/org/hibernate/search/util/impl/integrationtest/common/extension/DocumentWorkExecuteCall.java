/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.extension;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.StubDocumentWorkAssert.assertThatDocumentWork;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.util.common.spi.ToStringTreeAppender;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubTreeNodeDiffer;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.StubDocumentNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubDocumentWork;

class DocumentWorkExecuteCall extends Call<DocumentWorkExecuteCall> {

	private final DocumentKey documentKey;
	private final StubDocumentWork work;
	private final StubTreeNodeDiffer<StubDocumentNode> documentDiffer;
	private final CompletableFuture<?> completableFuture;

	// Constructor for expected call
	DocumentWorkExecuteCall(String indexName, StubDocumentWork work, StubTreeNodeDiffer<StubDocumentNode> documentDiffer,
			CompletableFuture<?> completableFuture) {
		this.documentKey = new DocumentKey( indexName, work.getTenantIdentifier(), work.getIdentifier() );
		this.work = work;
		this.documentDiffer = documentDiffer;
		this.completableFuture = completableFuture;
	}

	// Constructor for actual call
	DocumentWorkExecuteCall(String indexName, StubDocumentWork work) {
		this( indexName, work, null, null );
	}

	public DocumentKey documentKey() {
		return documentKey;
	}

	public CallBehavior<CompletableFuture<?>> verify(DocumentWorkExecuteCall actualCall) {
		assertThatDocumentWork( actualCall.work )
				.as( "Incorrect work when the execution of a work on document '" + documentKey + "' was expected:\n" )
				.documentDiffer( documentDiffer )
				.matches( work );
		return () -> completableFuture;
	}

	@Override
	protected boolean isSimilarTo(DocumentWorkExecuteCall other) {
		return Objects.equals( documentKey, other.documentKey );
	}

	@Override
	protected String summary() {
		return "execution of a " + work.getType() + " work on document '" + documentKey + "'";
	}

	@Override
	protected void details(ToStringTreeAppender appender) {
		appender.attribute( "documentKey", documentKey );
		appender.attribute( "work", work );
	}
}
