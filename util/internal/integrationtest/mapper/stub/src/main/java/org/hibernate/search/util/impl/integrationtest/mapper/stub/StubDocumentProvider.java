/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.mapper.stub;

import org.hibernate.search.engine.backend.work.execution.spi.DocumentContributor;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentReferenceProvider;

public class StubDocumentProvider {

	private final DocumentReferenceProvider referenceProvider;
	private final DocumentContributor contributor;

	public StubDocumentProvider(DocumentReferenceProvider referenceProvider, DocumentContributor contributor) {
		this.referenceProvider = referenceProvider;
		this.contributor = contributor;
	}

	public DocumentReferenceProvider getReferenceProvider() {
		return referenceProvider;
	}

	public DocumentContributor getContributor() {
		return contributor;
	}
}
