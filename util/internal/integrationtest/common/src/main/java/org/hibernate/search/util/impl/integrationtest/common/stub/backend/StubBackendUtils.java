package org.hibernate.search.util.impl.integrationtest.common.stub.backend;

import org.hibernate.search.engine.backend.common.DocumentReference;

public final class StubBackendUtils {

	private StubBackendUtils() {
	}

	public static DocumentReference reference(String typeName, String id) {
		return new StubDocumentReference( typeName, id );
	}
}
