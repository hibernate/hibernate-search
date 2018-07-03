/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.logging.spi;

import java.util.Objects;

/**
 * An abstract base for simple failure contexts based on a single parameter
 * passed to a rendering function.
 *
 * @param <T> The type of the parameter.
 */
public abstract class AbstractSimpleFailureContextElement<T> implements FailureContextElement {
	private final T param;

	protected AbstractSimpleFailureContextElement(T param) {
		this.param = param;
	}

	@Override
	public String toString() {
		return "SimpleFailureContextElement[" + render() + "]";
	}

	@Override
	public boolean equals(Object obj) {
		return obj != null && getClass().equals( obj.getClass() )
				&& Objects.equals( param, ( (AbstractSimpleFailureContextElement<?>) obj ).param );
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
