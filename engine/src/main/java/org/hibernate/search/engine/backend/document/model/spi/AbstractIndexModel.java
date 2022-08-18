/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.model.spi;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.metamodel.IndexDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexFieldDescriptor;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.common.reporting.spi.EventContextProvider;


public abstract class AbstractIndexModel<
				S extends AbstractIndexModel<?, R, F>,
				R extends IndexCompositeNode<?, ?, ?>,
				F extends IndexField<?, ?>
		>
		implements EventContextProvider, IndexDescriptor {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String hibernateSearchIndexName;
	private final EventContext eventContext;

	private final String mappedTypeName;

	private final IndexIdentifier identifier;
	private final R root;
	private final Map<String, F> staticFields;
	private final List<IndexFieldDescriptor> includedStaticFields;
	private final List<? extends AbstractIndexFieldTemplate<? super S, ? extends F, ? super R, ?>> fieldTemplates;
	private final ConcurrentMap<String, F> dynamicFieldsCache = new ConcurrentHashMap<>();

	public AbstractIndexModel(String hibernateSearchIndexName,
			String mappedTypeName,
			IndexIdentifier identifier,
			R root, Map<String, F> staticFields,
			List<? extends AbstractIndexFieldTemplate<? super S, ? extends F, ? super R, ?>> fieldTemplates) {
		this.hibernateSearchIndexName = hibernateSearchIndexName;
		this.eventContext = EventContexts.fromIndexName( hibernateSearchIndexName );
		this.mappedTypeName = mappedTypeName;
		this.identifier = identifier;
		this.root = root;
		this.staticFields = CollectionHelper.toImmutableMap( staticFields );
		this.includedStaticFields = CollectionHelper.toImmutableList( staticFields.values().stream()
				.filter( field -> IndexFieldInclusion.INCLUDED.equals( field.inclusion() ) )
				.collect( Collectors.toList() ) );
		this.fieldTemplates = fieldTemplates;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[indexName=" + hibernateSearchIndexName + "]";
	}

	protected abstract S self();

	@Override
	public final EventContext eventContext() {
		return eventContext;
	}

	@Override
	public final String hibernateSearchName() {
		return hibernateSearchIndexName;
	}

	public IndexIdentifier identifier() {
		return identifier;
	}

	@Override
	public final R root() {
		return root;
	}

	@Override
	public final Optional<IndexFieldDescriptor> field(String absolutePath) {
		return Optional.ofNullable( fieldOrNull( absolutePath ) );
	}

	public final F fieldOrNull(String absolutePath) {
		return fieldOrNull( absolutePath, IndexFieldFilter.INCLUDED_ONLY );
	}

	public final F fieldOrNull(String absolutePath, IndexFieldFilter filter) {
		try {
			F field = fieldOrNullIgnoringInclusion( absolutePath );
			return field == null ? null : filter.filter( field, field.inclusion() );
		}
		catch (SearchException e) {
			throw log.unableToResolveField( absolutePath, e.getMessage(), e, eventContext );
		}
	}

	@Override
	public final Collection<IndexFieldDescriptor> staticFields() {
		return includedStaticFields;
	}

	public final String mappedTypeName() {
		return mappedTypeName;
	}

	private F fieldOrNullIgnoringInclusion(String absolutePath) {
		F field = staticFields.get( absolutePath );
		if ( field != null ) {
			return field;
		}
		field = dynamicFieldsCache.get( absolutePath );
		if ( field != null ) {
			return field;
		}
		for ( AbstractIndexFieldTemplate<? super S, ? extends F, ? super R, ?> template : fieldTemplates ) {
			field = template.createNodeIfMatching( self(), root, absolutePath );
			if ( field != null ) {
				F previous = dynamicFieldsCache.putIfAbsent( absolutePath, field );
				if ( previous != null ) {
					// Some other thread created the node before us.
					// Keep the first created node, discard ours: they are identical.
					field = previous;
				}
				break;
			}
		}
		return field;
	}

}
