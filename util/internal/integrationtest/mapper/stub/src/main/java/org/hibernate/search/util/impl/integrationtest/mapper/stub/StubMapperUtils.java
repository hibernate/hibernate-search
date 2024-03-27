/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.mapper.stub;

import org.hibernate.search.engine.backend.work.execution.spi.DocumentContributor;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentReferenceProvider;

public final class StubMapperUtils {

	private StubMapperUtils() {
	}

	public static DocumentReferenceProvider referenceProvider(String identifier) {
		return referenceProvider( identifier, null );
	}

	public static DocumentReferenceProvider referenceProvider(String identifier, String routingKey) {
		return new StubDocumentReferenceProvider( identifier, routingKey );
	}

	public static StubDocumentProvider documentProvider(String identifier, DocumentContributor contributor) {
		return documentProvider( identifier, null, contributor );
	}

	public static StubDocumentProvider documentProvider(String identifier, String routingKey,
			DocumentContributor contributor) {
		return new StubDocumentProvider( referenceProvider( identifier, routingKey ), contributor );
	}

}
