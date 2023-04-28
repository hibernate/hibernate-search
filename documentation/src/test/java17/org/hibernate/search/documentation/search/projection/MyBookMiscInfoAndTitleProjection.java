/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
