/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.test.extension.parameterized;

import java.lang.reflect.InvocationTargetException;

public interface ParameterizedTestMethodInvoker {

	String getName();

	void invoke(Object requiredTestInstance) throws InvocationTargetException, IllegalAccessException;
}
