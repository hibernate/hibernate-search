package org.hibernate.search.backend.lucene.scope.model.impl;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexContext;

public interface LuceneScopeIndexManagerContext extends LuceneSearchIndexContext {

	LuceneIndexModel model();

}
