/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.metadata.impl;

import org.hibernate.annotations.common.reflection.XMember;

/**
 * @author Guillaume Smet
 * @author Yoann Rodiere
 */
public class ContainedInMetadataBuilder {

	private final XMember containedInMember;

	private Integer maxDepth;

	private String prefix;

	private String[] includePaths;

	public ContainedInMetadataBuilder(XMember containedInMember) {
		this.containedInMember = containedInMember;
	}

	public ContainedInMetadataBuilder maxDepth(Integer maxDepth) {
		this.maxDepth = maxDepth;
		return this;
	}

	public ContainedInMetadataBuilder prefix(String prefix) {
		this.prefix = prefix;
		return this;
	}

	public ContainedInMetadataBuilder includePaths(String[] includePaths) {
		this.includePaths = includePaths;
		return this;
	}

	public ContainedInMetadata createContainedInMetadata() {
		return new ContainedInMetadata( containedInMember, maxDepth, prefix, includePaths );
	}

}
