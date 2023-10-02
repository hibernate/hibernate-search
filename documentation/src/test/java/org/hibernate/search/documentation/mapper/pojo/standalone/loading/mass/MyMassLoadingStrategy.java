/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.pojo.standalone.loading.mass;

import java.util.Collection;
import java.util.List;

import org.hibernate.search.documentation.mapper.pojo.standalone.loading.mydatastore.MyDatastore;
import org.hibernate.search.documentation.mapper.pojo.standalone.loading.mydatastore.MyDatastoreConnection;
import org.hibernate.search.documentation.mapper.pojo.standalone.loading.mydatastore.MyDatastoreCursor;
import org.hibernate.search.mapper.pojo.standalone.loading.LoadingTypeGroup;
import org.hibernate.search.mapper.pojo.standalone.loading.MassEntityLoader;
import org.hibernate.search.mapper.pojo.standalone.loading.MassEntitySink;
import org.hibernate.search.mapper.pojo.standalone.loading.MassIdentifierLoader;
import org.hibernate.search.mapper.pojo.standalone.loading.MassIdentifierSink;
import org.hibernate.search.mapper.pojo.standalone.loading.MassLoadingOptions;
import org.hibernate.search.mapper.pojo.standalone.loading.MassLoadingStrategy;

// tag::include[]
public class MyMassLoadingStrategy<E>
		implements MassLoadingStrategy<E, String> {

	private final MyDatastore datastore; // <1>
	private final Class<E> rootEntityType;

	public MyMassLoadingStrategy(MyDatastore datastore, Class<E> rootEntityType) {
		this.datastore = datastore;
		this.rootEntityType = rootEntityType;
	}

	@Override
	public MassIdentifierLoader createIdentifierLoader(
			LoadingTypeGroup<E> includedTypes, // <2>
			MassIdentifierSink<String> sink, MassLoadingOptions options) {
		int batchSize = options.batchSize(); // <3>
		Collection<Class<? extends E>> typeFilter =
				includedTypes.includedTypesMap().values(); // <4>
		return new MassIdentifierLoader() {
			private final MyDatastoreConnection connection =
					datastore.connect(); // <5>
			private final MyDatastoreCursor<String> identifierCursor =
					connection.scrollIdentifiers( typeFilter );

			@Override
			public void close() {
				connection.close(); // <5>
			}

			@Override
			public long totalCount() { // <6>
				return connection.countEntities( typeFilter );
			}

			@Override
			public void loadNext() throws InterruptedException {
				List<String> batch = identifierCursor.next( batchSize );
				if ( batch != null ) {
					sink.accept( batch ); // <7>
				}
				else {
					sink.complete(); // <8>
				}
			}
		};
	}

	@Override
	public MassEntityLoader<String> createEntityLoader(
			LoadingTypeGroup<E> includedTypes, // <9>
			MassEntitySink<E> sink, MassLoadingOptions options) {
		return new MassEntityLoader<String>() {
			private final MyDatastoreConnection connection =
					datastore.connect(); // <10>

			@Override
			public void close() { // <8>
				connection.close();
			}

			@Override
			public void load(List<String> identifiers)
					throws InterruptedException {
				sink.accept( // <11>
						connection.loadEntitiesById( rootEntityType, identifiers )
				);
			}
		};
	}
}
// end::include[]
