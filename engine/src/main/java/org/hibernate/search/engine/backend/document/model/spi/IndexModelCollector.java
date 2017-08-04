/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.model.spi;

import java.util.Optional;
import java.util.Set;

import org.hibernate.search.engine.backend.document.spi.IndexFieldReference;
import org.hibernate.search.engine.backend.document.spi.IndexObjectReference;
import org.hibernate.search.engine.backend.projection.spi.Projection;
import org.hibernate.search.engine.mapper.model.spi.IndexableReference;

/**
 * @author Yoann Rodiere
 */
public interface IndexModelCollector {

	FieldModelContext field(String relativeName);

	IndexModelCollector childObject(String relativeName);

	/**
	 * Add a projection that will re-create the object represented by this index model builder
	 * from the values of the given required fields.
	 */
	// TODO move projections setup to another class: this has nothing to do with the index itself, it's more about mapping
	// TODO mandate that document ID bridges use this method to register a projection
	void projection(Set<IndexFieldReference<?>> requiredFields, Projection projection);

	// TODO move projections setup to another class: this has nothing to do with the index itself, it's more about mapping
	/*
	 * TODO is it such a good idea to allow projections on non-existent fields?
	 *
	 * We used to allow that because bridges had a "default field" which could be abused
	 * so that we never actually populated the field in the index, but still allowed to project on it.
	 *
	 * Ideally, we'd want to only allow projections on objects, defining those projections with the method above.
	 */
	void projection(String relativeName, Set<IndexableReference<?>> requiredFields, Projection projection);

	IndexObjectReference asReference();

	<T extends IndexModelCollector> Optional<T> unwrap(Class<T> clazz);

}
