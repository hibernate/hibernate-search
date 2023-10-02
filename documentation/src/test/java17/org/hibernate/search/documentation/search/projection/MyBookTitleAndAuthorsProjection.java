/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.search.projection;

import java.util.List;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;

//tag::include[]
@ProjectionConstructor // <1>
public record MyBookTitleAndAuthorsProjection(
		@ObjectProjection // <2>
		List<MyAuthorProjection> authors, // <3>
		@ObjectProjection(path = "mainAuthor") // <4>
		MyAuthorProjection theMainAuthor, // <5>
		String title // <6>
) {
}
//end::include[]
