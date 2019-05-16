/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common.impl;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The thread factory, used to customize thread names
 */
public class SearchThreadFactory implements ThreadFactory {
	private static final String THREAD_GROUP_PREFIX = "Hibernate Search: ";

	private static String createNamePrefix(String groupName) {
		return THREAD_GROUP_PREFIX + groupName + " - ";
	}

	public static String createName(String groupName, int threadNumber) {
		return createNamePrefix( groupName ) + threadNumber;
	}

	final ThreadGroup group;
	final AtomicInteger threadNumber = new AtomicInteger( 1 );
	final String namePrefix;

	public SearchThreadFactory(String groupname) {
		SecurityManager s = System.getSecurityManager();
		group = ( s != null ) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
		namePrefix = createNamePrefix( groupname );
	}

	@Override
	public Thread newThread(Runnable r) {
		return new Thread( group, r, namePrefix + threadNumber.getAndIncrement(), 0 );
	}

}