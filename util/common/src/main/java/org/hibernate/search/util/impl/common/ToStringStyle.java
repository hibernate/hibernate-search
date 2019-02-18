/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.common;

public class ToStringStyle {
	private static final ToStringStyle INLINE_DELIMITER_STRUCTURE = new ToStringStyle(
			" ", ",", "=",
			"{", "}", "",
			"[", "]", "", "", false
	);

	private static final ToStringStyle MULTILINE_DELIMITER_STRUCTURE = new ToStringStyle(
			"\n", "", "=",
			"{", "}", "\t",
			"[", "]", "\t", "\t", false
	);

	/**
	 * @return A single-line format relying on delimiters to display the structure (JSON-style).
	 */
	public static ToStringStyle inlineDelimiterStructure() {
		return INLINE_DELIMITER_STRUCTURE;
	}

	/**
	 * @return A multi-line format relying on delimiters to display the structure (JSON-style).
	 */
	public static ToStringStyle multilineDelimiterStructure() {
		return MULTILINE_DELIMITER_STRUCTURE;
	}

	/**
	 * @return A multi-line format relying on indenting and bullet points to display the structure (YAML-style).
	 * Object and array delimiters are not shown.
	 */
	public static ToStringStyle multilineIndentStructure(String nameValueSeparator, String indentInObject,
			String indentInListBulletPoint, String indentInListNoBulletPoint) {
		return new ToStringStyle(
				"\n", "", nameValueSeparator,
				"", "", indentInObject,
				"", "",
				indentInListBulletPoint, indentInListNoBulletPoint, true
		);
	}

	final String newline;
	final String entrySeparator;
	final String nameValueSeparator;
	final String startObject;
	final String endObject;
	final String indentInObject;
	final String startList;
	final String endList;
	final String indentInListBulletPoint;
	final String indentInListNoBulletPoint;
	final boolean squeezeObjectsInList;

	private ToStringStyle(String newline, String entrySeparator, String nameValueSeparator,
			String startObject, String endObject, String indentInObject,
			String startList, String endList,
			String indentInListBulletPoint, String indentInListNoBulletPoint, boolean squeezeObjectsInList) {
		this.newline = newline;
		this.entrySeparator = entrySeparator;
		this.nameValueSeparator = nameValueSeparator;
		this.startObject = startObject;
		this.endObject = endObject;
		this.indentInObject = indentInObject;
		this.startList = startList;
		this.endList = endList;
		this.indentInListBulletPoint = indentInListBulletPoint;
		this.indentInListNoBulletPoint = indentInListNoBulletPoint;
		this.squeezeObjectsInList = squeezeObjectsInList;
	}
}
