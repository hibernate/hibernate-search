/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import org.hibernate.search.backend.elasticsearch.search.extraction.impl.DocumentReferenceExtractorHelper;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.ObjectSearchProjectionBuilder;


public class ObjectSearchProjectionBuilderImpl implements ObjectSearchProjectionBuilder {

	private final ObjectSearchProjectionImpl projection;

	public ObjectSearchProjectionBuilderImpl(DocumentReferenceExtractorHelper helper) {
		this.projection = new ObjectSearchProjectionImpl( helper );
	}

	@Override
	public SearchProjection<Object> build() {
		return projection;
	}
}
