package org.hibernate.search.backend.lucene.document.model.dsl.impl;

import java.util.Map;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexCompositeNode;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexField;

public interface LuceneIndexNodeContributor {

	void contribute(LuceneIndexNodeCollector collector, LuceneIndexCompositeNode parentNode,
			Map<String, LuceneIndexField> staticChildrenByNameForParent);

}
