/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.search.projection;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentReferenceProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;

//tag::include[]
@ProjectionConstructor // <1>
public record MyBookDocRefAndTitleProjection(
		@DocumentReferenceProjection // <2>
		DocumentReference ref, // <3>
		String title // <4>
) {
}
//end::include[]
