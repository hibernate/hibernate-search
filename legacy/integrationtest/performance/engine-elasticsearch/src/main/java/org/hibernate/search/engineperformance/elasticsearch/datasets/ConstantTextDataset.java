/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engineperformance.elasticsearch.datasets;

import java.util.function.Supplier;

import org.hibernate.search.engineperformance.elasticsearch.model.AbstractBookEntity;
import org.hibernate.search.spi.IndexedTypeIdentifier;

public class ConstantTextDataset<T extends AbstractBookEntity> implements Dataset<T> {

	private final Supplier<T> constructor;

	private final IndexedTypeIdentifier typeId;

	public ConstantTextDataset(Supplier<T> constructor, IndexedTypeIdentifier typeId) {
		super();
		this.constructor = constructor;
		this.typeId = typeId;
	}

	@Override
	public T create(int id) {
		T book = constructor.get();
		book.setId( (long) id );
		book.setText( "Some very long text should be stored here. No, I mean long as in a book." );
		book.setTitle( "Naaa" );
		book.setRating( DatasetUtils.intToFloat( id ) );
		return book;
	}

	@Override
	public IndexedTypeIdentifier getTypeId() {
		return typeId;
	}

}
