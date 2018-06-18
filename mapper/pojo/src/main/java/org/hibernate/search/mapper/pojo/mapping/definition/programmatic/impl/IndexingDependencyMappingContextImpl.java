/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import org.hibernate.search.mapper.pojo.dirtiness.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractorPath;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.IndexingDependencyMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingContext;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorPropertyNode;


/**
 * @author Yoann Rodiere
 */
public class IndexingDependencyMappingContextImpl
		extends DelegatingPropertyMappingContext
		implements IndexingDependencyMappingContext,
		PojoMetadataContributor<PojoAdditionalMetadataCollectorPropertyNode, PojoMappingCollector> {

	private ContainerValueExtractorPath extractorPath = ContainerValueExtractorPath.defaultExtractors();
	private ReindexOnUpdate reindexOnUpdate = ReindexOnUpdate.DEFAULT;

	IndexingDependencyMappingContextImpl(PropertyMappingContext delegate) {
		super( delegate );
	}

	@Override
	public void contributeModel(PojoAdditionalMetadataCollectorPropertyNode collector) {
		if ( reindexOnUpdate != null ) {
			collector.value( extractorPath ).indexingDependency( reindexOnUpdate );
		}
	}

	@Override
	public void contributeMapping(PojoMappingCollector collector) {
		// Nothing to do
	}

	@Override
	public IndexingDependencyMappingContext reindexOnUpdate(ReindexOnUpdate reindexOnUpdate) {
		this.reindexOnUpdate = reindexOnUpdate;
		return this;
	}

	@Override
	public IndexingDependencyMappingContext withExtractors(ContainerValueExtractorPath extractorPath) {
		this.extractorPath = extractorPath;
		return this;
	}
}
