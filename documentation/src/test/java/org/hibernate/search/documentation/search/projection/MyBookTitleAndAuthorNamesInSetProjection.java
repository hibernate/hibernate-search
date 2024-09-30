/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.search.projection;

import java.util.Set;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FieldProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;

// @formatter:off
//tag::include[]
@ProjectionConstructor // <1>
public record MyBookTitleAndAuthorNamesInSetProjection(
		@FieldProjection // <2>
		String title, // <3>
		@FieldProjection(path = "authors.lastName") // <4>
		Set<String> authorLastNames // <5>
) {
}
//end::include[]
// @formatter:on
