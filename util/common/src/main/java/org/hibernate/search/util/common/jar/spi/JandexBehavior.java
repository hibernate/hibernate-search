/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common.jar.spi;

public final class JandexBehavior {

	private JandexBehavior() {
	}

	// Exposed for override in native images, to make it extra-clear to SubstrateVM
	// that the native executable will never use Jandex.
	public static void doWithJandex(JandexOperation operation) {
		operation.execute();
	}

	public interface JandexOperation {
		void execute();
	}
}
