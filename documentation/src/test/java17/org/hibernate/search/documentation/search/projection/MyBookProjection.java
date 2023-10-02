/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.search.projection;

import java.util.List;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IdProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;

//tag::include[]
@ProjectionConstructor // <1>
public record MyBookProjection(
		@IdProjection Integer id, // <2>
		String title, // <3>
		List<MyBookProjection.Author> authors) { // <4>
	@ProjectionConstructor // <5>
	public record Author(String firstName, String lastName) {
	}
}
//end::include[]
