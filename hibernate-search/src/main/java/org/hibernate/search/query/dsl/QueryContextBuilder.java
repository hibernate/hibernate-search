package org.hibernate.search.query.dsl;

/**
 * Query builder that needs contextualization:
 * A query builder should know which entity or analyzer it relies on.
 *
 * <code>
 * QueryBuilder builder =
 * searchFactory.buildQueryBuilder()
 *   .forEntity(Customer.class)
 *     .overridesForField("profession", "acronym-analyzer")
 *     .get();
 * </code>
 *
 * overridesForField is optional (and usually not needed). This method overrides the
 * underlying analyzer (for a given field) used to build queries.
 *
 * @author Emmanuel Bernard
 */
public interface QueryContextBuilder {
	//TODO make a forEntities
	EntityContext forEntity(Class<?> entityType);
}
