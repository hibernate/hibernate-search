/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.impl;

/**
 * @author Guillaume Smet
 */
// TODO: we'll need to populate this, once we add more options to this predicate
public class RangeQueryOptions {

	private boolean excludeLowerLimit = false;

	private boolean excludeUpperLimit = false;

	public RangeQueryOptions() {
	}

	public boolean isExcludeLowerLimit() {
		return excludeLowerLimit;
	}

	public void excludeLowerLimit() {
		excludeLowerLimit = true;
	}

	public boolean isExcludeUpperLimit() {
		return excludeUpperLimit;
	}

	public void excludeUpperLimit() {
		excludeUpperLimit = true;
	}
}
