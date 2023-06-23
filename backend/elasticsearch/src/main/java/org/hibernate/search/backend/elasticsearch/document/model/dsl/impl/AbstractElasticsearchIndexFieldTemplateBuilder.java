/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.dsl.impl;

import java.util.Map;

import org.hibernate.search.backend.elasticsearch.document.model.impl.AbstractElasticsearchIndexFieldTemplate;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexCompositeNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexField;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.AbstractTypeMapping;
import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldTemplateOptionsStep;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaBuildContext;
import org.hibernate.search.engine.common.tree.spi.TreeNodeInclusion;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.pattern.spi.SimpleGlobPattern;
import org.hibernate.search.util.common.reporting.EventContext;

abstract class AbstractElasticsearchIndexFieldTemplateBuilder<
		S extends AbstractElasticsearchIndexFieldTemplateBuilder<S, T>,
		T extends AbstractElasticsearchIndexFieldTemplate<?>>
		implements IndexSchemaFieldTemplateOptionsStep<S>,
		ElasticsearchIndexNodeContributor, IndexSchemaBuildContext {

	private final AbstractElasticsearchIndexCompositeNodeBuilder parent;
	protected final String absolutePath;
	private final TreeNodeInclusion inclusion;
	private final String prefix;

	private SimpleGlobPattern relativePathGlob;
	private boolean multiValued = false;

	AbstractElasticsearchIndexFieldTemplateBuilder(AbstractElasticsearchIndexCompositeNodeBuilder parent,
			String templateName, TreeNodeInclusion inclusion, String prefix) {
		this.parent = parent;
		this.absolutePath = FieldPaths.compose( parent.getAbsolutePath(), templateName );
		this.inclusion = inclusion;
		this.prefix = prefix;
	}

	@Override
	public EventContext eventContext() {
		return parent.getRootNodeBuilder().getIndexEventContext()
				.append( EventContexts.fromIndexFieldAbsolutePath( parent.getAbsolutePath() ) )
				.append( EventContexts.fromFieldTemplateAbsolutePath( absolutePath ) );
	}

	@Override
	public S matchingPathGlob(String pathGlob) {
		relativePathGlob = SimpleGlobPattern.compile( pathGlob );
		return thisAsS();
	}

	@Override
	public S multiValued() {
		this.multiValued = true;
		return thisAsS();
	}

	@Override
	public void contribute(ElasticsearchIndexNodeCollector collector,
			ElasticsearchIndexCompositeNode parentNode,
			Map<String, ElasticsearchIndexField> staticChildrenByNameForParent,
			AbstractTypeMapping parentMapping) {
		SimpleGlobPattern absolutePathGlob = FieldPaths.absolutize(
				parent.getAbsolutePath(),
				prefix,
				relativePathGlob != null ? relativePathGlob : SimpleGlobPattern.compile( "*" )
		);
		doContribute( collector, parentNode, inclusion, absolutePathGlob, multiValued );
	}

	protected abstract S thisAsS();

	protected abstract void doContribute(ElasticsearchIndexNodeCollector collector,
			ElasticsearchIndexCompositeNode parentNode,
			TreeNodeInclusion inclusion,
			SimpleGlobPattern absolutePathGlob,
			boolean multiValued);

}
