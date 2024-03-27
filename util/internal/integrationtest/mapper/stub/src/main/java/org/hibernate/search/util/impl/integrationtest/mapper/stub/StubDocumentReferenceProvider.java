/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.mapper.stub;

import org.hibernate.search.engine.backend.work.execution.spi.DocumentReferenceProvider;

class StubDocumentReferenceProvider implements DocumentReferenceProvider {

	private final String identifier;
	private final String routingKey;

	public StubDocumentReferenceProvider(String identifier, String routingKey) {
		this.identifier = identifier;
		this.routingKey = routingKey;
	}

	@Override
	public String identifier() {
		return identifier;
	}

	@Override
	public String routingKey() {
		return routingKey;
	}

	@Override
	public Object entityIdentifier() {
		return identifier;
	}
}
