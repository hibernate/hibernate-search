package org.hibernate.search.query.dsl;

/**
 * Abstract class that can be used to store state and any information that all the various TermQueryBuilder
 * types might need.
 *
 * @author Navin Surtani
 */


public abstract class AbstractTermQueryBuilder {

   protected TermQueryBuilderDataStore dataStore;
}
