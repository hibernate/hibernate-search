/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.analyzer.definition.impl;

import org.hibernate.search.elasticsearch.settings.impl.model.TokenFilterDefinition;
import org.hibernate.search.util.StringHelper;

/**
 * @author Yoann Rodiere
 */
public class ElasticsearchTokenFilterDefinitionContextImpl
		extends ElasticsearchAnalysisComponentDefinitionContextImpl<TokenFilterDefinition> {

	protected ElasticsearchTokenFilterDefinitionContextImpl(String name) {
		super( name, new TokenFilterDefinition() );
	}

	@Override
	public void populate(ElasticsearchAnalysisDefinitionRegistry registry) {
		if ( StringHelper.isEmpty( definition.getType() ) ) {
			throw LOG.invalidElasticsearchTokenFilterDefinition( name );
		}
		registry.register( name, definition );
	}

}
