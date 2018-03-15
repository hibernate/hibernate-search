/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import java.util.List;

import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractor;
import org.hibernate.search.mapper.pojo.extractor.impl.ContainerValueExtractorPath;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.AssociationInverseSideMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingContext;
import org.hibernate.search.mapper.pojo.model.augmented.building.impl.PojoAugmentedModelCollectorPropertyNode;


/**
 * @author Yoann Rodiere
 */
public class AssociationInverseSideMappingContextImpl
		extends DelegatingPropertyMappingContext
		implements AssociationInverseSideMappingContext,
		PojoMetadataContributor<PojoAugmentedModelCollectorPropertyNode, PojoMappingCollector> {

	private final String inversePropertyName;
	private ContainerValueExtractorPath extractorPath = ContainerValueExtractorPath.defaultExtractors();
	private ContainerValueExtractorPath inverseExtractorPath = ContainerValueExtractorPath.defaultExtractors();

	AssociationInverseSideMappingContextImpl(PropertyMappingContext delegate, String inversePropertyName) {
		super( delegate );
		this.inversePropertyName = inversePropertyName;
	}

	@Override
	public void contributeModel(PojoAugmentedModelCollectorPropertyNode collector) {
		collector.value( extractorPath ).associationInverseSide( inversePropertyName, inverseExtractorPath );
	}

	@Override
	public void contributeMapping(PojoMappingCollector collector) {
		// Nothing to do
	}

	@Override
	public AssociationInverseSideMappingContext withExtractors(
			List<? extends Class<? extends ContainerValueExtractor>> extractorClasses) {
		this.extractorPath = ContainerValueExtractorPath.explicitExtractors( extractorClasses );
		return this;
	}

	@Override
	public AssociationInverseSideMappingContext withoutExtractors() {
		this.extractorPath = ContainerValueExtractorPath.noExtractors();
		return this;
	}

	@Override
	public AssociationInverseSideMappingContext withoutInverseExtractors() {
		this.inverseExtractorPath = ContainerValueExtractorPath.noExtractors();
		return this;
	}

	@Override
	public AssociationInverseSideMappingContext withInverseExtractors(
			List<? extends Class<? extends ContainerValueExtractor>> inverseExtractorClasses) {
		this.inverseExtractorPath = ContainerValueExtractorPath.explicitExtractors( inverseExtractorClasses );
		return this;
	}
}
