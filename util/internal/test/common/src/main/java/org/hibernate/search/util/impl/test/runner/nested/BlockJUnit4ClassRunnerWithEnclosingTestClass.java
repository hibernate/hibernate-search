/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.test.runner.nested;

import java.util.List;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

final class BlockJUnit4ClassRunnerWithEnclosingTestClass extends BlockJUnit4ClassRunner {
	private final Object enclosingTestInstance;

	public BlockJUnit4ClassRunnerWithEnclosingTestClass(Class<?> testClass, Object enclosingTestInstance)
			throws InitializationError {
		super( testClass );
		this.enclosingTestInstance = enclosingTestInstance;
	}

	@Override
	protected Object createTest() throws Exception {
		return getTestClass().getOnlyConstructor().newInstance( enclosingTestInstance );
	}

	@Override
	protected void validateNoNonStaticInnerClass(List<Throwable> errors) {
		// Do nothing: we *expect* the test class to be a non-static nested class.
	}
}
