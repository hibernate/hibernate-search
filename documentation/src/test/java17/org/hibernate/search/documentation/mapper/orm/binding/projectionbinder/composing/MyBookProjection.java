/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.binding.projectionbinder.composing;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionBinding;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;
import org.hibernate.search.mapper.pojo.search.definition.mapping.annotation.ProjectionBinderRef;

//tag::include[]
@ProjectionConstructor
public record MyBookProjection(
		@ProjectionBinding(binder = @ProjectionBinderRef( // <1>
				type = MyObjectFieldProjectionBinder.class
		))
		MyAuthorProjection author) {

	@ProjectionConstructor // <2>
	public record MyAuthorProjection(String name) {
	}
}
//end::include[]

