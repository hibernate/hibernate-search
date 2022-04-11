/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoPropertyMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.IndexingDependencyOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorValueNode;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;



class IndexingDependencyOptionsStepImpl
		extends DelegatingPropertyMappingStep
		implements IndexingDependencyOptionsStep, PojoPropertyMetadataContributor {

	private ContainerExtractorPath extractorPath = ContainerExtractorPath.defaultExtractors();
	private ReindexOnUpdate reindexOnUpdate = ReindexOnUpdate.DEFAULT;
	// Use a LinkedHashSet for deterministic iteration
	private Set<PojoModelPathValueNode> derivedFrom = null;

	IndexingDependencyOptionsStepImpl(PropertyMappingStep delegate) {
		super( delegate );
	}

	@Override
	public void contributeAdditionalMetadata(PojoAdditionalMetadataCollectorPropertyNode collector) {
		PojoAdditionalMetadataCollectorValueNode collectorValueNode = collector.value( extractorPath );
		if ( reindexOnUpdate != null ) {
			collectorValueNode.reindexOnUpdate( reindexOnUpdate );
		}
		if ( derivedFrom != null ) {
			collectorValueNode.derivedFrom( derivedFrom );
		}
	}

	@Override
	public IndexingDependencyOptionsStep reindexOnUpdate(ReindexOnUpdate reindexOnUpdate) {
		this.reindexOnUpdate = reindexOnUpdate;
		return this;
	}

	@Override
	public IndexingDependencyOptionsStep derivedFrom(PojoModelPathValueNode pojoModelPath) {
		if ( derivedFrom == null ) {
			// Use a LinkedHashSet for deterministic iteration
			derivedFrom = new LinkedHashSet<>();
		}
		derivedFrom.add( pojoModelPath );
		return this;
	}

	@Override
	public IndexingDependencyOptionsStep extractors(ContainerExtractorPath extractorPath) {
		this.extractorPath = extractorPath;
		return this;
	}
}
