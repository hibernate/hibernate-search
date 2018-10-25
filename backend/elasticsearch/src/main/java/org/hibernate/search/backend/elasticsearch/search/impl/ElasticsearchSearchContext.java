/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.impl;

import org.hibernate.search.engine.backend.document.converter.runtime.ToIndexFieldValueConvertContext;
import org.hibernate.search.engine.backend.document.converter.runtime.spi.ToIndexFieldValueConvertContextImpl;
import org.hibernate.search.engine.mapper.mapping.context.spi.MappingContextImplementor;

public final class ElasticsearchSearchContext {

	private final ToIndexFieldValueConvertContext toIndexFieldValueConvertContext;

	public ElasticsearchSearchContext(MappingContextImplementor mappingContext) {
		this.toIndexFieldValueConvertContext = new ToIndexFieldValueConvertContextImpl( mappingContext );
	}

	public ToIndexFieldValueConvertContext getToIndexFieldValueConvertContext() {
		return toIndexFieldValueConvertContext;
	}
}
