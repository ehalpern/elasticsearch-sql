package org.twine.esql;


import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.avg.Avg;
import org.elasticsearch.search.aggregations.metrics.max.Max;
import org.elasticsearch.search.aggregations.metrics.min.Min;
import org.elasticsearch.search.aggregations.metrics.stats.Stats;
import org.elasticsearch.search.aggregations.metrics.sum.Sum;
import org.elasticsearch.search.aggregations.metrics.valuecount.ValueCount;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.nlpcn.es4sql.SearchDao;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.*;

import static org.elasticsearch.search.aggregations.bucket.range.Range.Bucket;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;


public class AggregationTest {


	@Test
	public void countTest() throws IOException, SQLException, SQLFeatureNotSupportedException {
		Aggregations result = query(String.format("SELECT COUNT(*) FROM %s.account", TestsConstants.TEST_INDEX));
		ValueCount count = result.get("COUNT(*)");
		Assert.assertEquals(1000, count.getValue());
	}


	@Test
	public void sumTest() throws IOException, SQLException, SQLFeatureNotSupportedException {
		Aggregations result = query(String.format("SELECT SUM(balance) FROM %s.account", TestsConstants.TEST_INDEX));
		Sum sum = result.get("SUM(balance)");
		assertThat(sum.getValue(), equalTo(25714837.0));
	}

	@Test
	public void minTest() throws IOException, SQLException, SQLFeatureNotSupportedException {
		Aggregations result = query(String.format("SELECT MIN(age) FROM %s.account", TestsConstants.TEST_INDEX));
		Min min = result.get("MIN(age)");
		assertThat(min.getValue(), equalTo(20.0));
	}

	@Test
	public void maxTest() throws IOException, SQLException {
		Aggregations result = query(String.format("SELECT MAX(age) FROM %s.account", TestsConstants.TEST_INDEX));
		Max max = result.get("MAX(age)");
		assertThat(max.getValue(), equalTo(40.0));
	}

	@Test
	public void avgTest() throws IOException, SQLException {
		Aggregations result = query(String.format("SELECT AVG(age) FROM %s.account", TestsConstants.TEST_INDEX));
		Avg avg = result.get("AVG(age)");
		assertThat(avg.getValue(), equalTo(30.171));
	}

	@Test
	public void statsTest() throws IOException, SQLException {
		Aggregations result = query(String.format("SELECT STATS(age) FROM %s.account", TestsConstants.TEST_INDEX));
		Stats stats = result.get("STATS(age)");
		Assert.assertEquals(1000, stats.getCount());
		assertThat(stats.getSum(), equalTo(30171.0));
		assertThat(stats.getMin(), equalTo(20.0));
		assertThat(stats.getMax(), equalTo(40.0));
		assertThat(stats.getAvg(), equalTo(30.171));
	}


	@Test
	public void aliasTest() throws IOException, SQLException {
		Aggregations result = query(String.format("SELECT COUNT(*) AS mycount FROM %s.account", TestsConstants.TEST_INDEX));
		assertThat(result.asMap(), hasKey("mycount"));
	}

	@Test
	public void groupByTest() throws Exception {
		Aggregations result = query(String.format("SELECT COUNT(*) FROM %s.account GROUP BY gender", TestsConstants.TEST_INDEX));
		Terms gender = result.get("gender");
		for(Terms.Bucket bucket : gender.getBuckets()) {
			String key = bucket.getKey();
			long count = ((ValueCount) bucket.getAggregations().get("COUNT(*)")).getValue();
			if(key.equalsIgnoreCase("m")) {
				Assert.assertEquals(507, count);
			}
			else if(key.equalsIgnoreCase("f")) {
				Assert.assertEquals(493, count);
			}
			else {
				throw new Exception(String.format("Unexpected key. expected: m OR f. found: %s", key));
			}
		}
	}

	@Test
	public void multipleGroupByTest() throws Exception {
		Set expectedAges = new HashSet<Integer>(ContiguousSet.create(Range.closed(20, 40), DiscreteDomain.integers()));

		Map<String, Set<Integer>> buckets = new HashMap<>();

		Aggregations result = query(String.format("SELECT COUNT(*) FROM %s.account GROUP BY gender, age", TestsConstants.TEST_INDEX));
		Terms gender = result.get("gender");
		for(Terms.Bucket genderBucket : gender.getBuckets()) {
			String genderKey = genderBucket.getKey();
			buckets.put(genderKey, new HashSet<Integer>());
			Terms ageBuckets = (Terms) genderBucket.getAggregations().get("age");
			for(Terms.Bucket ageBucket : ageBuckets.getBuckets()) {
				buckets.get(genderKey).add(Integer.parseInt(ageBucket.getKey()));
			}
		}

		Assert.assertEquals(2, buckets.keySet().size());
		Assert.assertEquals(expectedAges, buckets.get("m"));
		Assert.assertEquals(expectedAges, buckets.get("f"));
	}


	@Test
	public void orderByAscTest() throws IOException, SQLException, SQLFeatureNotSupportedException {
		ArrayList<Long> agesCount = new ArrayList<>();

		Aggregations result = query(String.format("SELECT COUNT(*) FROM %s.account GROUP BY age ORDER BY COUNT(*)", TestsConstants.TEST_INDEX));
		Terms age = result.get("age");

		for(Terms.Bucket bucket : age.getBuckets()) {
			agesCount.add(((ValueCount) bucket.getAggregations().get("COUNT(*)")).getValue());
		}

		ArrayList<Long> sortedAgesCount = (ArrayList<Long>)agesCount.clone();
		Collections.sort(sortedAgesCount);
		Assert.assertTrue("The list is not ordered ascending", agesCount.equals(agesCount));
	}


	@Test
	public void orderByDescTest() throws IOException, SQLException, SQLFeatureNotSupportedException {
		ArrayList<Long> agesCount = new ArrayList<>();

		Aggregations result = query(String.format("SELECT COUNT(*) FROM %s.account GROUP BY age ORDER BY COUNT(*) DESC", TestsConstants.TEST_INDEX));
		Terms age = result.get("age");

		for(Terms.Bucket bucket : age.getBuckets()) {
			agesCount.add(((ValueCount) bucket.getAggregations().get("COUNT(*)")).getValue());
		}

		ArrayList<Long> sortedAgesCount = (ArrayList<Long>)agesCount.clone();
		Collections.sort(sortedAgesCount, Collections.reverseOrder());
		Assert.assertTrue("The list is not ordered descending", agesCount.equals(agesCount));
	}



	@Test
	public void limitTest() throws IOException, SQLException, SQLFeatureNotSupportedException {
		Aggregations result = query(String.format("SELECT COUNT(*) FROM %s.account GROUP BY age ORDER BY COUNT(*) LIMIT 5", TestsConstants.TEST_INDEX));
		Terms age = result.get("age");

		assertThat(age.getBuckets().size(), equalTo(5));
	}



	@Ignore @Test // not yet supported
	public void countGroupByRange() throws IOException, SQLException, SQLFeatureNotSupportedException {
		Aggregations result = query(String.format("SELECT COUNT(age) FROM %s.account GROUP BY range(age, 20,25,30,35,40) ", TestsConstants.TEST_INDEX));
		org.elasticsearch.search.aggregations.bucket.range.Range  ageRanges = result.get("range(age,20,25,30,35,40)");
		assertThat(ageRanges.getBuckets().size(), equalTo(4));

		long[] expectedResults = new long[] {225L, 226L, 259L, 245L};
		int index = 0;
		for(Bucket bucket : ageRanges.getBuckets()) {
			assertThat(((ValueCount) bucket.getAggregations().get("COUNT(age)")).getValue(), equalTo(expectedResults[index]));
			index++;
		}
	}

	/**
	 * 时间 聚合 , 每天按照天聚合 参数说明:
	 * 
	 * <a>http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/search-aggregations-bucket-datehistogram-aggregation.html</a>
	 * 
	 * @throws IOException
	 * @throws SQLException
	 */
	@Ignore @Test // Not supported yet
	public void countGroupByDateTest() throws IOException, SQLException {
		SearchRequestBuilder result = (SearchRequestBuilder) MainTestSuite.getSearchDao().explain("select insert_time from online  group by date_histogram(field='insert_time','interval'='1.5h','format'='yyyy-MM') ");
		System.out.println(result);
	}

	/**
	 * 时间范围聚合
	 * 
	 * <a>http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/search-aggregations-bucket-daterange-aggregation.html</a>
	 * 
	 * @throws IOException
	 * @throws SQLException
	 */
	@Ignore @Test // not supported yet
	public void countDateRangeTest() throws IOException, SQLException, SQLFeatureNotSupportedException {
		SearchRequestBuilder result = (SearchRequestBuilder) MainTestSuite.getSearchDao().explain("select online from online  group by date_range(field='insert_time','format'='yyyy-MM-dd' ,'2014-08-18','2014-08-17','now-8d','now-7d','now-6d','now') ");
		System.out.println(result);
	}


	/**
	 * tophits 查询
	 * 
	 * <a>http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/search-aggregations-metrics-top-hits-aggregation.html</a>
	 * 
	 * @throws IOException
	 * @throws SQLException
	 */
	@Ignore @Test // not supported yet
	public void topHitTest() throws IOException, SQLException, SQLFeatureNotSupportedException {
		SearchRequestBuilder result = (SearchRequestBuilder) MainTestSuite.getSearchDao().explain("select topHits('size'=3,age='desc') from bank  group by gender ");
		System.out.println(result);
	}

	private Aggregations query(String query) throws SQLException, SQLFeatureNotSupportedException {
		SearchDao searchDao = MainTestSuite.getSearchDao();
		SearchRequestBuilder select = (SearchRequestBuilder) searchDao.explain(query);
		return select.get().getAggregations();
	}

}
