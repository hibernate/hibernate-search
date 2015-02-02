/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.cfg;

import java.lang.annotation.ElementType;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.util.TokenizerFactory;

/**
 * Configures index-embedded association.
 *
 * @author Emmanuel Bernard
 * @author Gunnar Morling
 * @see org.hibernate.search.annotations.IndexedEmbedded
 */
public class IndexEmbeddedMapping {

	private final SearchMapping mapping;
	private final Map<String,Object> indexEmbedded;
	private final EntityDescriptor entity;
	private final PropertyDescriptor property;

	public IndexEmbeddedMapping(SearchMapping mapping, PropertyDescriptor property, EntityDescriptor entity) {
		this.mapping = mapping;
		this.indexEmbedded = new HashMap<String, Object>();
		this.property = property;
		this.entity = entity;
		this.property.setIndexEmbedded( indexEmbedded );
	}

	public IndexEmbeddedMapping prefix(String prefix) {
		this.indexEmbedded.put( "prefix", prefix );
		return this;
	}

	public IndexEmbeddedMapping targetElement(Class<?> targetElement) {
		this.indexEmbedded.put( "targetElement", targetElement );
		return this;
	}

	public IndexEmbeddedMapping depth(int depth) {
		this.indexEmbedded.put( "depth", depth );
		return this;
	}

	public IndexEmbeddedMapping includeEmbeddedObjectId(boolean includeEmbeddedObjectId) {
		this.indexEmbedded.put( "includeEmbeddedObjectId", includeEmbeddedObjectId );
		return this;
	}

	public IndexEmbeddedMapping indexNullAs(String nullToken) {
		this.indexEmbedded.put( "indexNullAs", nullToken );
		return this;
	}

	public IndexEmbeddedMapping includePaths(String firstPath, String... furtherPaths) {
		this.indexEmbedded.put( "includePaths", merge( firstPath, furtherPaths ) );
		return this;
	}

	private String[] merge(String firstPath, String... furtherPaths) {
		if ( furtherPaths == null ) {
			return new String[] { firstPath };
		}
		else {
			String[] paths = new String[1 + furtherPaths.length];
			paths[0] = firstPath;
			System.arraycopy( furtherPaths, 0, paths, 1, furtherPaths.length );

			return paths;
		}
	}

	public PropertyMapping property(String name, ElementType type) {
		return new PropertyMapping( name, type, entity, mapping );
	}

	public AnalyzerDefMapping analyzerDef(String name, Class<? extends TokenizerFactory> tokenizerFactory) {
		return new AnalyzerDefMapping( name, tokenizerFactory, mapping );
	}

	public EntityMapping entity(Class<?> entityType) {
		return new EntityMapping( entityType, mapping );
	}

	public FieldMapping field() {
		return new FieldMapping( property, entity, mapping );
	}
}
