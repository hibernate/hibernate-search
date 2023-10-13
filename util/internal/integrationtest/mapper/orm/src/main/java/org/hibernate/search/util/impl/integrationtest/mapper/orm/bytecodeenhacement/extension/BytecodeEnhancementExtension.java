/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm.bytecodeenhacement.extension;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstanceFactoryContext;
import org.junit.jupiter.api.extension.TestInstancePreConstructCallback;
import org.junit.jupiter.api.extension.TestInstancePreDestroyCallback;

public class BytecodeEnhancementExtension implements TestInstancePreConstructCallback, TestInstancePreDestroyCallback {

	private ClassLoader originalClassLoader;

	@Override
	public void preConstructTestInstance(TestInstanceFactoryContext testInstanceFactoryContext,
			ExtensionContext extensionContext) {
		originalClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader( testInstanceFactoryContext.getTestClass().getClassLoader() );
	}

	@Override
	public void preDestroyTestInstance(ExtensionContext extensionContext) {
		Thread.currentThread().setContextClassLoader( originalClassLoader );
	}


}
