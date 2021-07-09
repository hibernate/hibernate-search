/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import java.util.List;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.analysis.model.impl.ElasticsearchAnalysisDefinitionRegistry;
import org.hibernate.search.backend.elasticsearch.document.model.lowlevel.impl.LowLevelIndexMetadataBuilder;
import org.hibernate.search.backend.elasticsearch.index.layout.impl.IndexNames;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl.IndexSettings;
import org.hibernate.search.backend.elasticsearch.metamodel.ElasticsearchIndexDescriptor;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexContext;
import org.hibernate.search.engine.backend.document.model.spi.AbstractIndexModel;
import org.hibernate.search.engine.backend.document.model.spi.IndexIdentifier;

public class ElasticsearchIndexModel
		extends AbstractIndexModel<ElasticsearchIndexModel, ElasticsearchIndexRoot, ElasticsearchIndexField>
		implements ElasticsearchIndexDescriptor, ElasticsearchSearchIndexContext {

	private final IndexNames names;

	private final ElasticsearchAnalysisDefinitionRegistry analysisDefinitionRegistry;
	private final IndexSettings customIndexSettings;
	private final RootTypeMapping mapping;

	public ElasticsearchIndexModel(IndexNames names, String mappedTypeName,
			IndexIdentifier identifier,
			ElasticsearchIndexRoot rootNode, Map<String, ElasticsearchIndexField> staticFields,
			List<AbstractElasticsearchIndexFieldTemplate<?>> fieldTemplates,
			ElasticsearchAnalysisDefinitionRegistry analysisDefinitionRegistry, IndexSettings customIndexSettings,
			RootTypeMapping mapping) {
		super( names.hibernateSearchIndex(), mappedTypeName, identifier, rootNode, staticFields, fieldTemplates );
		this.names = names;
		this.analysisDefinitionRegistry = analysisDefinitionRegistry;
		this.customIndexSettings = customIndexSettings;
		this.mapping = mapping;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[names=" + names + ", mapping=" + mapping + "]";
	}

	@Override
	protected ElasticsearchIndexModel self() {
		return this;
	}

	@Override
	public IndexNames names() {
		return names;
	}

	@Override
	public int maxResultWindow() {
		return ( customIndexSettings == null || customIndexSettings.getMaxResultWindow() == null )
				? IndexSettings.MAX_RESULT_WINDOW_DEFAULT
				: customIndexSettings.getMaxResultWindow();
	}

	public void contributeLowLevelMetadata(LowLevelIndexMetadataBuilder builder) {
		builder.setAnalysisDefinitionRegistry( analysisDefinitionRegistry );
		builder.setCustomIndexSettings( customIndexSettings );
		builder.setMapping( mapping );
	}

	@Override
	public String readName() {
		return names.read().toString();
	}

	@Override
	public String writeName() {
		return names.write().toString();
	}
}
