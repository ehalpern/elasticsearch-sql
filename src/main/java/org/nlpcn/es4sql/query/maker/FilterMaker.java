package org.nlpcn.es4sql.query.maker;

import org.elasticsearch.index.query.BaseFilterBuilder;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.nlpcn.es4sql.domain.Condition;
import org.nlpcn.es4sql.domain.Where;
import org.nlpcn.es4sql.domain.Where.CONN;
import java.sql.SQLSyntaxErrorException;

public class FilterMaker extends Maker {

	/**
	 * 将where条件构建成filter
	 * 
	 * @param where
	 * @return
	 * @throws SQLSyntaxErrorException
	 */
	public static BoolFilterBuilder explan(Where where) throws SQLSyntaxErrorException {
		BoolFilterBuilder boolFilter = FilterBuilders.boolFilter();
		new FilterMaker().explanWhere(boolFilter, where);
		return boolFilter;
	}

	private FilterMaker() {
		super(false);
	}

	private void explanWhere(BoolFilterBuilder boolFilter, Where where) throws SQLSyntaxErrorException {
		while (where.getWheres().size() == 1) {
			where = where.getWheres().getFirst();
		}
		if (where instanceof Condition) {
			addSubFilter(boolFilter, where, (BaseFilterBuilder) make((Condition) where));
		} else {
			BoolFilterBuilder subFilter = FilterBuilders.boolFilter();
			addSubFilter(boolFilter, where, subFilter);
			for (Where subWhere : where.getWheres()) {
				explanWhere(subFilter, subWhere);
			}
		}
	}

	/**
	 * 增加嵌套插
	 * 
	 * @param boolFilter
	 * @param where
	 * @param subFilter
	 */
	private void addSubFilter(BoolFilterBuilder boolFilter, Where where, BaseFilterBuilder subFilter) {
		if (where.getConn() == CONN.AND) {
			boolFilter.must(subFilter);
		} else {
			boolFilter.should(subFilter);
		}
	}

}
