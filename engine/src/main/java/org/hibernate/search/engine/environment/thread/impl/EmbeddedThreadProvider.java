/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.thread.impl;

import java.util.concurrent.ThreadFactory;

import org.hibernate.search.engine.environment.thread.spi.ThreadProvider;

public final class EmbeddedThreadProvider implements ThreadProvider {

	public static final String NAME = "embedded";

	private final String commonThreadNamePrefix;

	public EmbeddedThreadProvider() {
		this( "Hibernate Search - " );
	}

	public EmbeddedThreadProvider(String commonThreadNamePrefix) {
		this.commonThreadNamePrefix = commonThreadNamePrefix;
	}

	@Override
	public String createThreadName(String prefix, int threadNumber) {
		return createFullThreadNamePrefix( prefix ) + threadNumber;
	}

	@Override
	public ThreadFactory createThreadFactory(String prefix) {
		ThreadGroup group = Thread.currentThread().getThreadGroup();
		String namePrefix = createFullThreadNamePrefix( prefix );
		return new SimpleThreadFactory( group, namePrefix );
	}

	private String createFullThreadNamePrefix(String prefix) {
		return commonThreadNamePrefix + prefix + " - ";
	}

}
