package org.nlpcn.es4sql.query.maker;

import org.elasticsearch.index.query.BaseQueryBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.nlpcn.es4sql.domain.Condition;
import org.nlpcn.es4sql.domain.Where;
import org.nlpcn.es4sql.domain.Where.CONN;
import java.sql.SQLSyntaxErrorException;

public class QueryMaker extends Maker {

	/**
	 * 将where条件构建成query
	 * 
	 * @param where
	 * @return
	 * @throws SQLSyntaxErrorException
	 */
	public static BoolQueryBuilder explan(Where where) throws SQLSyntaxErrorException {
		//QueryBuilders.queryString();
		BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
		new QueryMaker().explanWhere(boolQuery, where);
		return boolQuery;
	}

	private QueryMaker() {
		super(true);
	}

	private void explanWhere(BoolQueryBuilder boolQuery, Where where) throws SQLSyntaxErrorException {
		while (where.getWheres().size() == 1) {
			where = where.getWheres().getFirst();
		}
		if (where instanceof Condition) {
			addSubQuery(boolQuery, where, (BaseQueryBuilder) make((Condition) where));
		} else {
			BoolQueryBuilder subQuery = QueryBuilders.boolQuery();
			addSubQuery(boolQuery, where, subQuery);
			for (Where subWhere : where.getWheres()) {
				explanWhere(subQuery, subWhere);
			}
		}
	}

	/**
	 * 增加嵌套插
	 * 
	 * @param boolQuery
	 * @param where
	 * @param subQuery
	 */
	private void addSubQuery(BoolQueryBuilder boolQuery, Where where, BaseQueryBuilder subQuery) {
		if (where.getConn() == CONN.AND) {
			boolQuery.must(subQuery);
		} else {
			boolQuery.should(subQuery);
		}
	}
}
