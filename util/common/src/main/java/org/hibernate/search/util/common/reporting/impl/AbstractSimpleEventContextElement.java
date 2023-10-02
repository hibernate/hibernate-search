/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.reporting.impl;

import java.util.Objects;

import org.hibernate.search.util.common.reporting.EventContextElement;

/**
 * An abstract base for simple event context elements based on a single parameter
 * passed to a rendering function.
 *
 * @param <T> The type of the parameter.
 */
public abstract class AbstractSimpleEventContextElement<T> implements EventContextElement {
	private final T param;

	protected AbstractSimpleEventContextElement(T param) {
		this.param = param;
	}

	@Override
	public String toString() {
		return "SimpleEventContextElement[" + render() + "]";
	}

	@Override
	public boolean equals(Object obj) {
		return obj != null
				&& getClass().equals( obj.getClass() )
				&& Objects.deepEquals( param, ( (AbstractSimpleEventContextElement<?>) obj ).param );
	}

	@Override
	public int hashCode() {
		return Objects.hashCode( param );
	}

	@Override
	public String render() {
		return render( param );
	}

	protected abstract String render(T param);
}
