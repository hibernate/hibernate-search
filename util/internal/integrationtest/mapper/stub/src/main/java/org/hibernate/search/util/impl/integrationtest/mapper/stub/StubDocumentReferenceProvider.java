/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
