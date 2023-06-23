/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.dsl.impl;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexCompositeNode;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexValueFieldTemplate;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexValueFieldType;
import org.hibernate.search.engine.common.tree.spi.TreeNodeInclusion;
import org.hibernate.search.util.common.pattern.spi.SimpleGlobPattern;

class LuceneIndexValueFieldTemplateBuilder
		extends AbstractLuceneIndexFieldTemplateBuilder<
				LuceneIndexValueFieldTemplateBuilder,
				LuceneIndexValueFieldTemplate> {

	private final LuceneIndexValueFieldType<?> type;

	LuceneIndexValueFieldTemplateBuilder(AbstractLuceneIndexCompositeNodeBuilder parent,
			String templateName, TreeNodeInclusion inclusion, LuceneIndexValueFieldType<?> type, String prefix) {
		super( parent, templateName, inclusion, prefix );
		this.type = type;
	}

	@Override
	protected LuceneIndexValueFieldTemplateBuilder thisAsS() {
		return this;
	}

	@Override
	protected void doContribute(LuceneIndexNodeCollector collector,
			LuceneIndexCompositeNode parentNode, SimpleGlobPattern absolutePathGlob, boolean multiValued) {
		LuceneIndexValueFieldTemplate fieldTemplate = new LuceneIndexValueFieldTemplate(
				parentNode, absolutePathGlob, type, inclusion, multiValued
		);

		collector.collect( fieldTemplate );
	}

}
