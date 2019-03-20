/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.mapper;

import java.util.function.Consumer;

import org.hibernate.search.engine.mapper.mapping.building.spi.IndexManagerBuildingState;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexModelBindingContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingConfigurationCollector;
import org.hibernate.search.engine.mapper.mapping.building.spi.RootIndexModelBindingContext;

class StubTypeMetadataContributor {

	private final StubTypeModel typeIdentifier;
	private final String indexName;
	private final Consumer<? super RootIndexModelBindingContext> delegate;

	StubTypeMetadataContributor(StubTypeModel typeIdentifier, String indexName, Consumer<? super RootIndexModelBindingContext> delegate) {
		this.typeIdentifier = typeIdentifier;
		this.indexName = indexName;
		this.delegate = delegate;
	}

	final void contribute(MappingConfigurationCollector<StubTypeMetadataContributor> collector) {
		if ( indexName != null ) {
			collector.mapToIndex( typeIdentifier, indexName );
		}
		collector.collectContributor( typeIdentifier, this );
	}

	public void contribute(IndexManagerBuildingState<?> indexManagerBuildingState) {
		delegate.accept( indexManagerBuildingState.getRootBindingContext() );
	}

}
