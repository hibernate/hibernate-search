/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.metadata.impl;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.annotations.common.reflection.XClass;

/**
 * Collects context information needed during the processing of the annotations.
 *
 * @author Hardy Ferentschik
 */
public class ParseContext {
	public static final boolean SKIP_FIELD_BRIDGES = true;
	public static final boolean INCLUDE_FIELD_BRIDGES = false;

	private final Set<XClass> processedClasses = new HashSet<>();
	private final Set<String> spatialNames = new TreeSet<>();
	private final Set<String> unqualifiedCollectedCollectionRoles = new HashSet<>();

	private XClass mappedClass;
	private boolean skipFieldBridges;
	private XClass currentClass;
	private int level = 0;
	private int maxLevel = Integer.MAX_VALUE;
	private boolean explicitDocumentId = false;
	private boolean includeEmbeddedObjectId = false;

	public ParseContext(XClass mappedClass, boolean skipFieldBridges) {
		this.mappedClass = mappedClass;
		this.skipFieldBridges = skipFieldBridges;
	}

	public XClass getMappedClass() {
		return mappedClass;
	}

	public boolean skipFieldBridges() {
		return skipFieldBridges;
	}

	boolean hasBeenProcessed(XClass processedClass) {
		return processedClasses.contains( processedClass );
	}

	void processingClass(XClass processedClass) {
		processedClasses.add( processedClass );
	}

	void removeProcessedClass(XClass processedClass) {
		processedClasses.remove( processedClass );
	}

	boolean isSpatialNameUsed(String name) {
		return spatialNames.contains( name );
	}

	void markSpatialNameAsUsed(String name) {
		spatialNames.add( name );
	}

	public XClass getCurrentClass() {
		return currentClass;
	}

	public void setCurrentClass(XClass currentClass) {
		this.currentClass = currentClass;
	}

	boolean isMaxLevelReached() {
		return level > maxLevel;
	}

	public int getMaxLevel() {
		return maxLevel;
	}

	public void setMaxLevel(int newMaxLevel) {
		this.maxLevel = newMaxLevel;
	}

	public int getLevel() {
		return level;
	}

	public void incrementLevel() {
		this.level++;
	}

	public void decrementLevel() {
		this.level--;
	}

	public boolean isExplicitDocumentId() {
		return explicitDocumentId;
	}

	public void setExplicitDocumentId(boolean explicitDocumentId) {
		this.explicitDocumentId = explicitDocumentId;
	}

	public Set<String> getCollectedUnqualifiedCollectionRoles() {
		return unqualifiedCollectedCollectionRoles;
	}

	public void collectUnqualifiedCollectionRole(String unqualifiedCollectionRole) {
		unqualifiedCollectedCollectionRoles.add( unqualifiedCollectionRole );
	}

	public boolean includeEmbeddedObjectId() {
		return includeEmbeddedObjectId;
	}

	public void setIncludeEmbeddedObjectId(boolean includeEmbeddedObjectId) {
		this.includeEmbeddedObjectId = includeEmbeddedObjectId;
	}
}


