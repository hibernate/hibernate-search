/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.dsl.impl;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaFieldTemplate;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeCollector;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexFieldType;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.util.common.pattern.spi.SimpleGlobPattern;

class LuceneIndexSchemaFieldTemplateBuilder
		extends AbstractLuceneIndexSchemaFieldTemplateBuilder<
						LuceneIndexSchemaFieldTemplateBuilder, LuceneIndexSchemaFieldTemplate
				> {

	private final LuceneIndexFieldType<?> type;

	LuceneIndexSchemaFieldTemplateBuilder(AbstractLuceneIndexSchemaObjectNodeBuilder parent,
			String templateName, IndexFieldInclusion inclusion, LuceneIndexFieldType<?> type, String prefix) {
		super( parent, templateName, inclusion, prefix );
		this.type = type;
	}

	@Override
	protected LuceneIndexSchemaFieldTemplateBuilder thisAsS() {
		return this;
	}

	@Override
	protected void doContribute(LuceneIndexSchemaNodeCollector collector,
			LuceneIndexSchemaObjectNode parentNode, SimpleGlobPattern absolutePathGlob, boolean multiValued) {
		LuceneIndexSchemaFieldTemplate fieldTemplate = new LuceneIndexSchemaFieldTemplate(
				parentNode, inclusion, absolutePathGlob, multiValued, type
		);

		collector.collect( fieldTemplate );
	}

}
