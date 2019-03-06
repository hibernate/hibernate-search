/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.impl;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.StubDocumentNode;

public class StubDocumentElement implements DocumentElement {

	private final StubDocumentNode.Builder builder;

	public StubDocumentElement(StubDocumentNode.Builder builder) {
		this.builder = builder;
	}

	@Override
	public <F> void addValue(IndexFieldReference<F> fieldReference, F value) {
		// FIXME Implement this
		throw new UnsupportedOperationException();
	}

	@Override
	public DocumentElement addObject(IndexObjectFieldReference fieldReference) {
		// FIXME Implement this
		throw new UnsupportedOperationException();
	}

	@Override
	public void addNullObject(IndexObjectFieldReference fieldReference) {
		// FIXME Implement this
		throw new UnsupportedOperationException();
	}

	public void putValue(String relativeFieldName, Object value) {
		builder.field( relativeFieldName, value );
	}

	public StubDocumentElement putChild(String relativeFieldName) {
		StubDocumentNode.Builder childBuilder = StubDocumentNode.object( builder, relativeFieldName );
		builder.child( childBuilder );
		return new StubDocumentElement( childBuilder );
	}

	public void putMissingChild(String relativeFieldName) {
		builder.missingObjectField( relativeFieldName );
	}
}
