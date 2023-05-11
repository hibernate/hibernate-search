/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.search.projection;

import java.util.List;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;

//tag::include[]
@ProjectionConstructor // <1>
public record MyBookTitleAndAuthorsProjection(
		@ObjectProjection // <2>
		List<MyAuthorProjection> authors, // <3>
		@ObjectProjection(path = "mainAuthor") // <4>
		MyAuthorProjection theMainAuthor, // <5>
		String title // <6>
) {
}
//end::include[]
