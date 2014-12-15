package org.hibernate.search.backend.impl.lucene.works;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.hibernate.search.backend.DeleteByQueryLuceneWork;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.SerializableQuery;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.store.Workspace;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Stateless implementation that performs an <code>DeleteByQueryWork</code>.
 *
 * @author Martin Braun
 * @see LuceneWorkVisitor
 * @see LuceneWorkDelegate
 */
class DeleteByQueryWorkDelegate implements LuceneWorkDelegate {

	private static final Log log = LoggerFactory.make();
	protected final Workspace workspace;

	DeleteByQueryWorkDelegate(Workspace workspace) {
		this.workspace = workspace;
	}

	@Override
	public void performWork(LuceneWork work, IndexWriter writer,
			IndexingMonitor monitor) {
		DeleteByQueryLuceneWork deleteWork = (DeleteByQueryLuceneWork) work;

		final Class<?> entityType = work.getEntityClass();
		final SerializableQuery query = deleteWork.getQuery();

		log.tracef("Removing all %s matching Query: %s", entityType.toString(),
				query.toString());

		BooleanQuery entityDeletionQuery = new BooleanQuery();

		{
			ToLuceneQuery converter = DeleteByQuerySupport.TO_LUCENE_QUERY_CONVERTER
					.get(query.getQueryKey());
			Query queryToDelete = converter.build(query);

			entityDeletionQuery.add(queryToDelete, BooleanClause.Occur.MUST);
		}

		Term classNameQueryTerm = new Term(ProjectionConstants.OBJECT_CLASS,
				entityType.getName());
		TermQuery classNameQuery = new TermQuery(classNameQueryTerm);
		entityDeletionQuery.add(classNameQuery, BooleanClause.Occur.MUST);

		try {
			writer.deleteDocuments(entityDeletionQuery);
		} catch (Exception e) {
			String message = String.format(
					"unable to remove all %s matching Query: %s",
					entityType.toString(), query.toString());
			throw new SearchException(message, e);
		}
		workspace.notifyWorkApplied(work);

	}

}
