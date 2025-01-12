package org.nlpcn.es4sql.domain;

import java.util.List;

import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.WildcardQueryBuilder;
import java.sql.SQLSyntaxErrorException;

import org.durid.sql.ast.SQLExpr;
import org.durid.sql.ast.expr.SQLCharExpr;
import org.durid.sql.ast.expr.SQLMethodInvokeExpr;
import org.durid.sql.ast.expr.SQLNumericLiteralExpr;

public class Paramer {
	public String analysis;
	public Float boost;
	public String value;

	public static Paramer parseParamer(SQLMethodInvokeExpr method) throws SQLSyntaxErrorException {
		Paramer instance = new Paramer();
		List<SQLExpr> parameters = method.getParameters();
		instance.value = ((SQLCharExpr) parameters.get(0)).getText();
		SQLExpr sqlExpr = null;
		for (int i = 1; i < parameters.size(); i++) {
			sqlExpr = parameters.get(i);
			if (sqlExpr instanceof SQLCharExpr) {
				instance.analysis = ((SQLCharExpr) sqlExpr).getText();
			} else {
				instance.boost = ((SQLNumericLiteralExpr) sqlExpr).getNumber().floatValue();
			}
		}

		return instance;
	}

	public static ToXContent fullParamer(QueryStringQueryBuilder query, Paramer paramer) {
		if (paramer.analysis != null) {
			query.analyzer(paramer.analysis);
		}

		if (paramer.boost != null) {
			query.boost(paramer.boost);
		}
		return query;
	}

	public static ToXContent fullParamer(MatchQueryBuilder query, Paramer paramer) {
		if (paramer.analysis != null) {
			query.analyzer(paramer.analysis);
		}

		if (paramer.boost != null) {
			query.boost(paramer.boost);
		}
		return query;
	}

	public static ToXContent fullParamer(WildcardQueryBuilder query, Paramer paramer) {
		if (paramer.boost != null) {
			query.boost(paramer.boost);
		}
		return query;
	}

}
