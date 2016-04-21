package com.utils.externals;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.servlet.http.Cookie;

import org.apache.log4j.Logger;

import com.interwoven.livesite.common.web.ForwardAction;
import com.interwoven.livesite.runtime.RequestContext;

public class SessionController {
	
	private static Logger logger = Logger.getLogger(SessionController.class);
	
	private static final String COOKIE_DOMAIN = ".jnpr.net";

    private static final String COOKIE_PATH = "/";
	
	public ForwardAction sessionControl(RequestContext context)
    {
		if(context.isRuntime())
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
			logger.warn("DOMAIN :: " + cookie_domain);
			
			String uid = context.getRequest().getHeader("user_id");
			logger.warn("User ID :: " + uid);
			
			
	    	if(!context.getSession().isLoggedIn())
	    	{
	    		String lc = null;
	    		
	    		if (context.getRequest().getCookies() != null) {
	                Cookie cookies[] = context.getRequest().getCookies();
	                for (int i = 0; i < cookies.length; i++) {
	                	                	
	                    if (cookies[i].getName().equals("loginDetails")) {
	                    	lc = cookies[i].getValue();                    	
	                        logger.warn("login cookie :: " + lc);
	                    }
	                }
	            }
	    		
	    		if(null != lc && lc.length() > 0 && lc != "")
	    		{
	    			
	    			Cookie loginCookie = new Cookie("loginDetails", "");
	                loginCookie.setMaxAge(0);
	                loginCookie.setPath(COOKIE_PATH);
	                loginCookie.setDomain(cookie_domain);
	                loginCookie.setSecure(false);
	                context.getResponse().addCookie(loginCookie); 
	                
	                logger.warn("PRE-CONTROLLER :: Removing Login cookie as Livesite session is not valid");
	    		}
	    		
	    	}
		}
		return null;
    }

}
