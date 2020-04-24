package com.qTestHSC.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.jayway.jsonpath.DocumentContext;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

public class Utility {

	static String CONTEXT_PATH="/test-runs";
	static String CONTEXT_PATH1= "/test-cases/{test-cases}";
	static String CONTEXT_PATH2= "/test-runs/{test-runsId}/test-logs";
	static String excelPath = "/Users/harjeetkaur/eclipseWorkspace1/qTestApi/payload/allData.xlsx";	

	/**
	 * @param excelPath
	 * @param header
	 * @param sheetName
	 * @return
	 */

	public static Map<String, List<String>> getAllcolumnValuesByHeader(String excelPath, String sheetName, String... headers)  {

		Map<String, List<String>> mp = new LinkedHashMap<String, List<String>>();

		for (String header : headers) {				
			ArrayList<String> allColumnVal = new ArrayList<String>();

			try {		
				FileInputStream fis = new FileInputStream(excelPath);			
				Workbook workbook = new XSSFWorkbook(fis);
				Sheet sheet = workbook.getSheet(sheetName);
				Row header_row = sheet.getRow(0);
				short cell_count = header_row.getLastCellNum();
				int cellNo=0;
				for (int i = 0; i < cell_count; i++) {
					Cell header_cell = header_row.getCell(i);
					String   excelHeader = header_cell.getStringCellValue();	
					if(header.equals(excelHeader))	{
						cellNo=i;
						break;
					}
				}

				for (int i = 1; i <= sheet.getLastRowNum(); i++) {
					Row rw = sheet.getRow(i);
					Cell cell = rw.getCell(cellNo);
					String cellValue = cell.getStringCellValue(); 
					allColumnVal.add(cellValue);
					System.out.println(cellValue); 
				}
			}

			catch (FileNotFoundException f) {
				f.printStackTrace();
			}	
			catch (IOException e) {

				e.printStackTrace();
			}

			mp.put(header, allColumnVal);  

		}

		return mp;
	}


	/**
	 * This method is returning Base URI from excel
	 * @return
	 * @throws IOException
	 */

	public static String getURI() {
		List<String> url = getAllcolumnValuesByHeader(excelPath, "Configuration", "Url").get("Url");
		String uri = url.get(0); 
		List<String> allProjectsId = getAllcolumnValuesByHeader(excelPath, "Configuration", "Project ID" ).get("Project ID");
		String projectId = allProjectsId.get(0);
		String uRI= uri + "/" + projectId;
		return uRI;
	}


	/**
	 * This method returns map of all Id's from Test suite API
	 * @return
	 */

	public static Map<String, List<Integer>> getApiIds()  {
		String newURI = getURI() + CONTEXT_PATH;			
		List<String> token = getAllcolumnValuesByHeader(excelPath, "Configuration", "Auth").get("Auth");	
		List<String> allTestSuitesId = getAllcolumnValuesByHeader(excelPath, "Configuration", "Test Suite ID").get("Test Suite ID");	
		String testSuiteId = allTestSuitesId.get(0).trim();
		int parentIdValue = Integer.parseInt(testSuiteId);		
		RequestSpecification requestSpecification= RestAssured.given().log().all().
				queryParam("parentId", parentIdValue).queryParam("parentType", "test-suite").
				headers("Authorization",token);
		requestSpecification.contentType(ContentType.JSON);
		Response response = requestSpecification.get(newURI);		
		System.out.println(response.getStatusCode());
		JsonPath testRunId= new JsonPath(response.asString());
		List<Integer> listTestRun = testRunId.getList("id");

		String jsonArray = response.andReturn().getBody().asString();
		DocumentContext jsonContext = com.jayway.jsonpath.JsonPath.parse(jsonArray);
		List<Integer> listTestCaseParentId = jsonContext.read("$[*]..test_case.id");


		List<Integer> TcIdList= new ArrayList<Integer>();				
		for(int i=0; i<listTestCaseParentId.size(); i++) {
			int tcId= listTestCaseParentId.get(i);             
			RequestSpecification requestSpec= RestAssured.given().log().all().
			pathParam("test-cases", tcId).headers("Authorization",token);
			requestSpec.contentType(ContentType.JSON);			
			Response res = requestSpec.get(getURI() + CONTEXT_PATH1);
			DocumentContext jsonCon = com.jayway.jsonpath.JsonPath.parse(res.asString());
			List<Integer> listofIds = jsonCon.read("$..pid");
			TcIdList.addAll(listofIds);
		}

		List<Integer> testLogIdlist= new ArrayList<Integer>();
		for(int i=0; i<listTestRun.size(); i++) {
			int trId= listTestRun.get(i);              
			System.out.println("Pinting tRId: "+trId);
			RequestSpecification requestSpec= RestAssured.given().log().all().
					pathParam("test-runsId", trId).headers("Authorization",token);
			requestSpec.contentType(ContentType.JSON);
			Response res = requestSpec.get(getURI()+ CONTEXT_PATH2);
			DocumentContext jsonCon = com.jayway.jsonpath.JsonPath.parse(res.asString());
			List<Integer> listofTLIds = jsonCon.read("$.items[*].id");
			testLogIdlist.addAll(listofTLIds);	   
		}
	
		
		Map<String, List<Integer>> mapId= new LinkedHashMap<String, List<Integer>>();
		mapId.put("testRunId", listTestRun);
		mapId.put("testCaseParentId", listTestCaseParentId);
		mapId.put("testCaseId", TcIdList);
		mapId.put("testLogId", testLogIdlist);

		return mapId;

	}
	
	/**
	 * This method giving API status code which required to make JSON payload for PUT call
	 * @return
	 */

	public static HashMap<String, String> apiStatusCode()  {
		HashMap<String, String> statusMap = new LinkedHashMap<String, String>(); 
		statusMap.put("PASS" , "601");
		statusMap.put("FAIL","602");
		statusMap.put("INCOMPLETE","603");
		statusMap.put("BLOCK","604");
		statusMap.put( "UNEXECUTED", "605");
		statusMap.put("NEED RETESTED","1563854");
		statusMap.put("PASSED WITH EXCEPTION","1563855");
		statusMap.put("NOT PLANNED", "1570048");
		return statusMap;
		
	}


}
