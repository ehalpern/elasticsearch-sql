package org.twine.elasticsearch.plugin;

import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.*;
import org.nlpcn.es4sql.SearchDao;
import org.nlpcn.es4sql.query.explain.ExplainManager;

import java.io.IOException;
import java.sql.SQLException;

public class RestSqlAction extends BaseRestHandler {

	@Inject
	public RestSqlAction(Settings settings, Client client, RestController restController) {
		super(settings, restController, client);
		restController.registerHandler(RestRequest.Method.POST, "/_esql/_explain", this);
		restController.registerHandler(RestRequest.Method.GET, "/_esql/_explain", this);
		restController.registerHandler(RestRequest.Method.POST, "/_esql", this);
		restController.registerHandler(RestRequest.Method.GET, "/_esql", this);
	}

	@Override
	protected void handleRequest(
		RestRequest request, RestChannel channel, final Client client
	)
		throws IOException, SQLException
	{
		String sql = request.param("sql");

		if (sql == null) {
			sql = request.content().toUtf8();
		}

		SearchDao searchDao = new SearchDao(client);
		ActionRequestBuilder actionRequestBuilder = searchDao.explain(sql);
		ActionRequest actionRequest = actionRequestBuilder.request();

		// TODO add unittests to explain. (rest level?)
		if (request.path().endsWith("/_explain")) {
			String jsonExplanation = ExplainManager.explain(actionRequestBuilder);
			BytesRestResponse bytesRestResponse = new BytesRestResponse(RestStatus.OK, jsonExplanation);
			channel.sendResponse(bytesRestResponse);
		} else {
			new ActionRequestExecuter(actionRequest, channel, client, searchDao.getColumns()).execute();
		}
	}
}