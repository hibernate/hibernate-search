/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.model.spi;

/**
 * @author Yoann Rodiere
 */
public interface IndexSchemaCollector extends IndexSchemaElement {

	@Override
	IndexSchemaCollector childObject(String relativeName);

	IndexSchemaElement withContext(IndexSchemaNestingContext context);

	/**
	 * Inform the model collector that documents will always be provided along
	 * with an explicit routing key,
	 * to be used to route the document to a specific shard.
	 */
	void explicitRouting();

}
