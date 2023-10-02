/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.projectionbinder.param.annotation;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;

//tag::include[]
@ProjectionConstructor
public record MyBookProjection(
		@MyFieldProjectionBinding(fieldName = "title") // <1>
		String title) {
}
//end::include[]

