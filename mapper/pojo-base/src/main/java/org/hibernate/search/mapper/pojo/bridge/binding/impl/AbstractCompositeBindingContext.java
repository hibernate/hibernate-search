/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.binding.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.dependency.impl.AbstractPojoBridgedElementDependencyContext;
import org.hibernate.search.mapper.pojo.model.impl.AbstractPojoModelCompositeElement;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

abstract class AbstractCompositeBindingContext extends AbstractBindingContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	AbstractCompositeBindingContext(BeanResolver beanResolver) {
		super( beanResolver );
	}

	static void checkBridgeDependencies(AbstractPojoModelCompositeElement<?> pojoModelRootElement,
			AbstractPojoBridgedElementDependencyContext pojoDependencyContext) {
		boolean isUseRootOnly = pojoDependencyContext.isUseRootOnly();
		boolean hasDependency = pojoModelRootElement.hasDependency()
				|| pojoDependencyContext.hasNonRootDependency();
		boolean hasNonRootDependency = pojoModelRootElement.hasNonRootDependency()
				|| pojoDependencyContext.hasNonRootDependency();
		if ( isUseRootOnly && hasNonRootDependency ) {
			throw log.inconsistentBridgeDependencyDeclaration();
		}
		else if ( !isUseRootOnly && !hasDependency ) {
			throw log.missingBridgeDependencyDeclaration();
		}
	}
}
