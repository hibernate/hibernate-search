/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.impl;

import org.hibernate.search.engine.backend.work.execution.spi.DocumentReferenceProvider;

public final class PojoDocumentReferenceProvider implements DocumentReferenceProvider {

	private final String documentIdentifier;
	private final String routingKey;

	public PojoDocumentReferenceProvider(String documentIdentifier,
			String routingKey) {
		this.documentIdentifier = documentIdentifier;
		this.routingKey = routingKey;
	}

	@Override
	public String getIdentifier() {
		return documentIdentifier;
	}

	@Override
	public String getRoutingKey() {
		return routingKey;
	}

}
