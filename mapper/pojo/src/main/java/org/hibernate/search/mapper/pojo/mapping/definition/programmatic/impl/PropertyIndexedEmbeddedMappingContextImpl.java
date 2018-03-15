/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.search.engine.backend.document.model.ObjectFieldStorage;
import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractor;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMappingCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoModelCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMappingCollectorValueNode;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyIndexedEmbeddedMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingContext;


/**
 * @author Yoann Rodiere
 */
public class PropertyIndexedEmbeddedMappingContextImpl extends DelegatingPropertyMappingContext
		implements PropertyIndexedEmbeddedMappingContext,
		PojoMetadataContributor<PojoModelCollectorPropertyNode, PojoMappingCollectorPropertyNode> {

	private String prefix;

	private ObjectFieldStorage storage = ObjectFieldStorage.DEFAULT;

	private Integer maxDepth;

	private final Set<String> includePaths = new HashSet<>();

	private List<Class<? extends ContainerValueExtractor>> extractorClasses = null;

	public PropertyIndexedEmbeddedMappingContextImpl(PropertyMappingContext parent) {
		super( parent );
	}

	@Override
	public void contributeModel(PojoModelCollectorPropertyNode collector) {
		// Nothing to do
	}

	@Override
	public void contributeMapping(PojoMappingCollectorPropertyNode collector) {
		PojoMappingCollectorValueNode valueNodeMappingCollector;
		if ( extractorClasses == null ) {
			valueNodeMappingCollector = collector.valueWithDefaultExtractors();
		}
		else if ( extractorClasses.isEmpty() ) {
			valueNodeMappingCollector = collector.valueWithoutExtractors();
		}
		else {
			valueNodeMappingCollector = collector.valueWithExtractors( extractorClasses );
		}
		valueNodeMappingCollector.indexedEmbedded(
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
		this.extractorClasses = new ArrayList<>( extractorClasses );
		return this;
	}

	@Override
	public PropertyIndexedEmbeddedMappingContext withoutExtractors() {
		this.extractorClasses = Collections.emptyList();
		return this;
	}
}
