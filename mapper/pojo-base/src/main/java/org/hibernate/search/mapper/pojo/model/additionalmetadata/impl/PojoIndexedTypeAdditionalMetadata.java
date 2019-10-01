/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.additionalmetadata.impl;

import java.util.Optional;

public class PojoIndexedTypeAdditionalMetadata {
	private final Optional<String> backendName;
	private final Optional<String> indexName;

	public PojoIndexedTypeAdditionalMetadata(Optional<String> backendName,
			Optional<String> indexName) {
		this.backendName = backendName;
		this.indexName = indexName;
	}

	public Optional<String> getBackendName() {
		return backendName;
	}

	public Optional<String> getIndexName() {
		return indexName;
	}
}
