/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engineperformance.elasticsearch.datasets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.hibernate.search.engineperformance.elasticsearch.model.BookEntity;

public final class TextSampleDataset implements Dataset {

	private final List<TextSample> samples;

	private final int size;

	public TextSampleDataset(Collection<TextSample> samples) {
		super();
		this.samples = new ArrayList<>( samples );
		this.size = this.samples.size();
	}

	@Override
	public BookEntity create(int id) {
		/*
		 * Choose samples randomly, so that updates actually change
		 * indexed data most of the time.
		 */
		int sampleIndex = ThreadLocalRandom.current().nextInt( 0, size );
		TextSample sample = samples.get( sampleIndex );
		BookEntity entity = new BookEntity();
		entity.setId( (long) id );
		entity.setTitle( sample.title );
		entity.setText( sample.text );
		entity.setRating( DatasetUtils.intToFloat( id ) );
		return entity;
	}

	public static class TextSample {

		private final String title;
		private final String text;

		public TextSample(String title, String text) {
			super();
			this.title = title;
			this.text = text;
		}

		@Override
		public String toString() {
			return new StringBuilder()
					.append( getClass().getSimpleName() )
					.append( "<" ).append( title ).append( ">" )
					.toString();
		}
	}
}
