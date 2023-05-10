/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoSearchMappingMethodParameterNode;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.MethodParameterMappingStep;
import org.hibernate.search.mapper.pojo.model.spi.PojoMethodParameterModel;

class InitialMethodParameterMappingStep
		implements MethodParameterMappingStep, PojoSearchMappingMethodParameterNode {
	private final InitialConstructorMappingStep parent;
	private final PojoMethodParameterModel<?> parameterModel;

	InitialMethodParameterMappingStep(InitialConstructorMappingStep parent,
			PojoMethodParameterModel<?> parameterModel) {
		this.parent = parent;
		this.parameterModel = parameterModel;
	}
}
