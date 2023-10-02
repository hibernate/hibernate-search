/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.environment.thread.spi;

import java.util.concurrent.ThreadFactory;

/**
 * The thread provider, used to customize thread groups and names.
 */
public interface ThreadProvider {

	String createThreadName(String prefix, int threadNumber);

	ThreadFactory createThreadFactory(String prefix);

}
