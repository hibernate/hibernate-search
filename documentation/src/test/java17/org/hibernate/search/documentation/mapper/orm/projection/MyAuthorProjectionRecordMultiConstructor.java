/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
