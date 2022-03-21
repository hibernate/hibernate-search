/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.spi;

abstract class ObjectArrayProjectionCompositor<V>
		implements ProjectionCompositor<Object[], V> {

	private final int size;

	ObjectArrayProjectionCompositor(int size) {
		this.size = size;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + transformer() + "]";
	}

	protected abstract Object transformer();

	@Override
	public Object[] createInitial() {
		return new Object[size];
	}

	@Override
	public Object[] set(Object[] components, int index, Object value) {
		components[index] = value;
		return components;
	}

	@Override
	public Object get(Object[] components, int index) {
		return components[index];
	}

	@Override
	public abstract V finish(Object[] components);

}
