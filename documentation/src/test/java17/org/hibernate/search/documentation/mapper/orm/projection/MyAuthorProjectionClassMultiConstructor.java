/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.projection;

import java.util.Objects;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;

//tag::include[]
public class MyAuthorProjectionClassMultiConstructor {
	public final String firstName;
	public final String lastName;

	@ProjectionConstructor // <1>
	public MyAuthorProjectionClassMultiConstructor(String firstName, String lastName) {
		this.firstName = firstName;
		this.lastName = lastName;
	}

	public MyAuthorProjectionClassMultiConstructor(String fullName) { // <2>
		this( fullName.split( " " )[0], fullName.split( " " )[1] );
	}

	// ... Equals and hashcode ...

	//end::include[]
	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		MyAuthorProjectionClassMultiConstructor that = (MyAuthorProjectionClassMultiConstructor) o;
		return Objects.equals( firstName, that.firstName ) && Objects.equals( lastName, that.lastName );
	}

	@Override
	public int hashCode() {
		return Objects.hash( firstName, lastName );
	}

	//tag::include[]
}
//end::include[]
