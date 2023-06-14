/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.binding.projectionbinder.composing;

import org.hibernate.search.engine.common.tree.TreeFilterDefinition;
import org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBinder;
import org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBindingContext;

//tag::include[]
public class MyObjectFieldProjectionBinder implements ProjectionBinder {
	@Override
	public void bind(ProjectionBindingContext context) {
		var authorProjection = context.createObjectDefinition( // <1>
				"author", // <2>
				MyBookProjection.MyAuthorProjection.class, // <3>
				TreeFilterDefinition.includeAll() // <4>
		);
		context.definition( // <5>
				MyBookProjection.MyAuthorProjection.class,
				authorProjection
		);
	}
}
//end::include[]
