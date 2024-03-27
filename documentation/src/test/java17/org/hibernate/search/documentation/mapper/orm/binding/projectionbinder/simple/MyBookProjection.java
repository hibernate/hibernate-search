/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.projectionbinder.simple;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionBinding;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;
import org.hibernate.search.mapper.pojo.search.definition.mapping.annotation.ProjectionBinderRef;

//tag::include[]
@ProjectionConstructor
public record MyBookProjection(
		@ProjectionBinding(binder = @ProjectionBinderRef( // <1>
				type = MyFieldProjectionBinder.class
		))
		String title) {
}
//end::include[]

