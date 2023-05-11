/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.search.definition.impl;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.definition.spi.CompositeProjectionDefinition;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.util.common.spi.ToStringTreeAppender;

final class ObjectInnerProjectionDefinition implements InnerProjectionDefinition {
	final String path;
	final boolean multi;
	final CompositeProjectionDefinition<?> composite;

	ObjectInnerProjectionDefinition(String path, boolean multi, CompositeProjectionDefinition<?> composite) {
		this.path = path;
		this.multi = multi;
		this.composite = composite;
	}

	@Override
	public String toString() {
		return "ObjectInnerProjectionDefinition["
				+ "path='" + path + '\''
				+ ", multi=" + multi
				+ ']';
	}

	@Override
	public void appendTo(ToStringTreeAppender appender) {
		appender.attribute( "path", path )
				.attribute( "multi", multi )
				.attribute( "composite", composite );
	}

	@Override
	public SearchProjection<?> create(SearchProjectionFactory<?, ?> f) {
		if ( multi ) {
			return composite.apply( f, f.object( path ) ).multi().toProjection();
		}
		else {
			return composite.apply( f, f.object( path ) ).toProjection();
		}
	}
}
