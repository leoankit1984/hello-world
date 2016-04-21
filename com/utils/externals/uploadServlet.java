package com.utils.externals;


import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.io.*;
 
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;
 
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;
import org.dom4j.Element;

import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;
 
 
public class uploadServlet extends HttpServlet {
	private static Logger logger = Logger.getLogger(uploadServlet.class);
	
	private static final String TMP_DIR_PATH1 = "/opt/webhost/webdev/uploaded/";
		
	private static final int MAX_FILE_SIZE = 1024 * 1024 * 1; // 1MB
	private static final int REQUEST_SIZE = 1024 * 1024 * 50; // 50MB
	
	private File tmpDir;
	private static final String DESTINATION_DIR_PATH ="/files";
	private File destinationDir;

 
	public void init(ServletConfig config) throws ServletException {
		super.init(config);		
				
		/*tmpDir = new File(TMP_DIR_PATH1);
		
		//String realPath = getServletContext().getRealPath(DESTINATION_DIR_PATH);
		String realPath = tmpDir + DESTINATION_DIR_PATH;
		logger.warn("Real path :: " + realPath);
		
		destinationDir = new File(realPath);
		
		logger.warn("Dest Dir :: " + destinationDir);
		
		if(!destinationDir.isDirectory()) {
			throw new ServletException(DESTINATION_DIR_PATH+" is not a directory");
		}
		*/
 
	}
 
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	    
		//Loading property file
		Properties prop = new Properties();		
		//Load property file from the web folder or servlet context
		prop.load(getServletContext().getResourceAsStream("/iw/mailTemplate.properties"));
		
		String destDir = prop.getProperty("destDir");
		destinationDir = new File(destDir);
		
		logger.warn("Destination Dir :: " + destinationDir);
		
		if(!destinationDir.isDirectory()) {
			throw new ServletException(destDir + " is not a directory");
		}
		
		
		DiskFileItemFactory  fileItemFactory = new DiskFileItemFactory ();
		/*
		 *Set the size threshold, above which content will be stored on disk.
		 */
		fileItemFactory.setSizeThreshold(5*1024*1024); //1 MB
		
		/*
		 * Set the temporary directory to store the uploaded files of size above threshold.
		 */
		fileItemFactory.setRepository(tmpDir);
 
		ServletFileUpload uploadHandler = new ServletFileUpload(fileItemFactory);
		uploadHandler.setFileSizeMax(MAX_FILE_SIZE);
		uploadHandler.setSizeMax(REQUEST_SIZE);
		
		 // Recipient's email ID needs to be mentioned.
		String lc = "";
		String user = "";
    	
    	if (request.getCookies() != null) {
            Cookie cookies[] = request.getCookies();
            for (int i = 0; i < cookies.length; i++) {
            	                	
                if (cookies[i].getName().equals("loginDetails")) {
                	lc = cookies[i].getValue();                    	
                    logger.warn("login cookie :: " + lc);
                }
            }
        }
    	
    	String to = prop.getProperty("to");
    	
    	String cc = "";
    	
    	if(null != lc && lc.length() > 0 && lc != "")
    	{
    		String [] loginVals = lc.split(":");    		
    		logger.warn("user email found in login cookie");
    		user = "Submitted by </td><td> " + loginVals[2] + " [ " + loginVals[1] + " ] " ;
    		cc = loginVals[1];
    		
    	}
	     
    	//CC
	      cc = prop.getProperty("cc");
	 
	      // Sender's email ID needs to be mentioned
	      String from = prop.getProperty("from");	      
	      
	      
	      //mail subject
	      String subject = prop.getProperty("subject");
	      
	      subject += destinationDir;
	 
	      // Assuming you are sending email from localhost
	      String host = "localhost";
	 
	      // Get system properties
	      Properties properties = System.getProperties();
	 
	      // Setup mail server
	      properties.setProperty("mail.smtp.host", host);
	 
	      // Get the default Session object.
	      Session session = Session.getDefaultInstance(properties);
	      
		  // Set response content type
	      response.setContentType("text/html");
	      
	      String msg = "";
	      String params = "<tr><td>" + user + "</td></tr><tr><td>Submitted Files  </td><td><table>";
	      String status = "";
	      
		
		try {
			/*
			 * Parse the request
			 */
			List items = uploadHandler.parseRequest(request);
			Iterator itr = items.iterator();
			while(itr.hasNext()) {
				FileItem item = (FileItem) itr.next();
				/*
				 * Handle Form Fields.
				 */
				if(item.isFormField()) {
					logger.warn("Param Name = "+item.getFieldName()+", Value = "+item.getString());
					//params += "<tr><td>" + item.getFieldName() + "</td><td>" + item.getString() + "</td></tr>";
					
				} else {
					
					logger.warn("File Name = "+item.getFieldName()+", Value = "+item.getName());
					
					/*
					 * Write file to the ultimate location.
					 */
					if(item.getName() != "" && null != item.getName())
					{
						File file = new File(destinationDir,item.getName());
						item.write(file);	
						params += "<tr><td>" + item.getName() + "</td></tr>";
						status = "success";			
						
					}
					
					
				}				
				
			}
			
			params += "</table></td></tr>";
			params += "<tr><td>Location </td><td>" + destinationDir + "</td></tr>";
			
			
		}catch(FileUploadException ex) {
			status = "failed";
			logger.warn("Error encountered while parsing the request",ex);
			//getServletContext().getRequestDispatcher("/failed.jsp").forward(request, response);
		} catch(Exception ex) {
			status = "failed";
			logger.warn("Error encountered while uploading file",ex);
			//getServletContext().getRequestDispatcher("/failed.jsp").forward(request, response);
		}
		
		if(status != "" && status == "success"){			
		
		           //get the property value
		String msgVal = prop.getProperty("html").toString();
		logger.warn("params is :: " + params);
		msg = msgVal.replaceAll("mailpar", params);
		
		
		
		logger.warn("msg is :: " + msg);
		logger.warn("subject is :: " + subject);

	
			try{
		         // Create a default MimeMessage object.
		         MimeMessage message = new MimeMessage(session);
		         // Set From: header field of the header.
		         message.setFrom(new InternetAddress(from));
		         // Set To: header field of the header.
		         message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
		         //Set CC
		         if(null != cc && !cc.equals(""))
		         {
		        	 message.addRecipient(Message.RecipientType.CC, new InternetAddress(cc)); 
		         }		         
		         // Set Subject: header field
		         message.setSubject(subject);
		         // Now set the actual message		        
		         message.setContent(msg, "text/html; charset=utf-8");
		         message.setText(msg, "utf-8", "html");
		         //message.setText(msg);
		         
		         
		         // Send message//
		         logger.warn("----Sending Mail----");
		         Transport.send(message);
		         
		      }catch (MessagingException mex) {
		         mex.printStackTrace();
		      }
						
		      getServletContext().getRequestDispatcher("/iw/success.jsp").forward(request, response);
	
		} //status if ends here
		else
		{
			getServletContext().getRequestDispatcher("/iw/failed.jsp").forward(request, response);
		}
 
	}
 
}