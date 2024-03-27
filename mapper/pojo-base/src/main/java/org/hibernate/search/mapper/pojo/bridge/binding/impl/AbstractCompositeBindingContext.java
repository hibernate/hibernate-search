/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.binding.impl;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.dependency.impl.AbstractPojoBridgedElementDependencyContext;
import org.hibernate.search.mapper.pojo.model.impl.AbstractPojoModelCompositeElement;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public abstract class AbstractCompositeBindingContext extends AbstractBindingContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected AbstractCompositeBindingContext(BeanResolver beanResolver, Map<String, Object> params) {
		super( beanResolver, params );
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
