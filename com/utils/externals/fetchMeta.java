package com.utils.externals;

import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;

import com.interwoven.livesite.dom4j.Dom4jUtils;
import com.interwoven.livesite.external.ExternalUtils;
import com.interwoven.livesite.external.ParameterHash;
import com.interwoven.livesite.runtime.RequestContext;
import com.interwoven.livesite.runtime.servlet.RequestUtils;

public class fetchMeta {

	
	private static final String ELEMENT_RESULT_SET = "ResultSet";

	private static final String ATTRIBUTE_VERSTION = "version";

	private static Logger LOGGER = Logger.getLogger(fetchMeta.class);

	public static final String ELEMENT_ROW = "Row";

	public static final String ELEMENT_SQLSTATEMENT = "sqlStatement";

	
	public static Document getResults(RequestContext context) {


		long startTime = System.currentTimeMillis();
		// create new document
		Document doc = Dom4jUtils.newDocument("<ResultSet/>");
		
		try {

			String pool = context.getParameterString("Pool");
			if (null == pool || pool.length() <= 0 || !(pool.matches("[a-zA-Z]*")))
				pool = "production";
			
			Document meta;
			
			meta = getSQL("production", context);
			
			doc.getRootElement().add(meta.getRootElement().detach());
			

		} catch (Exception e) {
			LOGGER.warn("An unhandled exception was thrown." +  e);
			Element rootElement = doc.getRootElement();
			Element ErrorElement = rootElement.addElement("Error");
			String error = "" + e + "";
			ErrorElement.setText(error);
		}
		
		return doc;
	}
	
	/**
	 * getSQL Requred parameter: Pool = The SQL pool to run against Requred parameter: cs = The sql
	 * query to execute XPath: //external
	 * 
	 * @param context
	 *            Request
	 * @return Document
	 */
	public static Document getSQL(String pool, RequestContext context) {

		Document doc = Dom4jUtils.newDocument("<Metadata/>");
	  	Element rootElement = doc.getRootElement();
	  	Element pagePathElement = rootElement.addElement("PagePath");
	  	
	  	
	  	String iwSiteName = context.getSite().getName();
		LOGGER.warn("iwSiteName: " + iwSiteName);
		
		String iwSitePath = "/" + context.getSite().getPath().replaceFirst("//.*?/", "");
		LOGGER.warn("iwSitePath: " + iwSitePath);
		
		String lspage = context.getParameterString("iw-ls-page-name");
		String page;
		
		if (context.isPreview())
		{
			page = iwSitePath + "/" + lspage + ".page";
		}
		else
		{
			page = "sites/" + iwSiteName+"/"+lspage+".page";	
		}
			
		LOGGER.warn("Page path is : " + page);
		
		pagePathElement.addCDATA(page);
		
		Connection conn = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		
		String sqlStatement = "Select title,keywords,summary,description from PAGES where path like '%"+page+"'";
		try {
			// Try to connect to the database
			LOGGER.warn("Attempting connect to pool " + pool);
			conn = RequestUtils.getConnection(pool);
			
			LOGGER.debug("Connected to pool " + pool);
			
			LOGGER.info("Before SQL prepare: " + sqlStatement);
			
			preparedStatement = conn.prepareStatement(sqlStatement);
			preparedStatement.setQueryTimeout(30);
			
			LOGGER.warn("After SQL prepare");
			LOGGER.warn("Before SQL execute");
			LOGGER.warn("SQL Query ::" + sqlStatement);
			
			if (!preparedStatement.execute()) {
				LOGGER.fatal("SQL execute FAIL: " + sqlStatement);
			} else {
				LOGGER.warn("SQL execute OK");
				int iteration = 0;
				do {
					resultSet = preparedStatement.getResultSet();
					LOGGER.warn("Got resultset, iteration: " + iteration++);
					Element resultSetElement = rootElement.addElement("PageInfo");
					ResultSetMetaData metaData = resultSet.getMetaData();
					LOGGER.warn("Got resultset metadata");
					int columnCount = metaData.getColumnCount();
					
					LOGGER.warn(columnCount + " columns in result set");
					String[] columnNames = new String[columnCount];
					for (int i = 0; i < columnCount; i++) {
						int columnIndex = i + 1;
						LOGGER.warn("Getting name for column " + columnIndex);
						String columnName = metaData.getColumnName(columnIndex);
						LOGGER.warn("Column " + columnIndex + " name: " + columnName);
						columnNames[i] = columnName;
					}
					LOGGER.warn("Got all columns - total count: " + columnNames.length);
					metaData = null;
					
					
					while (resultSet.next()) {
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
							}
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
			Element ErrorElement = rootElement.addElement("ErrorMeta");
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
		
		return doc;
	}
}

