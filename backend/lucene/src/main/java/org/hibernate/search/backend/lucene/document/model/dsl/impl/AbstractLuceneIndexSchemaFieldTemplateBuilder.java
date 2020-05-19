/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.dsl.impl;

import java.util.List;

import org.hibernate.search.backend.lucene.document.model.impl.AbstractLuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.document.model.impl.AbstractLuceneIndexSchemaFieldTemplate;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeCollector;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeContributor;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldTemplateOptionsStep;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaBuildContext;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.pattern.spi.SimpleGlobPattern;
import org.hibernate.search.util.common.reporting.EventContext;

abstract class AbstractLuceneIndexSchemaFieldTemplateBuilder<
				S extends AbstractLuceneIndexSchemaFieldTemplateBuilder<S, T>,
				T extends AbstractLuceneIndexSchemaFieldTemplate<?>
		>
		implements IndexSchemaFieldTemplateOptionsStep<S>,
				LuceneIndexSchemaNodeContributor, IndexSchemaBuildContext {

	private final AbstractLuceneIndexSchemaObjectNodeBuilder parent;
	protected final String absolutePath;
	protected final IndexFieldInclusion inclusion;
	private final String prefix;

	private SimpleGlobPattern relativePathGlob;
	private boolean multiValued = false;

	AbstractLuceneIndexSchemaFieldTemplateBuilder(AbstractLuceneIndexSchemaObjectNodeBuilder parent,
			String templateName, IndexFieldInclusion inclusion, String prefix) {
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
	public void contribute(LuceneIndexSchemaNodeCollector collector, LuceneIndexSchemaObjectNode parentNode,
			List<AbstractLuceneIndexSchemaFieldNode> staticChildrenForParent) {
		SimpleGlobPattern absolutePathGlob = FieldPaths.absolutize(
				parent.getAbsolutePath(),
				prefix,
				relativePathGlob != null ? relativePathGlob : SimpleGlobPattern.compile( "*" )
		);
		doContribute( collector, parentNode, absolutePathGlob, multiValued );
	}

	protected abstract S thisAsS();

	protected abstract void doContribute(LuceneIndexSchemaNodeCollector collector,
			LuceneIndexSchemaObjectNode parentNode,
			SimpleGlobPattern absolutePathGlob,
			boolean multiValued);

}
