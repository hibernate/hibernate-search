/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.projection;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;

//tag::include[]
public record MyAuthorProjectionRecordMultiConstructor(String firstName, String lastName) {
	@ProjectionConstructor // <1>
	public MyAuthorProjectionRecordMultiConstructor { // <2>
	}

	public MyAuthorProjectionRecordMultiConstructor(String fullName) { // <3>
		this( fullName.split( " " )[0], fullName.split( " " )[1] );
	}
}
//end::include[]
