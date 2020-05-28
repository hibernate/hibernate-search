/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.additionalmetadata.building.impl;

import java.util.Optional;

import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorIndexedTypeNode;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoIndexedTypeAdditionalMetadata;

class PojoIndexedTypeAdditionalMetadataBuilder implements PojoAdditionalMetadataCollectorIndexedTypeNode {

	private final PojoTypeAdditionalMetadataBuilder rootBuilder;
	private Optional<String> backendName = Optional.empty();
	private Optional<String> indexName = Optional.empty();
	private boolean enabled = true;

	PojoIndexedTypeAdditionalMetadataBuilder(PojoTypeAdditionalMetadataBuilder rootBuilder) {
		this.rootBuilder = rootBuilder;
	}

	@Override
	public ContextualFailureCollector failureCollector() {
		// There's nothing to add to the context
		return rootBuilder.failureCollector();
	}

	@Override
	public void backendName(String backendName) {
		this.backendName = Optional.ofNullable( backendName );
	}

	@Override
	public void indexName(String indexName) {
		this.indexName = Optional.ofNullable( indexName );
	}

	@Override
	public void enabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Optional<PojoIndexedTypeAdditionalMetadata> build() {
		if ( enabled ) {
			return Optional.of( new PojoIndexedTypeAdditionalMetadata( backendName, indexName ) );
		}
		else {
			return Optional.empty();
		}
	}
}
