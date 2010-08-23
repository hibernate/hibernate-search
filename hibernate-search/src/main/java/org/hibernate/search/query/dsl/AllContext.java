package org.hibernate.search.query.dsl;

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
