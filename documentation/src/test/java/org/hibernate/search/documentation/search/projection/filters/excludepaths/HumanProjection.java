/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.search.projection.filters.excludepaths;

import java.util.List;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FieldProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;

// @formatter:off
// tag::include[]
@ProjectionConstructor
public record HumanProjection(
		@FieldProjection
		String name,
		@FieldProjection
		String nickname,
		@ObjectProjection(excludePaths = { "parents.nickname", "parents.parents" })
		List<HumanProjection> parents
) {
}
// end::include[]
// @formatter:on
