/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoSearchMappingConstructorNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoSearchMappingMethodParameterNode;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ConstructorMappingStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.MethodParameterMappingStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.mapper.pojo.model.spi.PojoConstructorModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoMethodParameterModel;

class InitialConstructorMappingStep
		implements ConstructorMappingStep, PojoSearchMappingConstructorNode {

	private final TypeMappingStepImpl parent;
	private final PojoConstructorModel<?> constructorModel;

	private boolean projectionConstructor = false;
	private Map<Integer, InitialMethodParameterMappingStep> parameters;

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

	@Override
	public MethodParameterMappingStep parameter(int index) {
		if ( parameters == null ) {
			parameters = new HashMap<>();
		}
		InitialMethodParameterMappingStep parameter = parameters.get( index );
		if ( parameter == null ) {
			PojoMethodParameterModel<?> parameterModel = constructorModel.parameter( index );
			parameter = new InitialMethodParameterMappingStep( this, parameterModel );
			parameters.put( index, parameter );
		}
		return parameter;
	}

	@Override
	public Optional<PojoSearchMappingMethodParameterNode> parameterNode(int index) {
		return Optional.ofNullable( parameters == null ? null : parameters.get( index ) );
	}
}
