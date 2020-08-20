/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.directoryProvider;

import java.util.TimerTask;

import org.hibernate.search.store.impl.FSSlaveDirectoryProvider;

/**
 * Extending FSSlaveDirectoryProvider to test it via static fields.
 *
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public class FSSlaveDirectoryProviderTestingExtension extends FSSlaveDirectoryProvider {

	public static volatile TimerTask taskScheduled = null;
	public static volatile Long taskScheduledPeriod = null;

	@Override
	protected void scheduleTask(TimerTask task, long period) {
		taskScheduled = task;
		taskScheduledPeriod = Long.valueOf( period );
	}

	void triggerTimerAction() {
		super.attemptInitializeAndStart();
	}

}
