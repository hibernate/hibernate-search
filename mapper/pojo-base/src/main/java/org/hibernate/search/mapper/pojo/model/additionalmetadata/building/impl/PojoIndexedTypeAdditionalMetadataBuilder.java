/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.additionalmetadata.building.impl;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorIndexedTypeNode;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoIndexedTypeAdditionalMetadata;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class PojoIndexedTypeAdditionalMetadataBuilder implements PojoAdditionalMetadataCollectorIndexedTypeNode {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoTypeAdditionalMetadataBuilder rootBuilder;
	private final Optional<String> backendName;
	private final Optional<String> indexName;

	PojoIndexedTypeAdditionalMetadataBuilder(PojoTypeAdditionalMetadataBuilder rootBuilder,
			Optional<String> backendName, Optional<String> indexName) {
		this.rootBuilder = rootBuilder;
		this.backendName = backendName;
		this.indexName = indexName;
	}

	@Override
	public ContextualFailureCollector failureCollector() {
		// There's nothing to add to the context
		return rootBuilder.failureCollector();
	}

	void checkSameIndex(Optional<String> backendName, Optional<String> indexName) {
		if ( this.backendName.equals( backendName ) && this.indexName.equals( indexName ) ) {
			return;
		}
		throw log.multipleIndexMapping(
				this.backendName, this.indexName,
				backendName, indexName
		);
	}

	public PojoIndexedTypeAdditionalMetadata build() {
		return new PojoIndexedTypeAdditionalMetadata( backendName, indexName );
	}
}
