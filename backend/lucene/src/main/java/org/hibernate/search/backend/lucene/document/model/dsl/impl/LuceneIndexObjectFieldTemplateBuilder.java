/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.dsl.impl;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexCompositeNode;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexObjectFieldTemplate;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexCompositeNodeType;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.common.tree.spi.TreeNodeInclusion;
import org.hibernate.search.util.common.pattern.spi.SimpleGlobPattern;

class LuceneIndexObjectFieldTemplateBuilder
		extends AbstractLuceneIndexFieldTemplateBuilder<
				LuceneIndexObjectFieldTemplateBuilder,
				LuceneIndexObjectFieldTemplate> {

	private final LuceneIndexCompositeNodeType.Builder typeBuilder;

	LuceneIndexObjectFieldTemplateBuilder(AbstractLuceneIndexCompositeNodeBuilder parent,
			String templateName, TreeNodeInclusion inclusion, ObjectStructure structure, String prefix) {
		super( parent, templateName, inclusion, prefix );
		this.typeBuilder = new LuceneIndexCompositeNodeType.Builder( structure );
	}

	@Override
	protected LuceneIndexObjectFieldTemplateBuilder thisAsS() {
		return this;
	}

	@Override
	protected void doContribute(LuceneIndexNodeCollector collector,
			LuceneIndexCompositeNode parentNode, SimpleGlobPattern absolutePathGlob, boolean multiValued) {
		LuceneIndexObjectFieldTemplate fieldTemplate = new LuceneIndexObjectFieldTemplate(
				parentNode, absolutePathGlob, typeBuilder.build(), inclusion, multiValued );

		collector.collect( fieldTemplate );
	}

}
