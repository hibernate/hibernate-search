/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.codec.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import org.apache.lucene.codecs.Codec;
import org.apache.lucene.codecs.lucene95.Lucene95Codec;

class HibernateSearchLuceneCodecTest {
	@Test
	void checkDefaultCodec() {
		assertThat( Codec.getDefault() )
				.isNotNull()
				.extracting( Object::getClass )
				.isEqualTo( HibernateSearchLuceneCodec.DEFAULT_CODEC.getClass() );
	}
}
