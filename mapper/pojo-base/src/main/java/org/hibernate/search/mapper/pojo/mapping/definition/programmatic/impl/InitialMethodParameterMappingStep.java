/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoSearchMappingMethodParameterNode;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.MethodParameterMappingStep;
import org.hibernate.search.mapper.pojo.model.spi.PojoMethodParameterModel;
import org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBinder;

class InitialMethodParameterMappingStep
		implements MethodParameterMappingStep, PojoSearchMappingMethodParameterNode {

	private final InitialConstructorMappingStep parent;
	private final PojoMethodParameterModel<?> parameterModel;

	private List<ProjectionBindingData> projectionDefinitions;

	InitialMethodParameterMappingStep(InitialConstructorMappingStep parent,
			PojoMethodParameterModel<?> parameterModel) {
		this.parent = parent;
		this.parameterModel = parameterModel;
	}

	@Override
	public MethodParameterMappingStep projection(BeanReference<? extends ProjectionBinder> binder,
			Map<String, Object> params) {
		if ( projectionDefinitions == null ) {
			projectionDefinitions = new ArrayList<>();
		}
		projectionDefinitions.add( new ProjectionBindingData( binder, params ) );
		return this;
	}

	@Override
	public List<ProjectionBindingData> projectionBindings() {
		return projectionDefinitions == null ? Collections.emptyList() : projectionDefinitions;
	}

}
