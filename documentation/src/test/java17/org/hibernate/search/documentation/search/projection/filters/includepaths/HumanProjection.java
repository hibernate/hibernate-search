/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.search.projection.filters.includepaths;

import java.util.List;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FieldProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;

// tag::include[]
@ProjectionConstructor
public record HumanProjection(
		@FieldProjection
		String name,
		@FieldProjection
		String nickname,
		@ObjectProjection(includePaths = { "name", "nickname", "parents.name" })
		List<HumanProjection> parents
) {
}
// end::include[]
