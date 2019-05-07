/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.search.dsl.query;

import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultDefinitionContext;
import org.hibernate.search.mapper.pojo.search.PojoReference;

public interface JavaBeanSearchQueryResultDefinitionContext<O>
		extends SearchQueryResultDefinitionContext<PojoReference, O, SearchProjectionFactoryContext<PojoReference, O>> {

}
