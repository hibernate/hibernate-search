/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.environment.thread.impl;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

final class SimpleThreadFactory implements ThreadFactory {
	private final ThreadGroup group;
	private final String namePrefix;
	private final AtomicInteger threadNumber = new AtomicInteger( 0 );

	SimpleThreadFactory(ThreadGroup group, String namePrefix) {
		this.group = group;
		this.namePrefix = namePrefix;
	}

	@Override
	public Thread newThread(Runnable r) {
		return new Thread( group, r, namePrefix + threadNumber.getAndIncrement(), 0 );
	}
}
