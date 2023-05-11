/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
