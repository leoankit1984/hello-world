package com.utils.externals;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;

import com.interwoven.livesite.dom4j.Dom4jUtils;
import com.interwoven.livesite.runtime.RequestContext;
import com.interwoven.livesite.runtime.servlet.RequestUtils;

public class DocumentList {
	

	private static Logger LOGGER = Logger.getLogger(DocumentList.class);
	
	public static Document getSQL(RequestContext context) {

		Document doc = Dom4jUtils.newDocument("<ResultSet/>");
		long startTime = System.currentTimeMillis();
		
		try {
			
			// create new document
			String chosenCat = context.getParameterString("chosenCategory");
			String display = context.getParameterString("display");
			String sortby = context.getParameterString("sortby");
			String pageNum = context.getParameterString("pageNum");
			
			LOGGER.warn("Value of chosenCat is :: " + chosenCat);
			
			if (null == chosenCat || chosenCat.length() <= 0 || !(chosenCat.matches("[a-zA-Z ]*")))
			{
				chosenCat  = "all";
			}
			
			if (null == display)
			{
				display  = "10";
			}
			
			if (null == sortby || sortby.length() <= 0 || !(sortby.matches("[a-zA-Z]*")))
			{
				sortby  = "none";
			}
			
			if (null == pageNum)
			{
				pageNum  = "1";
			}
			
			Element rootElement = doc.getRootElement();
			
			Element choseCat = rootElement.addElement("ChosenCategory");
			choseCat.setText(chosenCat);
			
			Element disp = rootElement.addElement("Display");
			disp.setText(display);
			
			Element sort = rootElement.addElement("SortBy");
			sort.setText(sortby);
			
			Element page = rootElement.addElement("PageNumber");
			page.setText(pageNum);
			
			String pool = context.getParameterString("Pool");
			if (null == pool || pool.length() <= 0 || !(pool.matches("[a-zA-Z]*")))
			{
				pool  = "production";
			}
			
			String site = context.getSite().getName();
			LOGGER.warn("Site Name " + site);
			
			String iwSitePath = "/" + context.getSite().getPath().replaceFirst("//.*?/", "");
			LOGGER.warn("iwSitePath: " + iwSitePath);
			
			String pages = context.getParameterString("PageList");
			LOGGER.warn("PageList param in Request : " + pages);
			
			String str[] = pages.split(",");			
			String [] newList = new String[str.length];
			
			
			for (int i = 0; i < str.length; i++) {
				LOGGER.warn("Array Element " + i + " : " + str[i]);
				String tmp = str[i].toString();
				String tmp1;
				
				
				tmp.replaceAll("\\s+", "");
				if(tmp.matches("(?i).*PAGE_LINK.*"))
				{
					LOGGER.warn("Match found for page link");
					tmp1 = tmp;
					int idx1 = tmp1.indexOf('[');
					int idx2 = tmp1.indexOf(']');
					tmp1 = tmp1.substring(idx1+1, idx2);
					if(tmp1.matches("(?i).*PAGE_LINK.*"))
					{
						idx1 = tmp1.indexOf('[');
						idx2 = tmp1.length();
						tmp1 = tmp1.substring(idx1+1, idx2);
					}
					
					/*tmp1.replaceAll("(?i).*PAGE_LINK.*", "");
					tmp1.replace("[", "");
					tmp1.replace("]", ""); */
					LOGGER.warn("Extracted string : " + tmp1);					
					
					if (context.isPreview())
					{
						tmp1 = iwSitePath + "/" + tmp1 + ".page";
					}
					else
					{
						tmp1 = "sites/" + site + "/" + tmp1 + ".page";
					}
					
				}
				else
				{
					tmp1 = tmp;
				}
				LOGGER.warn("Page in Page List : " + tmp1);
				
				newList[i] = tmp1;
			}
			
			Document resultSets = runSQL(pool, newList, context, chosenCat);
			doc.getRootElement().add(resultSets.getRootElement().detach());
			LOGGER.warn("Execution Time: " + (System.currentTimeMillis() - startTime) + "ms");			
			

		} catch (Exception e) {
			LOGGER.error("An unhandled exception was thrown." +  e);
			Element rootElement = doc.getRootElement();
			Element ErrorElement = rootElement.addElement("Error");
			String error = "" + e + "";
			ErrorElement.setText(error);
			
		}
		
		return doc;
		
	}
	
	/* Fetch Metadata of pages
	 * 
	 */
		  
		public static Document runSQL(String pool, String [] pageList, RequestContext context, String chosenCat) {
			  
			  	Document doc = Dom4jUtils.newDocument("<PageMetadata/>");
			  	Element rootElement = doc.getRootElement();
			  				  	
			  	Connection conn = null;
				PreparedStatement preparedStatement = null;
				ResultSet resultSet = null;
				
				String cat = null;
				
				LOGGER.warn("Value of chosenCat in runSQL is :: " + chosenCat);
				
				for (int k = 0; k < pageList.length; k++) {
					
				
				String sqlStatement = "Select p.path,p.title,p.pageicon,p.summary,p.keywords,p.description,p.CreationDate,p.author,p.product_category, avg(r.rating) as AvgRating, count(r.rating) as TotalRating from PAGES p LEFT JOIN Ratings r ON p.path=r.path where p.path like '%"+pageList[k]+"'";
				if(!chosenCat.equalsIgnoreCase("all")){
					
					sqlStatement += " and product_category like ?";
					
				}
				
				LOGGER.warn("SQL Query :  " + sqlStatement);
				try {
					// Try to connect to the database
					LOGGER.warn("Attempting connect to pool " + pool);
					conn = RequestUtils.getConnection(pool);
					
					LOGGER.debug("Connected to pool " + pool);
					
					LOGGER.info("Before SQL prepare: " + sqlStatement);
					
					preparedStatement = conn.prepareStatement(sqlStatement);
					preparedStatement.setQueryTimeout(30);
					if(!chosenCat.equalsIgnoreCase("all")){						
						
						preparedStatement.setString(1, "%" + chosenCat);
						
					}
					
					LOGGER.warn("After SQL prepare");
					LOGGER.warn("Before SQL execute");
					int count = 0;
					double rate=0;
					double avg = 0;
					String ct;
					
					if (!preparedStatement.execute()) {
						LOGGER.fatal("SQL execute FAIL: " + sqlStatement);
					} else {
						LOGGER.warn("SQL execute OK");
						int iteration = 0;
						int titleIdx = 0;
						do {
							resultSet = preparedStatement.getResultSet();
							LOGGER.warn("Got resultset, iteration: " + iteration++);
							Element resultSetElement = rootElement.addElement("Page");
							ResultSetMetaData metaData = resultSet.getMetaData();
							LOGGER.warn("Got resultset metadata");
							int columnCount = metaData.getColumnCount();
							
							LOGGER.warn(columnCount + " columns in result set");
							String[] columnNames = new String[columnCount];
							for (int i = 0; i < columnCount; i++) {
								int columnIndex = i + 1;
								LOGGER.warn("Getting name for column " + columnIndex);
								String columnName = metaData.getColumnName(columnIndex);
								if(columnName.equalsIgnoreCase("title")){
									titleIdx = columnIndex;
								}
								LOGGER.warn("Index of Title column :: " + titleIdx);
								LOGGER.warn("Column " + columnIndex + " name: " + columnName);
								columnNames[i] = columnName;
							}
							LOGGER.warn("Got all columns - total count: " + columnNames.length);
							metaData = null;
							
							
							while (resultSet.next()) {
								
			if((resultSet.getString(titleIdx) != null) && (resultSet.getString(titleIdx) != "")){
									
										
								LOGGER.warn("Got row");
								Element row = resultSetElement.addElement("Row");
								for (int i = 0; i < columnNames.length; i++) {
									int columnIndex = i + 1;
									String columnName = columnNames[i];
									Element columnNode = row.addElement(columnName);
									LOGGER.warn("Fetching column '" + columnName + "' - index: " + columnIndex);
									String columnValue = resultSet.getString(columnIndex);
									
									LOGGER.warn("Column '" + columnName + "' - value: " + columnValue);
									if (columnValue != null && !columnValue.equals("")) {
										columnNode.addCDATA(columnValue);
										
											if (columnName.equalsIgnoreCase("Product_Category")) {
												if(null == cat){
													cat = columnValue;
												}
												else
												{
													if(!cat.contains(columnValue)){
														cat+=","+columnValue;
													}
													
													
												}												
											}
									}
									else
									{
										if(columnName.equalsIgnoreCase("avgrating"))
										{
											columnNode.addCDATA("0");
										}
										else if(columnName.equalsIgnoreCase("totalrating"))
										{
											columnNode.addCDATA("0");
										}
									}
									
								
								}
							}//if for resultset.getString
			else{
				LOGGER.warn("\n###########Removing empty <Page> element##########\n");
				rootElement.remove(resultSetElement);
			}
							}
							
							try {
								if (preparedStatement.getMoreResults(Statement.CLOSE_CURRENT_RESULT)) {
									continue;
								}
								break;
							} catch (SQLException e) {
								break;
							}
							
							
						} while (true);
						
					}
					
					
				} catch (SQLException e) {
					LOGGER.error(e);
					Element ErrorElement = rootElement.addElement("Error");
					String error = "" + e + "";
					ErrorElement.setText(error);
					
				} finally {
					// Close all open connections etc.
					if (null != resultSet) {
						try {
							resultSet.close();
						} catch (SQLException e) {
							LOGGER.error("Error closing result set", e);
						}
						resultSet = null;
					}
					if (preparedStatement != null) {
						try {
							preparedStatement.close();
						} catch (SQLException e) {
							LOGGER.error("Error closing prepared statement", e);
						}
						preparedStatement = null;
					}
					if (conn != null) {
						try {
							conn.close();
						} catch (SQLException e) {
							LOGGER.error("Error closing DB connection", e);
						}
						conn = null;
					}
				}
				}
				
				LOGGER.warn("All categories :: " +  cat);
				String pcat[] = cat.split(",");
				Element rootElem = doc.getRootElement();
				Element ProElement = rootElement.addElement("ProductCategories");
				
				for (int i = 0; i < pcat.length; i++) {
					Element CatElement = ProElement.addElement("Category");
					CatElement.setText(pcat[i]);
				}
				
				return doc;
		  }
	
}
