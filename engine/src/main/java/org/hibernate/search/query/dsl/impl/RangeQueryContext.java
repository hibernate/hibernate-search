/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

/**
 * @author Emmanuel Bernard
 */
public class RangeQueryContext {
	//RANGE
	private Object from;
	private boolean hasFrom;
	private Object to;
	private boolean hasTo;
	private boolean excludeFrom;
	private boolean excludeTo;

	public Object getFrom() {
		return from;
	}

	public void setFrom(Object from) {
		this.from = from;
		this.hasFrom = true;
	}

	public boolean hasFrom() {
		return hasFrom;
	}

	public Object getTo() {
		return to;
	}

	public void setTo(Object to) {
		this.to = to;
		this.hasTo = true;
	}

	public boolean hasTo() {
		return hasTo;
	}

	public boolean isExcludeFrom() {
		return excludeFrom;
	}

	public void setExcludeFrom(boolean excludeFrom) {
		this.excludeFrom = excludeFrom;
	}

	public boolean isExcludeTo() {
		return excludeTo;
	}

	public void setExcludeTo(boolean excludeTo) {
		this.excludeTo = excludeTo;
	}
}
