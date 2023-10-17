/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.VectorSimilarity;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoPropertyMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingVectorFieldStep;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class PropertyMappingVectorFieldStepImpl extends DelegatingPropertyMappingStep
		implements PropertyMappingVectorFieldStep, PojoPropertyMetadataContributor {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String relativeFieldName;
	private Projectable projectable;
	private VectorSimilarity vectorSimilarity;
	private Integer beamWidth;
	private Integer maxConnections;
	private String indexNullAs;


	PropertyMappingVectorFieldStepImpl(PropertyMappingStep parent, int dimension, String relativeFieldName) {
		super( parent );
	}

	@Override
	public PropertyMappingVectorFieldStep projectable(Projectable projectable) {
		this.projectable = projectable;
		return this;
	}

	@Override
	public PropertyMappingVectorFieldStep searchable(Searchable searchable) {
		return this;
	}

	@Override
	public PropertyMappingVectorFieldStep vectorSimilarity(VectorSimilarity vectorSimilarity) {
		this.vectorSimilarity = vectorSimilarity;
		return this;
	}

	@Override
	public PropertyMappingVectorFieldStep beamWidth(int beamWidth) {
		this.beamWidth = beamWidth;
		return this;
	}

	@Override
	public PropertyMappingVectorFieldStep maxConnections(int maxConnections) {
		this.maxConnections = maxConnections;
		return this;
	}

	@Override
	public PropertyMappingVectorFieldStep indexNullAs(String indexNullAs) {
		this.indexNullAs = indexNullAs;
		return this;
	}
}
