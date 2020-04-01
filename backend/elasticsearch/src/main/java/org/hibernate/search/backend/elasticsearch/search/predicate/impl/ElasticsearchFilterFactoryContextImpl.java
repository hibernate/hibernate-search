/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import java.util.Collection;
import java.util.Map;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.MetadataFields;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.factories.FilterFactoryContext;

/**
 *
 * @author Waldemar KÃÂÃÂaczyÃÂÃÂski
 */
public class ElasticsearchFilterFactoryContextImpl implements FilterFactoryContext {

	private final SearchPredicateFactory predicate;
	private final String nestedPath;
	private final Map<String, Object> params;
	private final String parentPath;
	private final String absolutePath;

	public ElasticsearchFilterFactoryContextImpl(SearchPredicateFactory predicate,
		String parentPath, String nestedPath, String absolutePath,
		Map<String, Object> params) {
		this.predicate = predicate;
		this.nestedPath = nestedPath;
		this.parentPath = parentPath;
		this.absolutePath = absolutePath;
		this.params = params;
	}

	@Override
	public SearchPredicateFactory predicate() {
		return predicate;
	}

	@Override
	public <T> T param(String name) {
		return (T) params.get( name );
	}

	@Override
	public Collection<String> getParamNames() {
		return params.keySet();
	}

	@Override
	public String getParentPath() {
		return parentPath;
	}

	@Override
	public String resolvePath(String relativeFieldName) {
		return MetadataFields.compose( parentPath, relativeFieldName );
	}

	@Override
	public String getAbsolutePath() {
		return absolutePath;
	}

	@Override
	public String getNestedPath() {
		return nestedPath;
	}

}
