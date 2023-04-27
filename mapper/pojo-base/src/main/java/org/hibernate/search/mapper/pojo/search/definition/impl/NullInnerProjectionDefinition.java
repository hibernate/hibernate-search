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

public final class NullInnerProjectionDefinition
		implements InnerProjectionDefinition {
	public static final NullInnerProjectionDefinition INSTANCE = new NullInnerProjectionDefinition();

	private NullInnerProjectionDefinition() {
	}

	@Override
	public String toString() {
		return "NullInnerProjectionDefinition";
	}

	@Override
	public void appendTo(ToStringTreeAppender appender) {
		appender.value( "<null projection>" );
	}

	@Override
	public SearchProjection<?> create(SearchProjectionFactory<?, ?> f) {
		return f.constant( null ).toProjection();
	}
}
