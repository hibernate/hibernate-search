/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.common.tree.TreeFilterDefinition;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoIndexMappingCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoPropertyMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingIndexedEmbeddedStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.util.common.annotation.Search5DeprecatedAPI;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class PropertyMappingIndexedEmbeddedStepImpl extends DelegatingPropertyMappingStep
		implements PropertyMappingIndexedEmbeddedStep, PojoPropertyMetadataContributor {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoRawTypeIdentifier<?> definingType;

	private final String relativeFieldName;

	private String prefix;

	private ObjectStructure structure = ObjectStructure.DEFAULT;

	private Integer includeDepth;
	private final Set<String> includePaths = new HashSet<>();
	private final Set<String> excludePaths = new HashSet<>();
	private boolean includeEmbeddedObjectId = false;

	private Class<?> targetType;

	private ContainerExtractorPath extractorPath = ContainerExtractorPath.defaultExtractors();

	PropertyMappingIndexedEmbeddedStepImpl(PropertyMappingStep parent, PojoRawTypeIdentifier<?> definingType,
			String relativeFieldName) {
		super( parent );
		this.definingType = definingType;
		if ( relativeFieldName != null && relativeFieldName.contains( FieldPaths.PATH_SEPARATOR_STRING ) ) {
			throw log.invalidFieldNameDotNotAllowed( relativeFieldName );
		}
		this.relativeFieldName = relativeFieldName;
	}

	@Override
	public void contributeIndexMapping(PojoIndexMappingCollectorPropertyNode collector) {
		String actualPrefix;
		if ( relativeFieldName != null ) {
			actualPrefix = relativeFieldName + FieldPaths.PATH_SEPARATOR;
		}
		else {
			actualPrefix = prefix;
		}
		collector.value( extractorPath ).indexedEmbedded(
				definingType, actualPrefix, structure,
				new TreeFilterDefinition( includeDepth, includePaths, excludePaths ),
				includeEmbeddedObjectId, targetType
		);
	}

	@Override
	@Deprecated
	@Search5DeprecatedAPI
	public PropertyMappingIndexedEmbeddedStep prefix(String prefix) {
		if ( relativeFieldName != null && prefix != null ) {
			throw log.cannotSetBothIndexedEmbeddedNameAndPrefix( relativeFieldName, prefix );
		}
		this.prefix = prefix;
		return this;
	}

	@Override
	public PropertyMappingIndexedEmbeddedStep structure(ObjectStructure structure) {
		this.structure = structure;
		return this;
	}

	@Override
	public PropertyMappingIndexedEmbeddedStep includeDepth(Integer depth) {
		this.includeDepth = depth;
		return this;
	}

	@Override
	public PropertyMappingIndexedEmbeddedStep includePaths(Collection<String> paths) {
		this.includePaths.addAll( paths );
		return this;
	}

	@Override
	public PropertyMappingIndexedEmbeddedStep excludePaths(Collection<String> paths) {
		this.excludePaths.addAll( paths );
		return this;
	}

	@Override
	public PropertyMappingIndexedEmbeddedStep includeEmbeddedObjectId(boolean include) {
		this.includeEmbeddedObjectId = include;
		return this;
	}

	@Override
	public PropertyMappingIndexedEmbeddedStep extractors(ContainerExtractorPath extractorPath) {
		this.extractorPath = extractorPath;
		return this;
	}

	@Override
	public PropertyMappingIndexedEmbeddedStep targetType(Class<?> targetType) {
		this.targetType = targetType;
		return this;
	}
}
