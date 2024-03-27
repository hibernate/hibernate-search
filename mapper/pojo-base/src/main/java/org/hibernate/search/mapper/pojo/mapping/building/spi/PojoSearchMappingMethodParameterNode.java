/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.building.spi;

import java.util.List;
import java.util.Map;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBinder;

public interface PojoSearchMappingMethodParameterNode {

	List<ProjectionBindingData> projectionBindings();

	final class ProjectionBindingData {
		public final BeanReference<? extends ProjectionBinder> reference;
		public final Map<String, Object> params;

		public ProjectionBindingData(BeanReference<? extends ProjectionBinder> reference,
				Map<String, Object> params) {
			this.reference = reference;
			this.params = params;
		}
	}

}
