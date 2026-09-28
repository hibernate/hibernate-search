package org.hibernate.search.util.impl.integrationtest.backend.elasticsearch;

public class ElasticsearchIndexSettingsTestUtils {

	private ElasticsearchIndexSettingsTestUtils() {
	}

	public static String settingsEnableReadWrite(boolean enable) {
		boolean block = !enable;
		return "{'blocks.write': " + block + ", 'blocks.read': " + block + "}";
	}
}
