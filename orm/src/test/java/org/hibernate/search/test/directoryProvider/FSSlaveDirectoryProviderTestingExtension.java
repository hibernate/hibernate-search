/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.hibernate.search.test.directoryProvider;

import java.util.TimerTask;

import org.hibernate.search.store.impl.FSSlaveDirectoryProvider;

/**
 * Extending FSSlaveDirectoryProvider to test it via static fields.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
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
