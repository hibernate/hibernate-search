/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.bean;


import java.util.StringJoiner;

/**
 * @author Yoann Rodiere
 */
final class ImmutableBeanReference implements BeanReference {

	private final Class<?> type;
	private final String name;

	ImmutableBeanReference(Class<?> type, String name) {
		this.type = type;
		this.name = name;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append( "<" );
		StringJoiner joiner = new StringJoiner( ", " );
		if ( type != null ) {
			joiner.add( "type=" + type );
		}
		if ( name != null ) {
			// Add this even if name is empty
			joiner.add( "name=" + name );
		}
		builder.append( joiner );
		builder.append( ">" );
		return builder.toString();
	}

	@Override
	public Class<?> getType() {
		return type;
	}

	@Override
	public String getName() {
		return name;
	}

}
