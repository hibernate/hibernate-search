/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.projectionbinder.composing;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionBinding;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;
import org.hibernate.search.mapper.pojo.search.definition.mapping.annotation.ProjectionBinderRef;

//tag::include[]
@ProjectionConstructor
public record MyBookProjection(
		@ProjectionBinding(binder = @ProjectionBinderRef( // <1>
				type = MyObjectFieldProjectionBinder.class
		))
		MyAuthorProjection author) {

	@ProjectionConstructor // <2>
	public record MyAuthorProjection(String name) {
	}
}
//end::include[]

