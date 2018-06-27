/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.common;

public class ToStringStyle {
	private static final ToStringStyle INLINE = new ToStringStyle(
			" ", "", ",", "=",
			"{", "}",
			"[", "]"
	);

	private static final ToStringStyle MULTILINE = new ToStringStyle(
			"\n", "\t", "", "=",
			"{", "}",
			"[", "]"
	);

	public static ToStringStyle inline() {
		return INLINE;
	}

	public static ToStringStyle multiline() {
		return MULTILINE;
	}

	final String newline;
	final String indent;
	final String entrySeparator;
	final String nameValueSeparator;
	final String startObject;
	final String endObject;
	final String startList;
	final String endList;

	private ToStringStyle(String newline, String indent, String entrySeparator, String nameValueSeparator,
			String startObject, String endObject,
			String startList, String endList) {
		this.newline = newline;
		this.indent = indent;
		this.entrySeparator = entrySeparator;
		this.nameValueSeparator = nameValueSeparator;
		this.startObject = startObject;
		this.endObject = endObject;
		this.startList = startList;
		this.endList = endList;
	}
}
