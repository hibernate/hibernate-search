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

	public static StubMappedIndex withoutFields() {
		return ofAdvancedNonRetrievable( ignored -> { } );
	}

	public static StubMappedIndex ofNonRetrievable(Consumer<? super IndexSchemaElement> binder) {
		return ofAdvancedNonRetrievable( ctx -> binder.accept( ctx.getSchemaElement() ) );
	}

	public static StubMappedIndex ofAdvancedNonRetrievable(Consumer<? super IndexedEntityBindingContext> binder) {
		return new StubMappedIndex() {
			@Override
			protected void bind(IndexedEntityBindingContext context) {
				binder.accept( context );
			}
		};
	}

	private String indexName;
	private String typeName;
	private String backendName;
	private MappedIndexManager manager;

	public StubMappedIndex() {
		this.indexName = "indexName";
		this.typeName = null;
	}

	@Override
	public final String name() {
		return indexName;
	}

	public StubMappedIndex name(String name) {
		this.indexName = name;
		return this;
	}

	public final String typeName() {
		return typeName != null ? typeName : indexName + "Type";
	}

	public StubMappedIndex typeName(String name) {
		this.typeName = name;
		return this;
	}

	public final Optional<String> backendName() {
		return Optional.ofNullable( backendName );
	}

	public StubMappedIndex backendName(String name) {
		this.backendName = name;
		return this;
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
