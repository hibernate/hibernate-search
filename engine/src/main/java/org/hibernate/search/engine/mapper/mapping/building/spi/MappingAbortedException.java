/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.spi;

import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;

/**
 * An exception thrown by {@link Mapper#prepareBuild()} when detecting that failures were
 * {@link ContextualFailureCollector#add(Throwable) collected}
 * and deciding to abort early to avoid a snowball effect creating too many failures,
 * which would make the failure report unclear.
 */
public class MappingAbortedException extends Exception {
}
