/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.search.projection;

import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.EntityReferenceProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;

//tag::include[]
@ProjectionConstructor // <1>
public record MyBookEntityRefAndTitleProjection(
		@EntityReferenceProjection // <2>
		EntityReference ref, // <3>
		String title // <4>
) {
}
//end::include[]
