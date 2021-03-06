// Copyright (c) YugaByte, Inc.

package com.yugabyte.yw.metrics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yugabyte.yw.common.ApiHelper;
import com.yugabyte.yw.common.FakeDBApplication;
import com.yugabyte.yw.models.MetricConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import play.libs.Json;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class MetricQueryExecutorTest extends FakeDBApplication {
  @Mock
  play.Configuration mockAppConfig;

  @Mock
  ApiHelper mockApiHelper;

  @Mock
  YBMetricQueryComponent mockYBMetricQueryComponent;

  private MetricConfig validMetric;

  @Before
  public void setUp() {
    when(mockAppConfig.getString("yb.metrics.url")).thenReturn("foo://bar");


    JsonNode configJson = Json.parse("{\"metric\": \"our_valid_metric\", " +
                                       "\"function\": \"sum\", \"filters\": {\"filter\": \"awesome\"}," +
                                       "\"layout\": {\"title\": \"Awesome Metric\", " +
                                       "\"xaxis\": { \"type\": \"date\" }}}");
    validMetric = MetricConfig.create("valid_metric", configJson);
    validMetric.save();
  }

  @Test
  public void testWithValidMetric() throws Exception {
    HashMap<String, String> params = new HashMap<>();
    params.put("start", "1479281737");
    params.put("queryKey", "valid_metric");
    MetricQueryExecutor qe = new MetricQueryExecutor(mockAppConfig, mockApiHelper, params,
                                                     new HashMap<>(), mockYBMetricQueryComponent);

    JsonNode responseJson = Json.parse("{\"status\":\"success\",\"data\":{\"resultType\":\"vector\",\"result\":[{\"metric\":\n" +
                                         " {\"cpu\":\"system\"},\"value\":[1479278137,\"0.027751899056199826\"]},{\"metric\":\n" +
                                         " {\"cpu\":\"system\"}, \"value\":[1479278137,\"0.04329469299783263\"]}]}}");

    when(mockApiHelper.getRequest(eq("foo://bar/query"), anyMap(), anyMap())).thenReturn(Json.toJson(responseJson));

    JsonNode result = qe.call();
    assertThat(result.get("queryKey").asText(), allOf(notNullValue(), equalTo("valid_metric")));

    JsonNode data = result.get("data");
    assertThat(data, allOf(notNullValue(), instanceOf(ArrayNode.class)));
    assertEquals(data.size(), 2);
    for (int i = 0; i< data.size(); i++) {
      assertThat(data.get(i).get("name").asText(), allOf(notNullValue(), equalTo("system")));
      assertThat(data.get(i).get("type").asText(), allOf(notNullValue(), equalTo("scatter")));
      assertThat(data.get(i).get("x"), allOf(notNullValue(), instanceOf(ArrayNode.class)));
      assertThat(data.get(i).get("y"), allOf(notNullValue(), instanceOf(ArrayNode.class)));
    }

    JsonNode layout = result.get("layout");
    assertThat(layout, allOf(notNullValue(), instanceOf(ObjectNode.class)));
    assertThat(layout.get("title").asText(), allOf(notNullValue(), equalTo("Awesome Metric")));
    assertThat(layout.get("xaxis"), allOf(notNullValue(), instanceOf(ObjectNode.class)));
    assertThat(layout.get("xaxis").get("type").asText(), allOf(notNullValue(), equalTo("date")));
  }

  @Test
  public void testWithInvalidMetric() throws Exception {
    HashMap<String, String> params = new HashMap<>();
    params.put("start", "1479281737");
    params.put("queryKey", "invalid_metric");

    JsonNode responseJson = Json.parse("{\"status\":\"success\",\"data\":{\"resultType\":\"vector\",\"result\":[{\"metric\":\n" +
                                         " {\"cpu\":\"system\"},\"value\":[1479278137,\"0.027751899056199826\"]}]}}");

    when(mockApiHelper.getRequest(eq("foo://bar/query"), anyMap(), anyMap())).thenReturn(Json.toJson(responseJson));

    MetricQueryExecutor qe = new MetricQueryExecutor(mockAppConfig, mockApiHelper, params,
                                                     new HashMap<>(), mockYBMetricQueryComponent);
    JsonNode result = qe.call();

    assertThat(result.get("queryKey").asText(), allOf(notNullValue(), equalTo("invalid_metric")));
    assertThat(result.get("error").asText(), allOf(notNullValue(), equalTo("Invalid Query Key")));
  }

  @Test
  public void testQueryWithEndDate() throws Exception {
    HashMap<String, String> params = new HashMap<>();
    params.put("start", "1479281737");
    params.put("end", "1479281937");
    params.put("queryKey", "valid_metric");

    MetricQueryExecutor qe = new MetricQueryExecutor(mockAppConfig, mockApiHelper, params,
                                                     new HashMap<>(), mockYBMetricQueryComponent);

    JsonNode responseJson = Json.parse("{\"status\":\"success\",\"data\":{\"resultType\":\"vector\",\"result\":[{\"metric\":\n" +
                                         " {\"cpu\":\"system\"},\"value\":[1479278137,\"0.027751899056199826\"]},{\"metric\":\n" +
                                         " {\"cpu\":\"system\"}, \"value\":[1479278137,\"0.04329469299783263\"]}]}}");

    ArgumentCaptor<String> queryUrl = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Map> queryParam = ArgumentCaptor.forClass(Map.class);


    when(mockApiHelper.getRequest(anyString(), anyMap(), anyMap())).thenReturn(Json.toJson(responseJson));
    qe.call();
    verify(mockApiHelper).getRequest(queryUrl.capture(), anyMap(), (Map<String, String>) queryParam.capture());

    assertThat(queryUrl.getValue(), allOf(notNullValue(), equalTo("foo://bar/query_range")));

    assertThat(queryParam.getValue(), allOf(notNullValue(), instanceOf(Map.class)));
    assertThat(queryParam.getValue().toString(),
               allOf(notNullValue(), equalTo("{start=1479281737, queryKey=valid_metric, end=1479281937, " +
                                               "query=sum(our_valid_metric{filter=\"awesome\"})}")));
  }

  @Test
  public void testQueryWithoutEndDate() throws Exception {
    HashMap<String, String> params = new HashMap<>();
    params.put("start", "1479281737");
    params.put("queryKey", "valid_metric");

    MetricQueryExecutor qe = new MetricQueryExecutor(mockAppConfig, mockApiHelper, params,
                                                     new HashMap<>(), mockYBMetricQueryComponent);

    JsonNode responseJson = Json.parse("{\"status\":\"success\",\"data\":{\"resultType\":\"vector\",\"result\":[{\"metric\":\n" +
                                         " {\"cpu\":\"system\"},\"value\":[1479278137,\"0.027751899056199826\"]},{\"metric\":\n" +
                                         " {\"cpu\":\"system\"}, \"value\":[1479278137,\"0.04329469299783263\"]}]}}");

    ArgumentCaptor<String> queryUrl = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Map> queryParam = ArgumentCaptor.forClass(Map.class);


    when(mockApiHelper.getRequest(anyString(), anyMap(), anyMap())).thenReturn(Json.toJson(responseJson));
    qe.call();
    verify(mockApiHelper).getRequest(queryUrl.capture(), anyMap(), (Map<String, String>) queryParam.capture());

    assertThat(queryUrl.getValue(), allOf(notNullValue(), equalTo("foo://bar/query")));

    assertThat(queryParam.getValue(), allOf(notNullValue(), instanceOf(Map.class)));
    assertThat(queryParam.getValue().toString(),
               allOf(notNullValue(), equalTo("{start=1479281737, queryKey=valid_metric, " +
                                               "query=sum(our_valid_metric{filter=\"awesome\"})}")));
  }

  @Test
  public void testInvalidQuery() throws Exception  {
    HashMap<String, String> params = new HashMap<>();
    params.put("start", "1479281737");
    params.put("queryKey", "valid_metric");

    MetricQueryExecutor qe = new MetricQueryExecutor(mockAppConfig, mockApiHelper, params,
                                                     new HashMap<>(), mockYBMetricQueryComponent);

    JsonNode responseJson = Json.parse("{\"status\":\"error\",\"errorType\":\"bad_data\"," +
            "\"error\":\"parse error at char 44: unexpected \\\"{\\\" in aggregation, expected \\\")\\\"\"}");
    when(mockApiHelper.getRequest(anyString(), anyMap(), anyMap())).thenReturn(Json.toJson(responseJson));
    JsonNode response = qe.call();
    assertThat(response.get("error").asText(), allOf(notNullValue(), equalTo("parse error at char 44: unexpected " +
            "\"{\" in aggregation, expected \")\"")));
  }

  @Test
  public void testNativeMetrics() throws Exception {
    when(mockAppConfig.getBoolean("yb.metrics.useNative")).thenReturn(true);
    HashMap<String, String> params = new HashMap<>();
    params.put("start", "1479281737");
    params.put("queryKey", "valid_metric");
    MetricQueryExecutor qe = new MetricQueryExecutor(mockAppConfig, mockApiHelper, params,
                                                     new HashMap<>(), mockYBMetricQueryComponent);

    JsonNode responseJson = Json.parse("{\"status\":\"success\",\"data\":{\"resultType\":\"vector\",\"result\":[{\"metric\":\n" +
                                         " {\"cpu\":\"system\"},\"value\":[1479278137,\"0.027751899056199826\"]},{\"metric\":\n" +
                                         " {\"cpu\":\"system\"}, \"value\":[1479278137,\"0.04329469299783263\"]}]}}");

    when(mockYBMetricQueryComponent.query(anyMap())).thenReturn(Json.toJson(responseJson));

    JsonNode result = qe.call();
    assertThat(result.get("queryKey").asText(), allOf(notNullValue(), equalTo("valid_metric")));

    JsonNode data = result.get("data");
    assertThat(data, allOf(notNullValue(), instanceOf(ArrayNode.class)));
    assertEquals(2, data.size());
    for (int i = 0; i< data.size(); i++) {
      assertThat(data.get(i).get("name").asText(), allOf(notNullValue(), equalTo("system")));
      assertThat(data.get(i).get("type").asText(), allOf(notNullValue(), equalTo("scatter")));
      assertThat(data.get(i).get("x"), allOf(notNullValue(), instanceOf(ArrayNode.class)));
      assertThat(data.get(i).get("y"), allOf(notNullValue(), instanceOf(ArrayNode.class)));
    }

    JsonNode layout = result.get("layout");
    assertThat(layout, allOf(notNullValue(), instanceOf(ObjectNode.class)));
    assertThat(layout.get("title").asText(), allOf(notNullValue(), equalTo("Awesome Metric")));
    assertThat(layout.get("xaxis"), allOf(notNullValue(), instanceOf(ObjectNode.class)));
    assertThat(layout.get("xaxis").get("type").asText(), allOf(notNullValue(), equalTo("date")));
  }
}
