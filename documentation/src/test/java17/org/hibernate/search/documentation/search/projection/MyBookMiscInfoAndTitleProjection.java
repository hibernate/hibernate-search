/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.search.projection;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.CompositeProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;

//tag::include[]
@ProjectionConstructor // <1>
public record MyBookMiscInfoAndTitleProjection(
		@CompositeProjection // <2>
		MiscInfo miscInfo, // <3>
		String title // <4>
) {

	@ProjectionConstructor // <3>
	public record MiscInfo(
			Genre genre,
			Integer pageCount
	) {
	}
}
//end::include[]
