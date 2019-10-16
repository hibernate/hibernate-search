/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
