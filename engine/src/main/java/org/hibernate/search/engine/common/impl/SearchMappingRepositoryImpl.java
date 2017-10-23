/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import java.util.Map;

import org.hibernate.search.engine.common.SearchMappingRepository;
import org.hibernate.search.engine.mapper.mapping.MappingKey;
import org.hibernate.search.engine.mapper.mapping.spi.MappingImplementor;
import org.hibernate.search.util.SearchException;


/**
 * @author Yoann Rodiere
 */
public class SearchMappingRepositoryImpl implements SearchMappingRepository {

	private final Map<MappingKey<?>, MappingImplementor> mappings;

	SearchMappingRepositoryImpl(Map<MappingKey<?>, MappingImplementor> mappings) {
		super();
		this.mappings = mappings;
	}

	@Override
	public <M> M getMapping(MappingKey<M> mappingKey) {
		@SuppressWarnings("unchecked") // See SearchMappingRepositoryBuilderImpl: we are sure that, if there is a mapping, it implements M
		M mapping = (M) mappings.get( mappingKey );
		if ( mapping == null ) {
			throw new SearchException( "No mapping registered for mapping key '" + mappingKey + "'" );
		}
		return mapping;
	}

	@Override
	public void close() {
		// TODO use a Closer
		for ( MappingImplementor mapping : mappings.values() ) {
			mapping.close();
		}
		// FIXME close backends too
	}
}
