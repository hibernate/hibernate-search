/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.search.projection;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ScoreProjection;

//tag::include[]
@ProjectionConstructor // <1>
public record MyBookScoreAndTitleProjection(
		@ScoreProjection // <2>
		float score, // <3>
		String title // <4>
) {
}
//end::include[]
