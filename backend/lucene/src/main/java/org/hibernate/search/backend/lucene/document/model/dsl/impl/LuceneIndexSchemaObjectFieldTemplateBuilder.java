/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.dsl.impl;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexObjectFieldTemplate;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexCompositeNode;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexCompositeNodeType;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.util.common.pattern.spi.SimpleGlobPattern;

class LuceneIndexSchemaObjectFieldTemplateBuilder
		extends AbstractLuceneIndexSchemaFieldTemplateBuilder<
						LuceneIndexSchemaObjectFieldTemplateBuilder, LuceneIndexObjectFieldTemplate
				> {

	private final LuceneIndexCompositeNodeType.Builder typeBuilder;

	LuceneIndexSchemaObjectFieldTemplateBuilder(AbstractLuceneIndexSchemaObjectNodeBuilder parent,
			String templateName, IndexFieldInclusion inclusion, ObjectStructure structure, String prefix) {
		super( parent, templateName, inclusion, prefix );
		this.typeBuilder = new LuceneIndexCompositeNodeType.Builder( structure );
	}

	@Override
	protected LuceneIndexSchemaObjectFieldTemplateBuilder thisAsS() {
		return this;
	}

	@Override
	protected void doContribute(LuceneIndexSchemaNodeCollector collector,
			LuceneIndexCompositeNode parentNode, SimpleGlobPattern absolutePathGlob, boolean multiValued) {
		LuceneIndexObjectFieldTemplate fieldTemplate = new LuceneIndexObjectFieldTemplate(
				parentNode, absolutePathGlob, typeBuilder.build(), inclusion, multiValued );

		collector.collect( fieldTemplate );
	}

}
