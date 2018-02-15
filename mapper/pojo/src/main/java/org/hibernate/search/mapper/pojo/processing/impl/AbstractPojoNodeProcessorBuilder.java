/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.impl;

import org.hibernate.search.engine.mapper.mapping.building.spi.IndexModelBindingContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataContributorProvider;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoIndexModelBinder;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeMetadataContributor;

/**
 * @author Yoann Rodiere
 */
abstract class AbstractPojoNodeProcessorBuilder<T> {

	protected final TypeMetadataContributorProvider<PojoTypeNodeMetadataContributor> contributorProvider;

	protected final AbstractPojoNodeProcessorBuilder<?> parent;
	protected final PojoIndexModelBinder indexModelBinder;
	protected final IndexModelBindingContext bindingContext;

	AbstractPojoNodeProcessorBuilder(AbstractPojoNodeProcessorBuilder<?> parent,
			TypeMetadataContributorProvider<PojoTypeNodeMetadataContributor> contributorProvider,
			PojoIndexModelBinder indexModelBinder, IndexModelBindingContext bindingContext) {
		this.parent = parent;
		this.contributorProvider = contributorProvider;
		this.indexModelBinder = indexModelBinder;
		this.bindingContext = bindingContext;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder( getClass().getSimpleName() )
				.append( "[" );
		appendPath( builder);
		builder.append( "]" );
		return builder.toString();
	}

	abstract PojoNodeProcessor<T> build();

	private void appendPath(StringBuilder builder) {
		if ( parent == null ) {
			appendSelfPath( builder );
		}
		else {
			parent.appendPath( builder );
			builder.append( " => " );
			appendSelfPath( builder );
		}
	}

	protected abstract void appendSelfPath(StringBuilder builder);

}
