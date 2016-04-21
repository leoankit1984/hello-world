package com.utils.externals;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.http.Cookie;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;

import com.interwoven.livesite.dom4j.Dom4jUtils;
import com.interwoven.livesite.runtime.RequestContext;
import com.interwoven.livesite.runtime.servlet.RequestUtils;

public class RatingController {

	
	  private static final String PARAMETER_NAME_SUBMITTED_RATING = "submittedRating";
	  private static final String PARAMETER_NAME_ASSET_ID = "assetId";
	  private static final String PARAMETER_NAME_TARGET_ID_KEY = "targetIdKey";
	  private static final String PARAMETER_NAME_TARGET_ID_VALUE = "targetIdValue";
	  private static final String RATING_TYPE_LOGIN_URL = "loginUrl";
	  private static final String PARAMETER_NAME_RATING_SERVICE = "ratingService";
	  private static final String PARAMETER_NAME_ANONYMOUS_SUBMISSION_ALLOWED = "anonymousSubmissionAllowed";
	  private static final long serialVersionUID = 1L;
	  private static final String MESSAGE_AUTHENTICATION_REQUIRED = "User is not authenticated.";
	  private static final String MESSAGE_DUPLICATE_SUBMISSION = "User has already submitted a rating.";
	  
	  private static Logger LOGGER = Logger.getLogger(RatingController.class);
	  
	  public Document handleRequest(RequestContext context)
	  {
		  Document doc = Dom4jUtils.newDocument("<ResultSet/>");
	    
	    Document result;
	    Document allRating;
	    
	    Element loginElement = doc.getRootElement().addElement("Login");
        Element stat = loginElement.addElement("Status");
	    
	    String lc = null;
		
		if (context.getRequest().getCookies() != null) {
            Cookie cookies[] = context.getRequest().getCookies();
            for (int i = 0; i < cookies.length; i++) {
            	                	
                if (cookies[i].getName().equals("loginDetails")) {
                	lc = cookies[i].getValue();                    	
                	LOGGER.warn("login cookie :: " + lc);
                }
            }
        }
		
		if(null != lc && lc.length() > 0 && lc != "")
		{
			stat.addCDATA("true");
			String [] loginVals = lc.split(":");
			Element userid = loginElement.addElement("UserId");
        	userid.addCDATA(loginVals[0]);
        	
        	Element email = loginElement.addElement("Email");
        	email.addCDATA(loginVals[1]);
        	
        	Element fname = loginElement.addElement("Name");
        	fname.addCDATA(loginVals[2]);
        	
        	Element frname = loginElement.addElement("FirstName");
        	frname.addCDATA(context.getRequest().getHeader("first_name"));
        	
        	Element lsname = loginElement.addElement("LastName");
        	lsname.addCDATA(context.getRequest().getHeader("last_name"));
        	
		}
		else
		{
			stat.addCDATA("false");
		}
		
	    
	    if (getSubmittedRating(context) != null)
	    {
	      result = handleSubmission(context);
	      allRating = getAllRatings("production", context);
	      doc.getRootElement().add(allRating.getRootElement().detach());
	      
	    }
	    else
	    {
	      result = handleInitialRendering(context);
	      allRating = getAllRatings("production", context);
	      doc.getRootElement().add(allRating.getRootElement().detach());
	    }
	    
	    doc.getRootElement().add(result.getRootElement().detach());
	    return doc;
	  }
	  
	  protected Document handleInitialRendering(RequestContext context)
	  {
	    Document doc = Dom4jUtils.newDocument();
	    Element resultElement = doc.addElement("RatingResult");

	    return doc;
	  }
	  
	  protected Document handleSubmission(RequestContext context)
	  {
		  
		  
	    Double submittedRating = getSubmittedRating(context);
	   
	    String userId = context.getParameterString("userid");
	    String userName = context.getParameterString("username");	    
	    String path = context.getParameterString("pagePath");
	    String email = context.getParameterString("emailId");
	    String website = context.getParameterString("webSite");
	    long ratingDate = System.currentTimeMillis();
	    
	    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
	    //DateFormat dateFormat = new SimpleDateFormat("MMMM dd,yyyy HH:mm");
		//Date date = new Date();
		//System.out.println(dateFormat.format(date));
		
	    String pool = context.getParameterString("Pool");
	   
	    String message = null;
	    boolean success;
	    String sqlStatement = "INSERT INTO RATINGS"
				+ "(PATH, USERID, USERNAME, EMAIL, WEBSITE, RATING, RATING_DATE) VALUES"
				+ "(?,?,?,?,?,?,?)";
	    //String sqlStatement = "INSERT INTO RATINGS VALUES('" + path +"','"+userId+"','"+userName+"','"+email+"','"+website+"','"+submittedRating+"','"+dateFormat.format(date)+"' )";
	    LOGGER.warn("Insert quey is :: " +  sqlStatement);
	    
	    Document insertQueryResult = runInsertSql(pool,sqlStatement,path,userId,userName,email,website,submittedRating);
	   // LOGGER.warn("document returned by insert query  :: " +  insertQueryResult); 
	       
	        
	    return insertQueryResult;
	  }
	  
	  public static Document runInsertSql(String pool, String sqlStatement ,String path,String userId,String userName,String email,String website,Double submittedRating) {
			long startTime = System.currentTimeMillis();
			
			Document doc = Dom4jUtils.newDocument();
			Element submissionResultElement = doc.addElement("SubmissionResult");
		    Element submissionElement = submissionResultElement.addElement("QueryResult");   
		      
			
			// declare document for return
			
			Connection conn = null;
			PreparedStatement preparedStatement = null;
			ResultSet resultSet = null;
			int result ;
			String ret;
			
			try {
				// Try to connect to the database
				LOGGER.warn("Attempting connect to pool " + pool);
				conn = RequestUtils.getConnection(pool);
				
				LOGGER.debug("Connected to pool " + pool);
				
				LOGGER.info("Before SQL prepare: " + sqlStatement);
				DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
				Date date = new Date();				
				
				preparedStatement = conn.prepareStatement(sqlStatement);
				preparedStatement.setQueryTimeout(30);
				
				preparedStatement.setString(1, path);
				preparedStatement.setString(2, userId);
				preparedStatement.setString(3, userName);
				preparedStatement.setString(4, email);
				preparedStatement.setString(5, website);
				preparedStatement.setDouble(6, submittedRating);
				preparedStatement.setString(7, dateFormat.format(date));
				
				LOGGER.warn("After SQL prepare");
				LOGGER.warn("Before SQL execute");
				
				
				
				result = preparedStatement.executeUpdate();
								
				LOGGER.warn("Insert query Execute result : " + result);
				
				if(result == '0')
				{
					submissionElement.addAttribute("queryStatus", "failure");
				}
				else if(result == '1')
				{				
						submissionElement.addAttribute("queryStatus", "success");
				}
				else
				{
					submissionElement.addAttribute("queryStatus", "unknown");
				}
					
				
				
				
				/*if (!preparedStatement.execute()) {
					LOGGER.fatal("SQL execute FAIL: " + sqlStatement);
				} else {
					LOGGER.warn("SQL execute OK");
					int iteration = 0;
					*/
						resultSet = preparedStatement.getResultSet();
						LOGGER.warn("insert query resultset: " + resultSet);
						
					
				//}
				
			} catch (SQLException e) {
				LOGGER.warn("sql exception in runInsertsql function :: " + e);
				Element ErrorElement = submissionElement.addElement("Error");
				String error = "" + e + "";
				ErrorElement.setText(error);
				
			} finally {
				// Close all open connections etc.
				if (null != resultSet) {
					try {
						resultSet.close();
					} catch (SQLException e) {
						LOGGER.warn("Error closing result set", e);
					}
					resultSet = null;
				}
				if (preparedStatement != null) {
					try {
						preparedStatement.close();
					} catch (SQLException e) {
						LOGGER.warn("Error closing prepared statement", e);
					}
					preparedStatement = null;
				}
				//Close connection
				if (conn != null) {
					try {
						conn.close();
					} catch (SQLException e) {
						LOGGER.warn("Error closing DB connection", e);
					}
					conn = null;
				}
				
				
				
			}
			LOGGER.warn("Execution Time: " + (System.currentTimeMillis() - startTime));			
			
			return doc;
		}
	  
/* Fetch All Ratings for a page from Ratings Table
 * 
 */
	  
	public static Document getAllRatings(String pool, RequestContext context) {
		  
		  	Document doc = Dom4jUtils.newDocument("<AllRatings/>");
		  	Element rootElement = doc.getRootElement();
		  	Element pagePathElement = rootElement.addElement("PagePath");
		  	
		  	
		    String assetId;
		    
		  	String iwSiteName = context.getSite().getName();
			LOGGER.warn("iwSiteName: " + iwSiteName);
			
			String iwSitePath = "/" + context.getSite().getPath().replaceFirst("//.*?/", "");
			LOGGER.warn("iwSitePath: " + iwSitePath);
			
			String lspage = context.getParameterString("iw-ls-page-name");
			String page = "sites/" + iwSiteName+"/"+lspage+".page";	
			String gpage = context.getPageName();
					
			
			pagePathElement.addCDATA(page);
			
			Connection conn = null;
			PreparedStatement preparedStatement = null;
			ResultSet resultSet = null;
			
			String sqlStatement = "Select userid,username,email,website,rating,rating_date from RATINGS where path like '"+page+"'" + " ORDER BY RATING_DATE ASC";
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
				int count = 0;
				double rate=0;
				double avg = 0;
				String ct;
				
				if (!preparedStatement.execute()) {
					LOGGER.fatal("SQL execute FAIL: " + sqlStatement);
				} else {
					LOGGER.warn("SQL execute OK");
					int iteration = 0;
					do {
						resultSet = preparedStatement.getResultSet();
						LOGGER.warn("Got resultset, iteration: " + iteration++);
						Element resultSetElement = rootElement.addElement("Ratings");
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
							//Element row = resultSetElement.addElement("Row");
							for (int i = 0; i < columnNames.length; i++) {
								int columnIndex = i + 1;
								String columnName = columnNames[i];
								//Element columnNode = row.addElement(columnName);
								//LOGGER.warn("Fetching column '" + columnName + "' - index: " + columnIndex);
								String columnValue = resultSet.getString(columnIndex);
								if(columnName.equalsIgnoreCase("rating"))
								{
									rate = rate + Double.valueOf(columnValue.trim()).doubleValue();
								}
								//LOGGER.warn("Column '" + columnName + "' - value: " + columnValue);
								//if (columnValue != null && !columnValue.equals("")) {
									//columnNode.addCDATA(columnValue);
								//}
							}
							count++;
						}
						ct = ""+count+"";
						if(count != 0)
						{
							avg = rate/count;
						}
						else
						{
							avg = 0;
						}
						String average = ""+avg+"";
						
						resultSetElement.addAttribute("TotalRatingsCount", ct);
						resultSetElement.addAttribute("AverageRating", average);
						
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
				Element ErrorElement = rootElement.addElement("ErrorAllRating");
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
	  
	  protected Double getSubmittedRating(RequestContext context)
	  {
	    String submittedRatingString = context.getParameterString("submittedRating");
	    Double rating;	    	    
	    
	    if ((submittedRatingString != null) && (submittedRatingString.trim().length() > 0) && submittedRatingString.matches("\\d+"))
	    {
	      rating = new Double(submittedRatingString);
	    }
	    else
	    {
	      rating = null;
	    }
	    return rating;
	  }
	  
	  public static String getAssetId(String page,String pool)
	  {
		  String sqlStatement = "SELECT asset_id FROM PAGES pg WHERE pg.PATH LIKE '%"+page+"'";
			
			LOGGER.warn("SQL Query to fetch Asset id from pages table: " + sqlStatement);
			
			Connection conn = null;
			PreparedStatement preparedStatement = null;
			ResultSet resultSet = null;
			
			String assetId = null;
			
			try {
				String aId = null;
				// Try to connect to the database
				LOGGER.warn("Attempting connect to pool " + pool);
				conn = RequestUtils.getConnection(pool);
				
				LOGGER.debug("Connected to pool " + pool);
				
				LOGGER.info("Before SQL prepare: " + sqlStatement);
				
				preparedStatement = conn.prepareStatement(sqlStatement);
				preparedStatement.setQueryTimeout(30);
							
				if (!preparedStatement.execute()) {
					LOGGER.warn("SQL execute FAIL: " + sqlStatement);
				} else {
					LOGGER.warn("SQL execute OK");
					int iteration = 0;
					do {
						resultSet = preparedStatement.getResultSet();
						LOGGER.warn("Got resultset, iteration: " + iteration++);
						
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
							
							for (int i = 0; i < columnNames.length; i++) {
								int columnIndex = i + 1;
								aId = resultSet.getString(columnIndex);
								
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
				assetId = aId;
			}
			catch (SQLException e) {
				LOGGER.warn("SQL Exception :: "+ e);
				
			}
			finally {
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
						
			
			return assetId;
	  }
	  
}