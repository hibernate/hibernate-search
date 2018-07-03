/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.spi;


import java.util.StringJoiner;

/**
 * @author Yoann Rodiere
 */
public final class ImmutableBeanReference implements BeanReference {

	private final String name;

	private final Class<?> type;

	public ImmutableBeanReference(String name) {
		this( name, null );
	}

	public ImmutableBeanReference(Class<?> type) {
		this( null, type );
	}

	public ImmutableBeanReference(String name, Class<?> type) {
		this.name = name;
		this.type = type;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append( "<" );
		StringJoiner joiner = new StringJoiner( ", " );
		if ( name != null ) {
			// Add this even if name is empty
			joiner.add( "name=" + name );
		}
		if ( type != null ) {
			joiner.add( "type=" + type );
		}
		builder.append( joiner );
		builder.append( ">" );
		return builder.toString();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Class<?> getType() {
		return type;
	}

}
