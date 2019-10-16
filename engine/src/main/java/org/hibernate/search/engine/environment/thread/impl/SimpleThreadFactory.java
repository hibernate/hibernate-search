/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.thread.impl;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

final class SimpleThreadFactory implements ThreadFactory {
	private final ThreadGroup group;
	private final String namePrefix;
	private final AtomicInteger threadNumber = new AtomicInteger( 1 );

	SimpleThreadFactory(ThreadGroup group, String namePrefix) {
		this.group = group;
		this.namePrefix = namePrefix;
	}

	@Override
	public Thread newThread(Runnable r) {
		return new Thread( group, r, namePrefix + threadNumber.getAndIncrement(), 0 );
	}
}
