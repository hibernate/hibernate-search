/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.util.common.stub.mapper;

import java.util.function.Consumer;

import org.hibernate.search.engine.mapper.mapping.building.spi.IndexManagerBuildingState;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexModelBindingContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.MetadataCollector;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataContributor;
import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;

class StubTypeMetadataContributor implements TypeMetadataContributor {

	private final StubTypeModel typeIdentifier;
	private final String indexName;
	private final Consumer<IndexModelBindingContext> delegate;

	StubTypeMetadataContributor(StubTypeModel typeIdentifier, String indexName, Consumer<IndexModelBindingContext> delegate) {
		this.typeIdentifier = typeIdentifier;
		this.indexName = indexName;
		this.delegate = delegate;
	}

	final void contribute(StubMapperFactory factory, MetadataCollector collector) {
		collector.collect( factory, typeIdentifier, indexName, this );
	}

	@Override
	public void beforeNestedContributions(MappableTypeModel typeModel) {
		// No-op
	}

	public void contribute(IndexManagerBuildingState<?> indexManagerBuildingState) {
		delegate.accept( indexManagerBuildingState.getRootBindingContext() );
	}

}
