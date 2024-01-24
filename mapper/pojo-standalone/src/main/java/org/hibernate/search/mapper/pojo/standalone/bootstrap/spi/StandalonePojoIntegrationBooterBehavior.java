/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.bootstrap.spi;

import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public final class StandalonePojoIntegrationBooterBehavior {

	private StandalonePojoIntegrationBooterBehavior() {
	}

	// Exposed for override in native images, to make it extra-clear to SubstrateVM
	// that the native executable will never perform the first phase of the boot.
	public static <T> T bootFirstPhase(BootPhase<T> phase) {
		return phase.execute();
	}

	public interface BootPhase<T> {
		T execute();
	}
}
