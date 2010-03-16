package org.hibernate.search.query.dsl.v2;

import org.apache.lucene.analysis.Analyzer;

/**
 * @author Emmanuel Bernard
 */
public interface QueryContextBuilder {
	//TODO make a forEntities
	EntityContext forEntity(Class<?> entityType);
	interface EntityContext {
		EntityContext overridesForField(String field, String analyzerName);
		QueryBuilder get();
	}
}
