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

public class DBUtils {

	
	private static final String ELEMENT_RESULT_SET = "ResultSet";

	private static final String ATTRIBUTE_VERSTION = "version";

	private static Logger LOGGER = Logger.getLogger(DBUtils.class);

	public static final String ELEMENT_ROW = "Row";

	public static final String ELEMENT_SQLSTATEMENT = "sqlStatement";

	/**
	 * Get Requred parameter: Pool = The SQL pool to run against Requred parameter: cs = The stored
	 * procedure to execute XPath: //external
	 * 
	 * @param context
	 *            Request
	 * @return Document
	 * 
	 */
	public static Document get(RequestContext context) {

		try {

			long startTime = System.currentTimeMillis();
			// create new document
			Document doc = Dom4jUtils.newDocument("<external/>");
			String pool = context.getParameterString("Pool");
			if (null == pool || pool.length() <= 0 || !(pool.matches("[a-zA-Z]*")))
				return ExternalUtils.newErrorDocument("executeSqlQuery: Parameter 'pool' not found.");
			// check we've got a stored procedure
			String sqlStatement = context.getParameterString("cs");
			if (null == sqlStatement || sqlStatement.length() <= 0 || !(sqlStatement.matches("[a-zA-Z0-9]")))
				return ExternalUtils.newErrorDocument("executeSqlQuery: Parameter 'cs' not found.");
			Element siteElem = doc.getRootElement().addElement("site");
			siteElem.addAttribute("name", context.getSite().getName());
			siteElem.addAttribute("path", context.getSite().getPath());
			siteElem.addAttribute("branch", context.getSite().getBranch());
			sqlStatement = getSQLStatement(context.getParameters(), sqlStatement, "sproc", "formatted");
			if(sqlStatement ==null) return ExternalUtils.newErrorDocument("Invalid parameters");
			Document resultSets = runCS(pool, sqlStatement);
			doc.getRootElement().add(resultSets.getRootElement().detach());
			LOGGER.warn("Execution Time: " + (System.currentTimeMillis() - startTime) + "ms");
			//return WebUtils.exposeData(context, doc);
			return doc;

		} catch (Exception e) {
			LOGGER.error("An unhandled exception was thrown." +  e);
			return ExternalUtils.newErrorDocument("An unhandled exception was thrown.", e);
		}
	}
	
	/**
	 * getSQL Requred parameter: Pool = The SQL pool to run against Requred parameter: cs = The sql
	 * query to execute XPath: //external
	 * 
	 * @param context
	 *            Request
	 * @return Document
	 */
	public static Document getSQL(RequestContext context) {

		try {

			long startTime = System.currentTimeMillis();
			// create new document
			LOGGER.warn("--------------------------------------------------------------------------------");
			LOGGER.warn("START: getSQL");
			LOGGER.warn("--------------------------------------------------------------------------------");
			Document doc = Dom4jUtils.newDocument("<external/>");
			String pool = context.getParameterString("Pool");
			String formatted = context.getParameterString("formatted");
			String removePAGE_LINK = context.getParameterString("stripPageLink");
			boolean removePageLink = false;
			if ((formatted == null) || (formatted.length() == 0)) {
				formatted = "formatted";
			}
			if (null == pool || pool.length() <= 0 || !(pool.matches("[a-zA-Z]*"))){
				pool = "production";
			}
			
			if (removePAGE_LINK != null && !removePAGE_LINK.equals("")) {
				removePageLink = true;
			}
			// check we've got a SQL string
			/*String sqlStatementName = context.getParameterString("cs");
			if (null == sqlStatementName || sqlStatementName.length() <= 0 && !(sqlStatementName.matches("[a-zA-Z]*")))
				return ExternalUtils.newErrorDocument("executeSqlQuery: Parameter 'cs' not found.");*/
			
			/*String sqlStatement = new StoredSQLStatements().getStatement(sqlStatementName);
			if (null == sqlStatement)
				return ExternalUtils.newErrorDocument("executeSqlQuery: Parameter 'cs' not found."); */
			
			Element siteElem = doc.getRootElement().addElement("site");
			siteElem.addAttribute("name", context.getSite().getName());
			siteElem.addAttribute("path", context.getSite().getPath());
			siteElem.addAttribute("branch", context.getSite().getBranch());
			
			ParameterHash ph = context.getParameters();
			if (ph == null)
				return ExternalUtils.newErrorDocument("Invalid parameters");
			
			String iwSiteName = context.getSite().getName();
			LOGGER.warn("iwSiteName: " + iwSiteName);
			String iwSitePath = "/" + context.getSite().getPath().replaceFirst("//.*?/", "");
			LOGGER.warn("iwSitePath: " + iwSitePath);
			ph.put("IWOV_SITE", iwSiteName);
			ph.put("IWOV_SITE_PATH", "/sites/" + iwSiteName);
			String page = context.getParameterString("iw-ls-page-name");
			
			String sqlStatement = "SELECT pg.HEADING, pg.Title, pg.Keywords FROM PAGES pg WHERE pg.PATH LIKE '%"+"sites/" + iwSiteName+"/"+page+".page'";
			
			LOGGER.warn("SQL Query: " + sqlStatement);
			
			/*if (formatted.equalsIgnoreCase("raw")) {
				LOGGER.warn("Passing raw SQL to getSQLStatement");
				sqlStatement = getSQLStatement(ph, sqlStatement, "sqlstring", "raw");
				if(sqlStatement ==null) return ExternalUtils.newErrorDocument("Invalid parameters");
			} else {
				LOGGER.warn("Passing formatted SQL to getSQLStatement");
				sqlStatement = getSQLStatement(ph, sqlStatement, "sqlstring", "formatted");
				if(sqlStatement ==null) return ExternalUtils.newErrorDocument("Invalid parameters");
			}
			if (removePageLink) {
				String overridePageRegex = "^\\$PAGE_LINK\\[(.+)\\]$";
				Pattern overridePagePattern = Pattern.compile(overridePageRegex);
				Matcher overridePageMatcher = overridePagePattern.matcher(sqlStatement);
				while (overridePageMatcher.find()) {
					if (overridePageMatcher.group(1) != null) {
						sqlStatement = overridePageMatcher.group(1);
					}
				}
			}*/
			
			LOGGER.warn("Final SQL: " + sqlStatement);
			Document resultSets = runSQL(pool, sqlStatement);
			doc.getRootElement().add(resultSets.getRootElement().detach());
			LOGGER.warn("--------------------------------------------------------------------------------");
			LOGGER.warn("END: getSQL");
			LOGGER.warn("--------------------------------------------------------------------------------");
			LOGGER.warn("Execution Time: " + (System.currentTimeMillis() - startTime) + "ms");
			//return WebUtils.exposeData(context, doc);
			return doc;

		} catch (Exception e) {
			LOGGER.error("An unhandled exception was thrown." +  e);
			return ExternalUtils.newErrorDocument("An unhandled exception was thrown.", e);
		}

	}
	
	
	/**
	 * 
	 * runCS - runs the stored procedure and gets the results as XML XPath: //ResultSets
	 * 
	 * @param String
	 *            pool
	 * @param String
	 *            sqlStatement
	 * @return Document
	 */
	public static Document runCS(String pool, String sqlStatement) {
		long startTime = System.currentTimeMillis();
		// declare document for return
		Document doc = Dom4jUtils.newDocument("<ResultSets/>");
		doc.getRootElement().addAttribute(ATTRIBUTE_VERSTION, "1.1");
		doc.getRootElement().addAttribute(ELEMENT_SQLSTATEMENT, sqlStatement);
		Connection conn = null;
		CallableStatement cs = null;
		// Try to connect to the database
		conn = RequestUtils.getConnection(pool);
		try {
			// Execute the sql query and put the results in a resultset
			cs = conn.prepareCall(sqlStatement);
			// execute stored procedure
			if (cs.execute()) {
				do {
					ResultSet rs = cs.getResultSet();
					Element resultSet = doc.getRootElement().addElement(ELEMENT_RESULT_SET);
					ResultSetMetaData rsmd = rs.getMetaData();
					ArrayList columnNames = new ArrayList();
					// Get the column names; column indices start from 1
					for (int i = 1; i < rsmd.getColumnCount() + 1; i++) {
						String columnName = rsmd.getColumnName(i);
						columnNames.add(columnName);
					}
					// Loop through the result set
					while (rs.next()) {
						Element row = resultSet.addElement(ELEMENT_ROW);
						for (int j = 0; j < columnNames.size(); j++) {
							Element columnNode = row.addElement(columnNames.get(j).toString());
							String val = rs.getString(columnNames.get(j).toString());
							if (val != null && !val.equals("")) {
								columnNode.addCDATA(val);
							}
						}
					}
					rs.close();
				} while (cs.getMoreResults() != false);
			}
		} catch (SQLException e) {
			// TODO - add error note
			ExternalUtils.newErrorDocument("SQLException error: " + e.toString());
		} catch (Exception e) {
			ExternalUtils.newErrorDocument("Error: " + e.toString());
		} finally {
			// Close all open connections etc.
			if (cs != null) {
				try {
					cs.close();
				} catch (SQLException e) {
					ExternalUtils.newErrorDocument("SQLException error while closing: " + e.toString());
				}
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					ExternalUtils.newErrorDocument("SQLException error while closing: " + e.toString());
				}
			}
		}
		LOGGER.warn("Execution Time: " + (System.currentTimeMillis() - startTime) + "ms");
		// Return the results
		return doc;
	}
	
	
	/**
	 * 
	 * runSQL - runs the SQL and gets the results as XML XPath: //ResultSets
	 * 
	 * @param String
	 *            pool
	 * @param String
	 *            sqlStatement
	 * @return Document
	 */
	public static Document runSQL(String pool, String sqlStatement) {
		long startTime = System.currentTimeMillis();
		// declare document for return
		Document doc = Dom4jUtils.newDocument("<ResultSets/>");
		Element rootElement = doc.getRootElement();
		rootElement.addAttribute(ATTRIBUTE_VERSTION, "1.1");
		rootElement.addAttribute(ELEMENT_SQLSTATEMENT, sqlStatement);
		Connection conn = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
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
			
			if (!preparedStatement.execute()) {
				LOGGER.fatal("SQL execute FAIL: " + sqlStatement);
			} else {
				LOGGER.warn("SQL execute OK");
				int iteration = 0;
				do {
					resultSet = preparedStatement.getResultSet();
					LOGGER.warn("Got resultset, iteration: " + iteration++);
					Element resultSetElement = rootElement.addElement(ELEMENT_RESULT_SET);
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
						Element row = resultSetElement.addElement(ELEMENT_ROW);
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
			return ExternalUtils.newErrorDocument("SQLException error: " + e.toString());
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
		LOGGER.warn("Execution Time: " + (System.currentTimeMillis() - startTime));
		return doc;
	}

	/**
	 * This is simple convenience method to close a database connection and ignore an exception that
	 * is thrown if the connection is not open. If you pass null instead of a connection instance,
	 * the method will simply return.
	 * 
	 * @param conn
	 *            The connection instance.
	 */
	private static void closeConnection(Connection conn) {
		if (conn == null)
			return;
		try {
			conn.close();
		} catch (SQLException sqlEx) {
			LOGGER.warn("While closing the connection an exception occurred: " + sqlEx.getMessage());
		}
		;
	}

	
	/**
	 * method to validate input string value for select / create / update SQL 
	 * 
	 */
	private static boolean validateinput(String inputStr){
		boolean validInput = true;

		Pattern p = Pattern.compile("(?i)(select|create|update)");
		Matcher m = p.matcher(inputStr);
		Matcher SubM = null;
		
		if(m.find()){
		String subTxt = inputStr.substring(m.end());
		Pattern subP = Pattern.compile("(?i)(from)");
		SubM = subP.matcher(subTxt);
		}
         if(m.find() && SubM.find())
    	 {
        	 validInput=false;
        	 LOGGER.warn(" invalid input -- some sql found in input ---"+inputStr);
      	 }
		return validInput;
		
	}
	
	/**
	 * getSQLStatement - creates the SQL statement from the statement passed in and by substituting
	 * all occurences of {PARAMNAME} with the value of the parameter
	 * 
	 * @param context
	 *            Request
	 * @return Document
	 */
	public static String getSQLStatement(ParameterHash params, String sqlStatement, String type, String method) {
		long startTime = System.currentTimeMillis();
		LOGGER.warn("    Start getSQLStatement");
		//sqlStatement = parseForExpansions(sqlStatement, params);
		LOGGER.warn("    parseForExpansions returned: " + sqlStatement);
		String pageLinkRegex = "^\\$PAGE_LINK\\[(.+)\\]$";
		Pattern pageLinkPattern = Pattern.compile(pageLinkRegex);
		Matcher pageLinkMatcher;
		// get iterator of params
		Iterator it = params.keySet().iterator();
		while (it.hasNext()) {
			String key = it.next().toString();
			LOGGER.warn("    Param key: " + key);
			Object value = params.get(key);
			// prepare the parameters
			if (value == null) {
				LOGGER.warn("    value is null");
				// no param for this found so forget it
			} else if (value instanceof ArrayList) {
				LOGGER.warn("    value is ArrayList");
				ArrayList vals = (ArrayList) value;
				String newString = "";
				for (int i = 0; i < vals.size(); i++) {
					if (i > 0) {
						newString += ",";
					}
					newString += (String) vals.get(i);
				}
				pageLinkMatcher = pageLinkPattern.matcher(newString);
				while (pageLinkMatcher.find()) {
					if (pageLinkMatcher.group(1) != null) {
						newString = pageLinkMatcher.group(1);
					}
				}
				LOGGER.warn("    pageLinkMatcher regex returned: " + newString);
				// newString.replaceAll( "'", "''" ) - replaces the freakin
				// single quote to be two - a sql thing
				if (method.equalsIgnoreCase("raw")) {
					LOGGER.warn("    raw sqlStatement pre-repleace: " + sqlStatement);
					sqlStatement = sqlStatement.replaceFirst(("\\{" + key + "\\}"), newString);
					LOGGER.warn("    raw sqlStatement post-replace: " + sqlStatement);
				} else {
					LOGGER.warn("    formatted sqlStatement pre-repleace: " + sqlStatement);
					sqlStatement = sqlStatement.replaceFirst(("\\{" + key + "\\}"), "'" + newString.replaceAll("'", "''") + "'");
					LOGGER.warn("    formatted sqlStatement post-replace: " + sqlStatement);
				}
			} else if (!(value.toString().equalsIgnoreCase("Pool") || value.toString().equalsIgnoreCase("cs"))) {
				LOGGER.warn("    value is String");
				// newString.replaceAll( "'", "''" ) - replaces the freakin
				// single quote to be two - a sql thing
				String valueString = value.toString();
				LOGGER.warn("    validating input");
				if(!validateinput(valueString)) {sqlStatement= null; break;}
				LOGGER.warn("    validated input");
				pageLinkMatcher = pageLinkPattern.matcher(valueString);
				while (pageLinkMatcher.find()) {
					if (pageLinkMatcher.group(1) != null) {
						valueString = pageLinkMatcher.group(1);
					}
				}
				LOGGER.warn(valueString);
				if (method.equalsIgnoreCase("raw")) {
					sqlStatement = sqlStatement.replaceFirst(("\\{" + key + "\\}"), valueString);
				} else {
					sqlStatement = sqlStatement.replaceFirst(("\\{" + key + "\\}"), "'" + valueString.replaceAll("'", "''") + "'");
				}
			}
		}
		if (!method.equalsIgnoreCase("raw")) {
			// replace any unmatched params with nulls
			sqlStatement = sqlStatement.replaceAll("\\{(.*?)\\}", "null");
		}
		if (type.equals("sproc")) {
			sqlStatement = "{call " + sqlStatement + "}";
		}

		LOGGER.warn("Execution Time: " + (System.currentTimeMillis() - startTime) + "ms");
		return sqlStatement;
	}

	/*
	 * @author a408015 APH
	 * @param sqlStatement
	 *            The SQL from the Datum
	 * @param hash
	 *            The parameters passed in
	 * @return
	 * @see getSQLStatement
	 * @see "See SQLUtilsTest Test cases and examples"
	 */
	/*public static String parseForExpansions(String sqlStatement, ParameterHash hash) {
		long startTime = System.currentTimeMillis();
		LOGGER.info("Passed sqlStatement of " + sqlStatement);
		// Create a string buffer that will ultimately become the SQL statement
		// passed back
		StringBuffer outputSQL = new StringBuffer();
		// Look for special expansions in the form of {GROUPINGOPERATOR
		// COLUMNNAME:DATUMNAME}
		// eg: {and PATH:myDatumName}
		// Pattern expansionPattern = Pattern.compile("\\{[a-zA-Z._]*?:.*?\\}");
		Pattern expansionPattern = Pattern.compile("\\{(and\\s)?(.[^\\}]*?):{1}(.*?)?\\}");
		Matcher expansionMatcher = expansionPattern.matcher(sqlStatement);
		// Section: Loop though all the matching occurances of the above regex
		// processing the expansions.
		while (expansionMatcher.find()) {
			// Get the positions of the start and end points in the string
			int start = expansionMatcher.start();
			int end = expansionMatcher.end();
			// Get the expansion expression (ie, everything in between the curly
			// braces)
			String expansionContent = sqlStatement.subSequence(start + 1, end - 1).toString();
			LOGGER.warn("expansionContent is: " + expansionContent);
			// Get the TABLE.COLUMN part (everything to the left of the colon)
			String leftPart = expansionContent.split(":")[0];
			// Assume the left part does not contain the grouping operator for
			// now...
			String columnName = leftPart;
			// Declare a grouping operator, this will be the operator that will
			// sit before the grouped OR values of the select datum.
			// Just define a blank string for now.
			String groupingOperator = "";
			// Look to see if the expression contains the grouping operator eg
			// {and TABLE....
			if (leftPart.indexOf(" ") > 0) {
				groupingOperator = leftPart.split(" ")[0]; // this would be
				// something like
				// 'and' or 'or'
				columnName = leftPart.split(" ")[1];
				LOGGER.warn("Operator for column grouping " + columnName + " is " + groupingOperator);
			}
			// Get the Datum name part (everything to the right of the colon)
			String datumName = expansionContent.split(":")[1];
			// Setup blanks to support the LIKE statements
			String startWild = "";
			String endWild = "";
			// Flag to determine if the values should be quoted
			boolean quote = false;
			// If the datumname part starts and ends with a quote set the quote
			// flag...
			if (datumName.startsWith("'") && datumName.endsWith("'")) {
				quote = true;
			}
			// then remove the quotes
			datumName = datumName.replaceAll("'", "");
			// Check for wild cards, then remove them
			if (datumName.startsWith("%")) {
				startWild = "%";
			}
			if (datumName.endsWith("%")) {
				endWild = "%";
			}
			datumName = datumName.replaceAll("%", "");
			LOGGER.warn("datumName: " + datumName);
			// Use = as the operator except if there are wildcards (%) then use
			// LIKE
			String operator = (startWild.equals("") && endWild.equals("") ? "=" : "LIKE");
			// At this point multipleSelectDatumName should now just be the pure
			// name of the datum
			LOGGER.warn("Extracted from source: TABLE:COL=" + columnName + ", DATUMNAME=" + datumName);
			// Get the list of selections from the ParameterHash using the
			// datumname as a key
			// ArrayList selections = (ArrayList)
			// hash.get(multipleSelectDatumName); // Supports ONLY
			// SelectMultiples
		//	ArrayList selections = WebUtils.getReplicatedParameter(hash, datumName); // For
			// support
			// for
			// single
			// datums
			// AND
			// multiples
			// Section: Get rid of any empty expansions and skip on to the next
			// expansion
			// Should nothing be found, moan and skip onto the next match
			if (null == selections) {
				LOGGER.warn(datumName + " could not be looked up, check the Datums and the SQL statement. Expansion will be removed.");
				expansionMatcher.appendReplacement(outputSQL, "");
				continue;
			}
			// Skip onto the next match if no selections were made - ignore this
			// datum value and strip out of the SQL.
			if (selections.size() == 0) {
				LOGGER.warn(datumName + " has no values. Expansion will be removed.");
				expansionMatcher.appendReplacement(outputSQL, "");
				continue;
			}
			// This could be a single valued datum with a possible empty string
			// which is pretty useless
			// so ignore this datum value and strip out of the SQL.
			if (selections.size() == 1) {
				String single = (String) selections.get(0);
				if ("".equals(single)) {
					LOGGER.warn(datumName + " is an empty string. Expansion will be removed.");
					expansionMatcher.appendReplacement(outputSQL, "");
					continue;
				}
			}
			// Section: Start doing the expansion replacement in the input
			// string
			// Create a String buffer for this expansion which will be used to
			// build up the SQL snippet for this datum
			StringBuffer tempSQL = new StringBuffer();
			// Loop through the selections
			for (Iterator iterator = selections.iterator(); iterator.hasNext();) {
				String selection = (String) iterator.next();
				// Wrap with wildcards where appropriate
				selection = startWild + selection + endWild;
				// Wrap with single quotes if appropriate
				if (quote) {
					selection = "'" + selection + "'";
				}
				// Append on this section of SQL to the string buffer separated
				// by OR clauses
				tempSQL.append(columnName + " " + operator + " " + selection + "" + (iterator.hasNext() ? " OR " : ""));
			}
			// Finally swap out the raw expansion from the SQL datum string
			// tucked away in the expansionMatcher
			// and replace with the built SQL snippet wrapping in brackets.
			expansionMatcher.appendReplacement(outputSQL, groupingOperator + " (" + tempSQL.toString() + ")");
		}
		// Copy in the remainder of the original input SQL after all the
		// expansions
		expansionMatcher.appendTail(outputSQL);
		// Redefine the incoming SQL statement with the new expanded version
		// also
		// doing a little bit of hacky double operator cleanup caused by
		// removing expansions
		// bleugghh, sorry (this problem should be minimised by using the
		// preferred syntax though).
		sqlStatement = outputSQL.toString().replaceAll("and(\\s+and)+", "and").replaceAll("AND(\\s+AND)+", "AND").replaceAll("\\s+", " ");
		LOGGER.info("Transformed SQL: " + sqlStatement);
		LOGGER.warn("Execution Time: " + (System.currentTimeMillis() - startTime) + "ms");
		return sqlStatement;
	}*/
	

}
