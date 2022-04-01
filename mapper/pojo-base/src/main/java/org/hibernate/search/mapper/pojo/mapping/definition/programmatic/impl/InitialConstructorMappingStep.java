/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import org.hibernate.search.mapper.pojo.mapping.building.spi.ErrorCollectingPojoConstructorMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoSearchMappingCollectorTypeNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ConstructorMappingStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.mapper.pojo.model.spi.PojoConstructorModel;

public class InitialConstructorMappingStep implements ConstructorMappingStep, PojoTypeMetadataContributor {

	private final TypeMappingStepImpl parent;
	private final PojoConstructorModel<?> constructorModel;

	private final ErrorCollectingPojoConstructorMetadataContributor children =
			new ErrorCollectingPojoConstructorMetadataContributor();

	InitialConstructorMappingStep(TypeMappingStepImpl parent, PojoConstructorModel<?> constructorModel) {
		this.parent = parent;
		this.constructorModel = constructorModel;
	}

	@Override
	public TypeMappingStep hostingType() {
		return parent;
	}

	@Override
	public void contributeSearchMapping(PojoSearchMappingCollectorTypeNode collector) {
		// Constructor mapping is not inherited
		if ( !constructorModel.typeModel().typeIdentifier().equals( collector.typeIdentifier() ) ) {
			return;
		}
		if ( children.hasContent() ) {
			children.contributeSearchMapping( collector.constructor( constructorModel.parametersJavaTypes() ) );
		}
	}

	@Override
	public ConstructorMappingStep projectionConstructor() {
		children.add( new ProjectionConstructorMappingContributor() );
		return this;
	}
}
