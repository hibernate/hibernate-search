/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.rule;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.StubDocumentWorkAssert.assertThatDocumentWork;

import java.util.Objects;

import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubDocumentWork;

class DocumentWorkDiscardCall extends Call<DocumentWorkDiscardCall> {

	private final DocumentKey documentKey;
	private final StubDocumentWork work;

	DocumentWorkDiscardCall(String indexName, StubDocumentWork work) {
		this.documentKey = new DocumentKey( indexName, work.getTenantIdentifier(), work.getIdentifier() );
		this.work = work;
	}

	public DocumentKey documentKey() {
		return documentKey;
	}

	public CallBehavior<Void> verify(DocumentWorkDiscardCall actualCall) {
		assertThatDocumentWork( actualCall.work )
				.as( "Incorrect work when the discarding of a work on document '" + documentKey + "' was expected:\n" )
				.matches( work );
		return () -> null;
	}

	@Override
	protected boolean isSimilarTo(DocumentWorkDiscardCall other) {
		return Objects.equals( documentKey, other.documentKey );
	}

	@Override
	public String toString() {
		return "discarding of a work on document '" + documentKey + "'";
	}
}
