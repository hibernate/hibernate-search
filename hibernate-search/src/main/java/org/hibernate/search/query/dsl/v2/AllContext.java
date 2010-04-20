package org.hibernate.search.query.dsl.v2;

import java.util.List;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.Query;

/**
 * @author Emmanuel Bernard
 */
public interface AllContext extends QueryCustomization<AllContext>, Termination<AllContext> {
	/**
	 * Exclude the documents matching these queries
	 */
	AllContext except(Query... queriesMatchingExcludedDocuments);
}
