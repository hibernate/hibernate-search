/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.test.powermock;

import org.junit.runner.RunWith;

import org.easymock.EasyMockSupport;
import org.powermock.api.easymock.PowerMock;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public abstract class PowerMockSupport extends EasyMockSupport {

	@Override
	public void resetAll() {
		PowerMock.resetAll();
		super.resetAll();
	}

	@Override
	public void replayAll() {
		PowerMock.replayAll();
		super.replayAll();
	}

	@Override
	public void verifyAll() {
		PowerMock.verifyAll();
		super.verifyAll();
	}

}
