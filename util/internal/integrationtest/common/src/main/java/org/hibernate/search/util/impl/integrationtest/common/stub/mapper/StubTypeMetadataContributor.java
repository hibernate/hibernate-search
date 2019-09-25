/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.mapper;

import java.util.function.Consumer;

import org.hibernate.search.engine.mapper.mapping.building.spi.MappingConfigurationCollector;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingContext;

class StubTypeMetadataContributor {

	private final StubTypeModel typeIdentifier;
	private final String backendName;
	private final String indexName;
	private final Consumer<? super IndexedEntityBindingContext> delegate;

	StubTypeMetadataContributor(StubTypeModel typeIdentifier, String backendName, String indexName,
			Consumer<? super IndexedEntityBindingContext> delegate) {
		this.typeIdentifier = typeIdentifier;
		this.backendName = backendName;
		this.indexName = indexName;
		this.delegate = delegate;
	}

	final void contribute(MappingConfigurationCollector<StubTypeMetadataContributor> collector) {
		collector.collectContributor( typeIdentifier, this );
	}

	public void contribute(IndexedEntityBindingContext bindingContext) {
		delegate.accept( bindingContext );
	}

	public String getBackendName() {
		return backendName;
	}

	public String getIndexName() {
		return indexName;
	}
}
