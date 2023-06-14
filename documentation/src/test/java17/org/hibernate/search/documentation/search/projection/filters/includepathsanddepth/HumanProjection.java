/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.search.projection.filters.includepathsanddepth;

import java.util.List;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FieldProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;

// tag::include[]
@ProjectionConstructor
public record HumanProjection(
		@FieldProjection
		String name,
		@FieldProjection
		String nickname,
		@ObjectProjection(includeDepth = 2, includePaths = { "parents.parents.name" })
		List<HumanProjection> parents
) {
}
// end::include[]
