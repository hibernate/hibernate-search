/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engineperformance.elasticsearch.datasets;

import org.hibernate.search.engineperformance.elasticsearch.model.BookEntity;

public class ConstantTextDataset implements Dataset {

	@Override
	public BookEntity create(int id) {
		BookEntity book = new BookEntity();
		book.setId( (long) id );
		book.setText( "Some very long text should be stored here. No, I mean long as in a book." );
		book.setTitle( "Naaa" );
		book.setRating( DatasetUtils.intToFloat( id ) );
		return book;
	}

}
