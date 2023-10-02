/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.search.projection;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DistanceProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;

//tag::include[]
@ProjectionConstructor // <1>
public record MyAuthorPlaceProjection(
		@DistanceProjection( // <2>
				fromParam = "point-param", // <3>
				path = "placeOfBirth") // <4>
		Double distance ) { // <5>
}
//end::include[]
