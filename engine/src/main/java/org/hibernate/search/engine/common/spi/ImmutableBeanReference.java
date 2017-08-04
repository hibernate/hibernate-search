/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.spi;


/**
 * @author Yoann Rodiere
 */
public final class ImmutableBeanReference<T> implements BeanReference<T> {

	private final String name;

	private final Class<? extends T> type;

	public ImmutableBeanReference(String name) {
		this( name, null );
	}

	public ImmutableBeanReference(Class<? extends T> type) {
		this( null, type );
	}

	public ImmutableBeanReference(String name, Class<? extends T> type) {
		this.name = name;
		this.type = type;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Class<? extends T> getType() {
		return type;
	}

}
