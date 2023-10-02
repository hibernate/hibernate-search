/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.projectionbinder.param.string;

import org.hibernate.search.mapper.pojo.common.annotation.Param;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionBinding;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;
import org.hibernate.search.mapper.pojo.search.definition.mapping.annotation.ProjectionBinderRef;

//tag::include[]
@ProjectionConstructor
public record MyBookProjection(
		@ProjectionBinding(binder = @ProjectionBinderRef(
				type = MyFieldProjectionBinder.class,
				params = @Param( name = "fieldName", value = "title" )
		)) String title) { // <1>
}
//end::include[]

