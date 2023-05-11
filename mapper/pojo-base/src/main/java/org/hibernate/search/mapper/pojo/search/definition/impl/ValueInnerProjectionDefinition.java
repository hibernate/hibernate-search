/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.search.definition.impl;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.util.common.spi.ToStringTreeAppender;

final class ValueInnerProjectionDefinition implements InnerProjectionDefinition {
	final String path;
	final boolean multi;
	final Class<?> type;

	ValueInnerProjectionDefinition(String path, boolean multi, Class<?> type) {
		this.path = path;
		this.multi = multi;
		this.type = type;
	}

	@Override
	public String toString() {
		return "ValueInnerProjectionDefinition["
				+ "path='" + path + '\''
				+ ", multi=" + multi
				+ ']';
	}

	@Override
	public void appendTo(ToStringTreeAppender appender) {
		appender.attribute( "path", path )
				.attribute( "multi", multi )
				.attribute( "type", type );
	}

	@Override
	public SearchProjection<?> create(SearchProjectionFactory<?, ?> f) {
		if ( multi ) {
			return f.field( path, type ).multi().toProjection();
		}
		else {
			return f.field( path, type ).toProjection();
		}
	}
}
