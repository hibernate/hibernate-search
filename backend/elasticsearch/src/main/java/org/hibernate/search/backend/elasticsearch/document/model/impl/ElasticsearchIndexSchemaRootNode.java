/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import java.util.Collections;
import java.util.List;

import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;


public class ElasticsearchIndexSchemaRootNode implements ElasticsearchIndexSchemaObjectNode {

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public String getAbsolutePath() {
		return null;
	}

	@Override
	public String getAbsolutePath(String relativeFieldName) {
		return relativeFieldName;
	}

	@Override
	public IndexFieldInclusion getInclusion() {
		return IndexFieldInclusion.INCLUDED;
	}

	@Override
	public List<String> getNestedPathHierarchy() {
		return Collections.emptyList();
	}

}
