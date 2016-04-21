package com.utils.externals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Properties;
import java.io.*;

import javax.servlet.http.Cookie;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;

import com.interwoven.livesite.common.codec.URLUTF8Codec;
import com.interwoven.livesite.dom4j.Dom4jUtils;
import com.interwoven.livesite.external.ExternalUtils;
import com.interwoven.livesite.external.ParameterHash;
import com.interwoven.livesite.external.impl.LivesiteSiteMap;
import com.interwoven.livesite.file.FileDALIfc;
import com.interwoven.livesite.model.EndUserSite;
import com.interwoven.livesite.runtime.RequestContext;
import com.interwoven.livesite.common.web.ForwardAction;
import com.interwoven.livesite.common.web.URLRedirectForwardAction;



public class doLogin {
	
	 	private static Logger logger = Logger.getLogger(doLogin.class);

	    //private static final String COOKIE_DOMAIN = ".jnpr.net";

	    private static final String COOKIE_PATH = "/";
	   
	    
	    public ForwardAction setLogin(RequestContext context)
	    {
	    	String propfile = context.getFileDal().getRoot() + "/iw/domain.properties";	    	
	    	InputStream myInputStream = context.getFileDal().getStream(propfile);	    	
	    	
		    Properties prop = new Properties();		
			//Load property file from the web folder or servlet context
		    
		    try
		    {
		    	prop.load(myInputStream);
		    }
		    catch (IOException e)
		    {
		      throw new RuntimeException("Unable to read file", e);
		    }	    
			
						
			String cookie_domain = prop.getProperty("domain");
			logger.debug("DOMAIN :: " + cookie_domain);
	    	//Login Check start
	    	
	    	Pattern pageLink = Pattern.compile("^\\$PAGE_LINK\\[(.*)\\]$", 1);
	    	
	    	Pattern oamLink = Pattern.compile("^.*obrareq.cgi.*");
	    	
	    	
	    	 
	        String newReferrer = context.getParameterString("newReferrer");
	        
	        String rfu = context.getRequest().getHeader("referer");	
	        
	        logger.warn("Referrer URL from headers is  : " + rfu);
	        
	        logger.warn("Referrer URL from newReferrer Parameter String  : " + newReferrer);
	        
	    	if (null != newReferrer && newReferrer.length() > 0 && "" != newReferrer)
	        {
	        	rfu = newReferrer;
	        }       
	    	
	    	Matcher oammatcher;
	    	boolean oamCheck = false;
	    	
	    	if(null != rfu)
	    	{
	    		oammatcher = oamLink.matcher(rfu);
	    		oamCheck = oammatcher.matches();
	    	}
	    	
       
	        if(null == rfu || "" == rfu || oamCheck)
	        {
	    	    rfu = context.getRedirectUrl();
	    	    logger.warn("Referrer URL is null, Context Redirect URL is  : " + rfu);
	        }
	       
	        ForwardAction faction = null;
	       
	    	String uid = "";
	       	        
	        String roles = "";
	        String employeeType = "";
	        String anys = "OblixAnonymous";
	        
	        logger.warn("getting headers");
	        
	    try {	    		
	    	
	        uid = context.getRequest().getHeader("user_id");
	        roles = context.getRequest().getHeader("roles_entitled");
	        employeeType = context.getRequest().getHeader("employee_type");
	        
	        //Arraylist for Junos developer role
	        ArrayList<String> validRoles = new ArrayList();
	        
	        validRoles.add("sdk-pre-release-binaries");
	        validRoles.add("sdk-release-binaries");
	        validRoles.add("sdk-access");
	        validRoles.add("sdk-release-docs");
	        validRoles.add("sdk-pre-release-docs");
	        
	        //Arraylist for external user roles
	        ArrayList<String> rolesOfUser = new ArrayList();
	        
	        String[] rolesOfUserTemp = {};
	        
	        if(null != roles && roles.contains(";"))
	        {
	        	rolesOfUserTemp = roles.split(";");
	        	
	        	for(int i=0;i<rolesOfUserTemp.length;i++)
	        	{
	        		rolesOfUser.add(rolesOfUserTemp[i]);
	        	}	        	
	        }
	        
	        if(null != roles && !roles.contains(";"))
	        {
	        	rolesOfUser.add(roles);
	        }
	    	
	        String junosLogpage = context.getParameterString("junosLoginPage");
	        Matcher matcher = pageLink.matcher(junosLogpage);
	        
	        if (matcher.matches()) {
	        	
	        		junosLogpage = matcher.group(1);
	        	
	            }
	        
	        Pattern jPage = Pattern.compile(junosLogpage);
	        
	        String junosDeclinepage = context.getParameterString("junosDeclinePage");
	        Matcher matcher1 = pageLink.matcher(junosDeclinepage);
	        
	        	if (matcher1.matches()) {
	        	
	        			junosDeclinepage = "/" + context.getSite() + matcher.group(1) + ".page";
	        	
	            }

	        String lsPageName = context.getPageName();
	        Matcher matchls = jPage.matcher(lsPageName);
	        
	        String junrole = context.getParameterString("junosrole");	        
	        
	        logger.warn("Role is  : " + roles + "  Employee type :: " + employeeType);
	        
	        logger.warn("Page is :: " + context.getPageName() + " | Junos login page : " + junosLogpage + 
	        		" | Jun Decline : " + junosDeclinepage + " | Jun role : " + junrole);
	        
	        
	       
	        
	       
	       if((lsPageName.equals(junosLogpage) || matchls.matches()) && junrole.equals("yes"))
	       {
	    	   logger.warn("Login page matches Junos role check page");
	    	   
	    	   if(rolesOfUser.size() > 0 && !employeeType.toLowerCase().equals("employee"))
	    	   {
		    	   boolean hasCommon = false;
		    	   
			    	   for (String alpha : rolesOfUser) 
			    	   {
			    	       for (String beta : validRoles) 
			    	       {
			    	    	  if (alpha.equals(beta)) 
			    	           {
			    	    		   hasCommon = true;
			    	               break;
			    	           }
			    	       }
			    	       
				    	       if (hasCommon)
				    	       {
				    	    	   break;
				    	       }
			    	       
			    	   }
		    	   
		    	   if (!hasCommon) 
	    	   	   {	
	    		   		rfu =  junosDeclinepage;
	    		   		logger.warn("User does NOT have Junos Developer role");
	    	   	   }
		    	   else
		    	   {
		    		   logger.warn("User is NOT an employee & has Junos Developer role");
		    	   }
	    	   }
	    	   else
	    	   {
	    		   logger.warn("User is Juniper Employee");
	    	   }
	    	  
	       }
	       
	       if(null == rfu || "" == rfu)
	       {
	    	   rfu="/content/default.page";
	    	   logger.warn("Referrer URL from all sources is null , so redirecting to default page ");	
	    	   
	       }
	       logger.warn("Final Referrer URL is  : " + rfu);	
	       
	       
	        
	        if (null != uid && uid.length() > 0 && "" != uid && !uid.equalsIgnoreCase(anys))
			{
	        	logger.warn("Creating Login cookie and OAM session for user :" + uid);
	        	
	        	//Writing value to Cookie Start//
	            
	        	String cookieVal = uid + ":" + context.getRequest().getHeader("mail") + ":" +
	        	context.getRequest().getHeader("full_name") + ":" + context.getRequest().getHeader("first_name");
	        	
	            Cookie loginCookie = new Cookie("loginDetails", cookieVal);
	            loginCookie.setPath(COOKIE_PATH);
	            loginCookie.setDomain(cookie_domain);
	            loginCookie.setSecure(false);
	            context.getResponse().addCookie(loginCookie); 
	            	                   
	            if(!context.getSession().isLoggedIn())
	            {
	            	logger.warn("Establishing Livesite login session for user :" + uid);
	            	context.getSession().setLoggedIn(true);
		            //Session timeout same as OAM timeout
		            context.getSession().getHttpSession().setMaxInactiveInterval(7200);
	            }
	            
	                    
			}	       
	      //Login Check end
	        
	        
	    	}
	    catch (Exception e) {
	            logger.warn("exception in getting headers : " +  e.getMessage());
	        }
	    	
	    	logger.warn("Redirecting to  : " + rfu);
	        faction = new URLRedirectForwardAction(context, rfu);
	        context.setRedirectUrl(null);
	        
	        return faction;
	    }


}
