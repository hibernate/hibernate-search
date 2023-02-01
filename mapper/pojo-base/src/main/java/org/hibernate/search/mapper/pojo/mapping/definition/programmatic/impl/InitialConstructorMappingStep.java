/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import java.util.Optional;

import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoSearchMappingConstructorNode;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ConstructorMappingStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.mapper.pojo.model.spi.PojoConstructorModel;

public class InitialConstructorMappingStep
		implements ConstructorMappingStep, PojoSearchMappingConstructorNode {

	private final TypeMappingStepImpl parent;
	private final PojoConstructorModel<?> constructorModel;

	private boolean projectionConstructor = false;

	InitialConstructorMappingStep(TypeMappingStepImpl parent, PojoConstructorModel<?> constructorModel) {
		this.parent = parent;
		this.constructorModel = constructorModel;
	}

	@Override
	public TypeMappingStep hostingType() {
		return parent;
	}

	@Override
	public Class<?>[] parametersJavaTypes() {
		return constructorModel.parametersJavaTypes();
	}

	@Override
	public ConstructorMappingStep projectionConstructor() {
		this.projectionConstructor = true;
		return this;
	}

	@Override
	public boolean isProjectionConstructor() {
		return projectionConstructor;
	}
}
