/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.query.dsl.impl;

/**
 * @author Emmanuel Bernard
 */
public class RangeQueryContext {
	//RANGE
	private Object from;
	private Object to;
	private boolean excludeFrom;
	private boolean excludeTo;

	public Object getFrom() {
		return from;
	}

	public void setFrom(Object from) {
		this.from = from;
	}

	public Object getTo() {
		return to;
	}

	public void setTo(Object to) {
		this.to = to;
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
