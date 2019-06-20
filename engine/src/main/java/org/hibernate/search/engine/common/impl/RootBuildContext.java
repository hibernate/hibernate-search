/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.classpath.spi.ClassResolver;
import org.hibernate.search.engine.environment.classpath.spi.ResourceResolver;
import org.hibernate.search.engine.reporting.spi.FailureCollector;

class RootBuildContext {

	private final ClassResolver classResolver;
	private final ResourceResolver resourceResolver;
	private final BeanResolver beanResolver;

	private final FailureCollector failureCollector;

	RootBuildContext(ClassResolver classResolver, ResourceResolver resourceResolver,
			BeanResolver beanResolver, FailureCollector failureCollector) {
		this.classResolver = classResolver;
		this.resourceResolver = resourceResolver;
		this.beanResolver = beanResolver;
		this.failureCollector = failureCollector;
	}

	ClassResolver getClassResolver() {
		return classResolver;
	}

	ResourceResolver getResourceResolver() {
		return resourceResolver;
	}

	BeanResolver getBeanResolver() {
		return beanResolver;
	}

	FailureCollector getFailureCollector() {
		return failureCollector;
	}
}
