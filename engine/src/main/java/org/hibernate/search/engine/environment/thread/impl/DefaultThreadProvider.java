/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.thread.impl;

import java.util.concurrent.ThreadFactory;

import org.hibernate.search.engine.environment.thread.spi.ThreadProvider;

public final class DefaultThreadProvider implements ThreadProvider {

	private final String commonThreadNamePrefix;

	public DefaultThreadProvider() {
		this( "Hibernate Search: " );
	}

	public DefaultThreadProvider(String commonThreadNamePrefix) {
		this.commonThreadNamePrefix = commonThreadNamePrefix;
	}

	@Override
	public String createThreadName(String prefix, int threadNumber) {
		return createFullThreadNamePrefix( prefix ) + threadNumber;
	}

	@Override
	public ThreadFactory createThreadFactory(String prefix) {
		SecurityManager s = System.getSecurityManager();
		ThreadGroup group = ( s != null ) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
		String namePrefix = createFullThreadNamePrefix( prefix );
		return new SimpleThreadFactory( group, namePrefix );
	}

	private String createFullThreadNamePrefix(String prefix) {
		return commonThreadNamePrefix + prefix + " - ";
	}

}