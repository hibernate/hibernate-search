/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.mapper.mapping.building.spi;

import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.util.common.AssertionFailure;

/**
 * An exception thrown by {@link Mapper#prepareBuild()} when detecting that failures were
 * {@link ContextualFailureCollector#add(Throwable) collected}
 * and deciding to abort early to avoid a snowball effect creating too many failures,
 * which would make the failure report unclear.
 */
public class MappingAbortedException extends Exception {
	/**
	 * Collects the cause/suppressed of this exception, if any, in the failure collector,
	 * and throws an exception if there is nothing to explain the abortion
	 * (neither in the failure collector or in this exceptions' cause/suppressed).
	 * @param failureCollector A failure collector for the mapping that was just aborted.
	 */
	public void collectSilentlyAndCheck(ContextualFailureCollector failureCollector) {
		/*
		 * This generally shouldn't do anything, because we don't expect a cause nor suppressed exceptions
		 * in the MappingAbortedException, but ignoring exceptions can lead to
		 * spending some really annoying hours debugging.
		 * So let's be extra cautious not to lose these.
		 */
		Throwable cause = getCause();
		if ( cause != null ) {
			failureCollector.add( cause );
		}
		Throwable[] suppressed = getSuppressed();
		for ( Throwable throwable : suppressed ) {
			failureCollector.add( throwable );
		}

		if ( !failureCollector.hasFailure() ) {
			throw new AssertionFailure(
					"Caught " + MappingAbortedException.class.getSimpleName()
							+ ", but the mapper did not collect any failure.",
					this
			);
		}
	}
}
