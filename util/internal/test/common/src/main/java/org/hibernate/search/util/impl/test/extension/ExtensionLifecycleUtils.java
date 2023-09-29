/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.test.extension;

import java.util.Optional;

import org.hibernate.search.util.impl.test.extension.parameterized.ParameterizedSetup;

import org.junit.jupiter.api.extension.ExtensionContext;

public final class ExtensionLifecycleUtils {

	private ExtensionLifecycleUtils() {
	}

	public static boolean isAll(ExtensionContext context, boolean fallback) {
		Optional<ParameterizedSetup.Lifecycle> lifecycle = ParameterizedSetup.Lifecycle.getCurrentLifecycle( context );
		return lifecycle.map( ParameterizedSetup.Lifecycle.PER_CONFIGURATION::equals ).orElse( fallback );
	}

	public static boolean isEach(ExtensionContext context, boolean fallback) {
		Optional<ParameterizedSetup.Lifecycle> lifecycle = ParameterizedSetup.Lifecycle.getCurrentLifecycle( context );
		return lifecycle.map( ParameterizedSetup.Lifecycle.PER_METHOD::equals ).orElse( fallback );
	}

}
