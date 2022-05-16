/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.test.runner.nested;

import org.junit.runners.model.RunnerBuilder;

final class NestedRunnerWithEnclosingTestClass extends NestedRunner {
	private final Object enclosingTestInstance;

	public NestedRunnerWithEnclosingTestClass(Class<?> testClass, RunnerBuilder builder, Object enclosingTestInstance)
			throws Throwable {
		super( testClass, builder );
		this.enclosingTestInstance = enclosingTestInstance;
	}

	@Override
	protected Object createTest() throws Exception {
		return getTestClass().getOnlyConstructor().newInstance( enclosingTestInstance );
	}
}
