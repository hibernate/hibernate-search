/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.stub;

import java.util.Optional;
import java.util.function.Consumer;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingContext;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager;

// TODO This extends StubMappingIndexManager for backward compatibility; ideally we'll move everything to this class, eventually.
public abstract class StubMappedIndex extends StubMappingIndexManager {

	public static StubMappedIndex withoutFields(String indexName) {
		return withoutRetrievableBinding( indexName, ignored -> { } );
	}

	public static StubMappedIndex withoutRetrievableBinding(String indexName, Consumer<? super IndexSchemaElement> binder) {
		return new StubMappedIndex( indexName ) {
			@Override
			protected void bind(IndexedEntityBindingContext context) {
				binder.accept( context.getSchemaElement() );
			}
		};
	}

	private final String indexName;
	private MappedIndexManager manager;

	public StubMappedIndex(String indexName) {
		this.indexName = indexName;
	}

	@Override
	public final String name() {
		return indexName;
	}

	public String typeName() {
		// Use the index name for the type name by default.
		// Tests are easier to write if we can just have one constant for the index name.
		return name();
	}

	public Optional<String> backendName() {
		return Optional.empty();
	}

	public IndexManager toApi() {
		return manager.toAPI();
	}

	protected abstract void bind(IndexedEntityBindingContext context);

	protected void onIndexManagerCreated(MappedIndexManager manager) {
		this.manager = manager;
	}

	@Override
	protected MappedIndexManager delegate() {
		return manager;
	}
}
