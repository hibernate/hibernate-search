/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.testsupport.util;

import org.hibernate.search.engine.environment.bean.BeanResolver;

/**
 * Used for partial mocks, to benefit from default methods and mock the rest.
 * <p>
 * We have to base partial mocks on an abstract class,
 * otherwise we get an exception with message "Partial mocking doesn't make sense for interface"
 */
public abstract class AbstractBeanResolverPartialMock implements BeanResolver {
}
