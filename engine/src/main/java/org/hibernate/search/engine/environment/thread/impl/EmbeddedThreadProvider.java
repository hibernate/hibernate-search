/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
		return new SimpleThreadFactory(
				Thread.currentThread().getThreadGroup(),
				createFullThreadNamePrefix( prefix )
		);
	}

	private String createFullThreadNamePrefix(String prefix) {
		return commonThreadNamePrefix + prefix + " - ";
	}
}
