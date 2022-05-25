/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.search.projection;

import java.util.List;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;

//tag::include[]
@ProjectionConstructor // <1>
public record MyBookProjection(String title, // <2>
		List<MyBookProjection.Author> authors) { // <3>
	@ProjectionConstructor // <4>
	public record Author(String firstName, String lastName) {
	}
}
//end::include[]
