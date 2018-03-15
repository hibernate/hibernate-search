/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.search.engine.backend.document.model.ObjectFieldStorage;
import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractor;
import org.hibernate.search.mapper.pojo.extractor.impl.ContainerValueExtractorPath;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMappingCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoModelCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyIndexedEmbeddedMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingContext;


public class PropertyIndexedEmbeddedMappingContextImpl extends DelegatingPropertyMappingContext
		implements PropertyIndexedEmbeddedMappingContext,
		PojoMetadataContributor<PojoModelCollectorPropertyNode, PojoMappingCollectorPropertyNode> {

	private String prefix;

	private ObjectFieldStorage storage = ObjectFieldStorage.DEFAULT;

	private Integer maxDepth;

	private final Set<String> includePaths = new HashSet<>();

	private ContainerValueExtractorPath extractorPath = ContainerValueExtractorPath.defaultExtractors();

	PropertyIndexedEmbeddedMappingContextImpl(PropertyMappingContext parent) {
		super( parent );
	}

	@Override
	public void contributeModel(PojoModelCollectorPropertyNode collector) {
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
	public PropertyIndexedEmbeddedMappingContext prefix(String prefix) {
		this.prefix = prefix;
		return this;
	}

	@Override
	public PropertyIndexedEmbeddedMappingContext storage(ObjectFieldStorage storage) {
		this.storage = storage;
		return this;
	}

	@Override
	public PropertyIndexedEmbeddedMappingContext maxDepth(Integer depth) {
		this.maxDepth = depth;
		return this;
	}

	@Override
	public PropertyIndexedEmbeddedMappingContext includePaths(Collection<String> paths) {
		this.includePaths.addAll( paths );
		return this;
	}

	@Override
	public PropertyIndexedEmbeddedMappingContext withExtractors(
			List<? extends Class<? extends ContainerValueExtractor>> extractorClasses) {
		this.extractorPath = ContainerValueExtractorPath.explicitExtractors( extractorClasses );
		return this;
	}

	@Override
	public PropertyIndexedEmbeddedMappingContext withoutExtractors() {
		this.extractorPath = ContainerValueExtractorPath.noExtractors();
		return this;
	}
}
