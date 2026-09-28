package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl;

import org.hibernate.search.engine.backend.common.DocumentReference;

class StubDocumentReferenceProjection extends AbstractStubPassThroughProjection<DocumentReference> {
	static final StubDocumentReferenceProjection INSTANCE = new StubDocumentReferenceProjection();

	@Override
	protected String typeName() {
		return "documentReference";
	}
}
