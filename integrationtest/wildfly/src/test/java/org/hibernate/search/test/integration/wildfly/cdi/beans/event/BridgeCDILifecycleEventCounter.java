/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.wildfly.cdi.beans.event;

public interface BridgeCDILifecycleEventCounter {
	void onFieldBridgeConstruct();

	void onFieldBridgeDestroy();

	void onClassBridgeConstruct();

	void onClassBridgeDestroy();

	static BridgeCDILifecycleEventCounter noOp() {
		return new BridgeCDILifecycleEventCounter() {
			@Override
			public void onFieldBridgeConstruct() {
			}

			@Override
			public void onFieldBridgeDestroy() {
			}

			@Override
			public void onClassBridgeConstruct() {
			}

			@Override
			public void onClassBridgeDestroy() {
			}
		};
	}
}
