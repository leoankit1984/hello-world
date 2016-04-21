package com.utils.externals;

import java.io.IOException;
import java.io.InputStream;
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

public class doLogout {
	
	private static Logger logger = Logger.getLogger(doLogout.class);

    private static final String COOKIE_DOMAIN = ".jnpr.net";

    private static final String COOKIE_PATH = "/";
    
    public ForwardAction removeSession(RequestContext context)
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
		//logger.debug("DOMAIN :: " + cookie_domain);
		
        String rfu = context.getRequest().getHeader("referer");
        String logoutHome = context.getParameterString("logoutHome");
        
        logger.warn("Referrer URL is  : " + rfu);	       
       
        ForwardAction faction = null;
        
        //Removing Cookies
        
        Cookie loginCookie = new Cookie("loginDetails", "");
        loginCookie.setMaxAge(0);
        loginCookie.setPath(COOKIE_PATH);
        loginCookie.setDomain(cookie_domain);
        loginCookie.setSecure(false);
        context.getResponse().addCookie(loginCookie); 
        
        Cookie ssoCookie = new Cookie("ObSSOCookie", "");
        ssoCookie.setMaxAge(0);
        ssoCookie.setPath(COOKIE_PATH);
        ssoCookie.setDomain(cookie_domain);
        ssoCookie.setSecure(false);
        context.getResponse().addCookie(ssoCookie);
        
        Cookie smCookie = new Cookie("SMSESSION", "");
        smCookie.setMaxAge(0);
        smCookie.setPath(COOKIE_PATH);
        smCookie.setDomain(cookie_domain);
        smCookie.setSecure(false);
        context.getResponse().addCookie(smCookie); 
        
        Cookie jsCookie = new Cookie("JSESSIONID", "");
        jsCookie.setMaxAge(0);
        jsCookie.setPath(COOKIE_PATH);
        jsCookie.setDomain(cookie_domain);
        jsCookie.setSecure(false);
        context.getResponse().addCookie(jsCookie);
        
        context.getSession().setLoggedIn(false);
        
           
        //Redirecting to Referrer page
        
        
        
        if(null != logoutHome && logoutHome.length() > 0 && "" != logoutHome)
        {        
        	logger.warn("Redirecting to  : " + logoutHome);
        	faction = new URLRedirectForwardAction(context, logoutHome);
        }
        else
        {
        	logger.warn("Redirecting to  : " + rfu);
        	faction = new URLRedirectForwardAction(context, rfu);
        }
        context.setRedirectUrl(null);
        
        return faction;
        
    }
}
