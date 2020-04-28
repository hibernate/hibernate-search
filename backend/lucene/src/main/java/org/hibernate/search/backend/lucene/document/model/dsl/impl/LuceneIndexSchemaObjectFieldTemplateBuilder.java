/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.dsl.impl;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeCollector;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectFieldTemplate;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.util.common.pattern.spi.SimpleGlobPattern;

class LuceneIndexSchemaObjectFieldTemplateBuilder
		extends AbstractLuceneIndexSchemaFieldTemplateBuilder<
						LuceneIndexSchemaObjectFieldTemplateBuilder, LuceneIndexSchemaObjectFieldTemplate
				> {

	private final ObjectFieldStorage storage;

	LuceneIndexSchemaObjectFieldTemplateBuilder(AbstractLuceneIndexSchemaObjectNodeBuilder parent,
			String templateName, ObjectFieldStorage storage, String prefix) {
		super( parent, templateName, prefix );
		this.storage = storage;
	}

	@Override
	protected LuceneIndexSchemaObjectFieldTemplateBuilder thisAsS() {
		return this;
	}

	@Override
	protected void doContribute(LuceneIndexSchemaNodeCollector collector,
			LuceneIndexSchemaObjectNode parentNode, SimpleGlobPattern absolutePathGlob, boolean multiValued) {
		LuceneIndexSchemaObjectFieldTemplate fieldTemplate = new LuceneIndexSchemaObjectFieldTemplate(
				absolutePathGlob, multiValued, storage
		);

		collector.collect( fieldTemplate );
	}

}
