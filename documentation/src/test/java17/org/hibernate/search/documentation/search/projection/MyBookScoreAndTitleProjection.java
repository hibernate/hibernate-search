/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.search.projection;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ScoreProjection;

//tag::include[]
@ProjectionConstructor // <1>
public record MyBookScoreAndTitleProjection(
		@ScoreProjection // <2>
		float score, // <3>
		String title // <4>
) {
}
//end::include[]
