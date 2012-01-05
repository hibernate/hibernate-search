/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.search.engine.spi;

import org.apache.lucene.search.Collector;
import org.apache.lucene.search.TimeLimitingCollector;
import org.apache.lucene.util.Counter;

/**
 * Lucene delegates responsibility for efficient time tracking to an external service;
 * this is needed for example by the {@link TimeLimitingCollector#TimeLimitingCollector(Collector, Counter, long)}
 * used by Hibernate Search when time limits are enabled on fulltext queries.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 * @since 4.1
 */
public interface TimingSource {

	/**
	 * Returns and approximation of {@link System#nanoTime()}.
	 * Performance should be preferred over accuracy by the implementation, but the value is monotonic
	 * and expresses time in milliseconds, however, subsequent invocations could return the same value.
	 *
	 * @return an increasing value related to time in milliseconds. Only meaningful to compare time intervals, with no guarantees of high precision.
	 */
	long getMonotonicTimeEstimate();

	/**
	 * Invoked on SearchFactory shutdown. There is no start method as it's expected to be lazily initialized
	 */
	void stop();

	/**
	 * Needs to be invoked at least once before {@link #getMonotonicTimeEstimate()} can be used.
	 * Safe to be invoked multiple times.
	 */
	void ensureInitialized();

}
