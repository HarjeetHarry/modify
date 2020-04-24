package com.qTestApi.test;

import org.testng.annotations.Test;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.Map.Entry;

import org.json.simple.JSONObject;
import org.testng.annotations.Test;

import io.restassured.response.Response;

import com.qTestHSC.util.Utility;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

public class ApiCallTest {
	static String excelPath = "/Users/harjeetkaur/eclipseWorkspace1/qTestApi/payload/allData.xlsx";	
	static String CONTEXT_PATH3="/test-runs/{test-runId}/test-logs/{test-logId}";

	@Test
	public void testCaseStatusChangeWithAPI() {
		Map<String, List<Integer>> idMap = Utility.getApiIds();
		List<Integer> tcIdList = idMap.get("testCaseId");	    
		List<String> tcIdListFromExcel= Utility.getAllcolumnValuesByHeader(excelPath, "TestExecution" ,"Id").get("Id");
		List<String> tcStatusListFromExcel = Utility.getAllcolumnValuesByHeader(excelPath, "TestExecution", "PQT Status").get("PQT Status");

		HashMap<String, String> mapStatusCode = Utility.apiStatusCode();	
		Map<String, String> excelMapForTC = new LinkedHashMap<String, String>();
		for (int i=0; i<tcIdListFromExcel.size(); i++) {
			excelMapForTC.put(tcIdListFromExcel.get(i), tcStatusListFromExcel.get(i));					
		}

		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));	

		JSONObject	strJSON = new JSONObject();
		JSONObject	strJSON2 = new JSONObject();		
		strJSON.put("exe_start_date", formatter.format(new Date()) + "+00:00");
		strJSON.put("exe_end_date", formatter.format(new Date()) + "+00:00");

		int i=0;
		if(tcIdList.containsAll(tcIdListFromExcel)) {
			Set<Entry<String, String>> updatedDataMap = excelMapForTC.entrySet();		
			for(Entry<String, String> entry : updatedDataMap){	           
				strJSON2.put("id", mapStatusCode.get(entry.getValue()));
				strJSON.put("status", strJSON2);

				List<Integer> trIds = idMap.get("testRunId");
				List<Integer>  tlIds = idMap.get("testLogId");	
				int trId= trIds.get(i);
				int tlId= tlIds.get(i);
				i++;

				List<String> token = Utility.getAllcolumnValuesByHeader(excelPath, "Configuration", "Auth").get("Auth");
				RequestSpecification requestSpecification= RestAssured.given().log().all().
						pathParam("test-runId", trId).pathParam("test-logId", tlId).
						headers("Authorization",token).body(strJSON);	
				requestSpecification.contentType(ContentType.JSON);
				Response response = requestSpecification.put(Utility.getURI() + CONTEXT_PATH3);	
				System.out.println(response.getStatusCode());

			}


		}   
	}

}
