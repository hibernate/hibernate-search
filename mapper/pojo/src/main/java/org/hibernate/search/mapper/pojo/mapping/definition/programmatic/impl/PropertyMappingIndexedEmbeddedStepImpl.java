/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMappingCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoPropertyMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingIndexedEmbeddedStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorPropertyNode;


class PropertyMappingIndexedEmbeddedStepImpl extends DelegatingPropertyMappingStep
		implements PropertyMappingIndexedEmbeddedStep, PojoPropertyMetadataContributor {

	private String prefix;

	private ObjectFieldStorage storage = ObjectFieldStorage.DEFAULT;

	private Integer maxDepth;

	private final Set<String> includePaths = new HashSet<>();

	private ContainerExtractorPath extractorPath = ContainerExtractorPath.defaultExtractors();

	PropertyMappingIndexedEmbeddedStepImpl(PropertyMappingStep parent) {
		super( parent );
	}

	@Override
	public void contributeAdditionalMetadata(PojoAdditionalMetadataCollectorPropertyNode collector) {
		// Nothing to do
	}

	@Override
	public void contributeMapping(PojoMappingCollectorPropertyNode collector) {
		collector.value( extractorPath ).indexedEmbedded(
				prefix, storage, maxDepth, includePaths
				/*
				 * Ignore mapped types, we don't need to discover new mappings automatically
				 * like in the annotation mappings.
				 */
		);
	}

	@Override
	public PropertyMappingIndexedEmbeddedStep prefix(String prefix) {
		this.prefix = prefix;
		return this;
	}

	@Override
	public PropertyMappingIndexedEmbeddedStep storage(ObjectFieldStorage storage) {
		this.storage = storage;
		return this;
	}

	@Override
	public PropertyMappingIndexedEmbeddedStep maxDepth(Integer depth) {
		this.maxDepth = depth;
		return this;
	}

	@Override
	public PropertyMappingIndexedEmbeddedStep includePaths(Collection<String> paths) {
		this.includePaths.addAll( paths );
		return this;
	}

	@Override
	public PropertyMappingIndexedEmbeddedStep extractors(ContainerExtractorPath extractorPath) {
		this.extractorPath = extractorPath;
		return this;
	}

}
