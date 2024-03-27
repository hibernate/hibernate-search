/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.extension;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.StubDocumentWorkAssert.assertThatDocumentWork;

import java.util.Objects;

import org.hibernate.search.util.common.spi.ToStringTreeAppender;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubTreeNodeDiffer;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.StubDocumentNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubDocumentWork;

class DocumentWorkDiscardCall extends Call<DocumentWorkDiscardCall> {

	private final DocumentKey documentKey;
	private final StubDocumentWork work;
	private final StubTreeNodeDiffer<StubDocumentNode> documentDiffer;

	// Constructor for expected call
	DocumentWorkDiscardCall(String indexName, StubDocumentWork work, StubTreeNodeDiffer<StubDocumentNode> documentDiffer) {
		this.documentKey = new DocumentKey( indexName, work.getTenantIdentifier(), work.getIdentifier() );
		this.work = work;
		this.documentDiffer = documentDiffer;
	}

	// Constructor for actual call
	DocumentWorkDiscardCall(String indexName, StubDocumentWork work) {
		this( indexName, work, null );
	}

	public DocumentKey documentKey() {
		return documentKey;
	}

	public CallBehavior<Void> verify(DocumentWorkDiscardCall actualCall) {
		assertThatDocumentWork( actualCall.work )
				.as( "Incorrect work when the discarding of a work on document '" + documentKey + "' was expected:\n" )
				.documentDiffer( documentDiffer )
				.matches( work );
		return () -> null;
	}

	@Override
	protected boolean isSimilarTo(DocumentWorkDiscardCall other) {
		return Objects.equals( documentKey, other.documentKey );
	}

	@Override
	protected String summary() {
		return "discarding of a " + work.getType() + " work on document '" + documentKey + "'";
	}

	@Override
	protected void details(ToStringTreeAppender appender) {
		appender.attribute( "documentKey", documentKey );
		appender.attribute( "work", work );
	}
}
