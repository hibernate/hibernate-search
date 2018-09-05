/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

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

	/**
	 * Creates an entity context which can be used to obtain a {@link QueryBuilder}.
	 * <p>
	 * Note that the passed entity type is used to verify field names, transparently apply analyzers and field bridges
	 * etc. The query result list, however, is not automatically restricted to the given type. Instead a type filter
	 * must be applied when creating the full text query in order to restrict the query result to certain entity types.
	 *
	 * @param entityType entity type used for meta data retrieval during query creation
	 * @return an entity context
	 */
	EntityContext forEntity(Class<?> entityType);
}
