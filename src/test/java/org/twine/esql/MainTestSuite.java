package org.twine.esql;

import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.deletebyquery.DeleteByQueryRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.io.ByteStreams;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.node.NodeBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.nlpcn.es4sql.SearchDao;

import java.io.FileInputStream;

@RunWith(Suite.class)
@Suite.SuiteClasses({
		QueryTest.class,
		AggregationTest.class,
		//BugTest.class,
	  EsqlQueryTest.class
})
public class MainTestSuite {

	private static Client client;
	private static SearchDao searchDao;

	@BeforeClass
	public static void setUp() throws Exception {
		Settings settings = ImmutableSettings.settingsBuilder()
			.put("discovery.zen.ping.multicast.enabled", false)
			.put("discovery.zen.ping.unicast.hosts", "localhost")
			.build();
		client = NodeBuilder.nodeBuilder().settings(settings).node().client();
		//client = new TransportClient();
		//client.addTransportAddress(getTransportAddress());

		NodesInfoResponse nodeInfos = client.admin().cluster().prepareNodesInfo().get();
		String clusterName = nodeInfos.getClusterName().value();
		System.out.println(String.format("Found cluster... cluster name: %s", clusterName));

		// Load test data.
		deleteQuery(TestsConstants.TEST_INDEX);
		loadBulk("js/test/resources/accounts.json");
		loadBulk("js/test/resources/phrases.json");
		loadBulk("js/test/resources/online.json");

		prepareOdbcIndex();
		loadBulk("js/test/resources/odbc-date-formats.json");

		searchDao = new SearchDao(client);
		System.out.println("Finished the setup process...");
	}


	@AfterClass
	public static void tearDown() {
		System.out.println("teardown process...");
	}


	/**
	 * Delete all data inside specific index
	 * @param indexName the documents inside this index will be deleted.
	 */
	public static void deleteQuery(String indexName) {
		deleteQuery(indexName, null);
	}

	/**
	 * Delete all data using DeleteByQuery.
	 * @param indexName the index to delete
	 * @param typeName the type to delete
	 */
	public static void deleteQuery(String indexName, String typeName) {
		try {
			DeleteByQueryRequestBuilder deleteQuery = new DeleteByQueryRequestBuilder(client);
			deleteQuery.setIndices(indexName);
			if (typeName != null) {
				deleteQuery.setTypes(typeName);
			}
			deleteQuery.setQuery(QueryBuilders.matchAllQuery());

			deleteQuery.get();
			System.out.println(String.format("Deleted index %s and type %s", indexName, typeName));
		}
		catch(IndexMissingException e) {
			System.out.println(String.format("Failed to delete index, Index %s does not exist, continue any way", indexName));
		}
	}


	/**
	 * Loads all data from the json into the test
	 * elasticsearch cluster, using TEST_INDEX
	 * @param jsonPath the json file represents the bulk
	 * @throws Exception
	 */
	public static void loadBulk(String jsonPath) throws Exception {
		System.out.println(String.format("Loading file %s into elasticsearch cluster", jsonPath));

		BulkRequestBuilder bulkBuilder = new BulkRequestBuilder(client);
		byte[] buffer = ByteStreams.toByteArray(new FileInputStream(jsonPath));
		bulkBuilder.add(buffer, 0, buffer.length, true, TestsConstants.TEST_INDEX, null);
		BulkResponse response = bulkBuilder.get();

		if(response.hasFailures()) {
			throw new Exception(String.format("Failed during bulk load of file %s. failure message: %s", jsonPath, response.buildFailureMessage()));
		}
	}

    public static void prepareOdbcIndex(){
        String dataMapping = "{\n" +
                "\t\"odbc\" :{\n" +
                "\t\t\"properties\":{\n" +
                "\t\t\t\"insert_time\":{\n" +
                "\t\t\t\t\"type\":\"date\",\n" +
                "\t\t\t\t\"format\": \"{'ts' ''yyyy-MM-dd HH:mm:ss.SSS''}\"\n" +
                "\t\t\t},\n" +
                "\t\t\t\"docCount\":{\n" +
                "\t\t\t\t\"type\":\"string\"\n" +
                "\t\t\t}\n" +
                "\t\t}\n" +
                "\t}\n" +
                "}";

        client.admin().indices().preparePutMapping(TestsConstants.TEST_INDEX).setType("odbc").setSource(dataMapping).execute().actionGet();
    }

	public static SearchDao getSearchDao() {
		return searchDao;
	}

	public static Client getClient() {
		return client;
	}

	private static InetSocketTransportAddress getTransportAddress() {
		String host = System.getenv("ES_TEST_HOST");
		String port = System.getenv("ES_TEST_PORT");

		if(host == null) {
			host = "localhost";
			System.out.println("ES_TEST_HOST enviroment variable does not exist. choose default 'localhost'");
		}

		if(port == null) {
			port = "9200";
			System.out.println("ES_TEST_PORT enviroment variable does not exist. choose default '9300'");
		}

		System.out.println(String.format("Connection details: host: %s. port:%s.", host, port));
		return new InetSocketTransportAddress(host, Integer.parseInt(port));
	}


}
