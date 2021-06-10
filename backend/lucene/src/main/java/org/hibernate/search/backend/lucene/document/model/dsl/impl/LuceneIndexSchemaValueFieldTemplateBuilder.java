/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.dsl.impl;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaValueFieldTemplate;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexCompositeNode;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexValueFieldType;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.util.common.pattern.spi.SimpleGlobPattern;

class LuceneIndexSchemaValueFieldTemplateBuilder
		extends AbstractLuceneIndexSchemaFieldTemplateBuilder<
						LuceneIndexSchemaValueFieldTemplateBuilder, LuceneIndexSchemaValueFieldTemplate
				> {

	private final LuceneIndexValueFieldType<?> type;

	LuceneIndexSchemaValueFieldTemplateBuilder(AbstractLuceneIndexSchemaObjectNodeBuilder parent,
			String templateName, IndexFieldInclusion inclusion, LuceneIndexValueFieldType<?> type, String prefix) {
		super( parent, templateName, inclusion, prefix );
		this.type = type;
	}

	@Override
	protected LuceneIndexSchemaValueFieldTemplateBuilder thisAsS() {
		return this;
	}

	@Override
	protected void doContribute(LuceneIndexSchemaNodeCollector collector,
			LuceneIndexCompositeNode parentNode, SimpleGlobPattern absolutePathGlob, boolean multiValued) {
		LuceneIndexSchemaValueFieldTemplate fieldTemplate = new LuceneIndexSchemaValueFieldTemplate(
				parentNode, inclusion, absolutePathGlob, multiValued, type
		);

		collector.collect( fieldTemplate );
	}

}
