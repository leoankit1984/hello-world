package com.utils.externals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

public class sitemapUtils extends LivesiteSiteMap {
    private static Logger logger = Logger.getLogger(sitemapUtils.class);

    private static final String COOKIE_DOMAIN = ".juniper.net";

    private static final String COOKIE_PATH = "/";

    /**
     * getBreadCrumbsWithParams() - returns Dom4j document containing single
     * node tree from the sitemap that contains the current page.
     * *
     * @param context
     * @return
     */
    
	public Document getSitemapAndBreadcrumbs(RequestContext context) {
        logger.info("getsitemapAndBreadcrumbs called at " + System.currentTimeMillis());
        boolean havesetsegment = false;
        Document resultSets = Dom4jUtils.newDocument("<ResultSets/>");
        
		   
		Element resultSetsSitemap = resultSets.getRootElement().addElement("sitemap");
        Document sitemapDoc = ensureUniqueNodeIds(getSiteMap(context));
        // failIfNull(sitemapDoc, "Cannot build sitemap DOM object");
        // Lack of a sitemap is not a fatal
        if (sitemapDoc != null) {
            Element sitemapTree = (Element) sitemapDoc.getRootElement().selectSingleNode("segment");
            // failIfNull(sitemapTree, "Cannot build sitemap tree object");
            // again not a fatal
            resultSetsSitemap.add(sitemapTree.detach());
        }
        
      //Login Check start
        String uid = context.getRequest().getHeader("user_id");    
      
     
        Element loginElement = resultSets.getRootElement().addElement("Login");
        Element stat = loginElement.addElement("Status");
        
        String anys = "OblixAnonymous";
        
        
        if (null != uid && uid.length() > 0 && "" != uid && !uid.equalsIgnoreCase(anys))
		{
        	
        	stat.addCDATA("true");
        	Element userid = loginElement.addElement("UserId");
        	userid.addCDATA(uid);
        	
        	Element email = loginElement.addElement("Email");
        	email.addCDATA(context.getRequest().getHeader("mail"));
        	
        	Element fname = loginElement.addElement("Name");
        	fname.addCDATA(context.getRequest().getHeader("full_name"));
        	
        	Element frname = loginElement.addElement("FirstName");
        	frname.addCDATA(context.getRequest().getHeader("first_name"));
        	
        	Element lsname = loginElement.addElement("LastName");
        	lsname.addCDATA(context.getRequest().getHeader("last_name"));
        	
        	//Writing value to Cookie Start//
            
        	String cookieVal = uid + ":" + context.getRequest().getHeader("mail") + ":" + 
        	context.getRequest().getHeader("full_name") + ":" + context.getRequest().getHeader("first_name");
        	
            Cookie transactionalSegmentCookie = new Cookie("loginDetails", cookieVal);
            transactionalSegmentCookie.setPath(COOKIE_PATH);
            transactionalSegmentCookie.setDomain(COOKIE_DOMAIN);
            transactionalSegmentCookie.setSecure(false);
            context.getResponse().addCookie(transactionalSegmentCookie);                 
                    
		}
        else
        {
        	String lc = "";
        	
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
        		String [] loginVals = lc.split(":");
        		stat.addCDATA("true");
        		
        		Element userid = loginElement.addElement("UserId");
            	userid.addCDATA(loginVals[0]);
            	
            	Element email = loginElement.addElement("Email");
            	email.addCDATA(loginVals[1]);
            	
            	Element fname = loginElement.addElement("Name");
            	fname.addCDATA(loginVals[2]);
            	
            	Element frname = loginElement.addElement("FirstName");
            	frname.addCDATA(loginVals[3]);
            	
            	Element lsname = loginElement.addElement("LastName");
            	lsname.addCDATA(loginVals[3]);
        		
        	}
        	else
        	{
        		stat.addCDATA("false");
        		
        	}
        	
        }
      //Login Check end
        
        return resultSets;
    }
	
	
	public Document ensureUniqueNodeIds(Document doc) {
        long startTime = System.currentTimeMillis();
        /**
         * Dom4j -> W3C -> Dom4j
         * 
         * This is being done because the Document returned from
         * getSiteMap **COULD** contain elements with MULTIPLE ID ATTRIBUTES
         * (through the use of refid (copy/paste nodes)), converting from a
         * Dom4J to W3C Document and back cleans the structure so only one id
         * attribute is used. Due to bug in LivesiteSiteMap class. Adds about
         * 40ms to the method execution.
         */
        org.w3c.dom.Document w3cDoc = Dom4jUtils.toW3cDocument(doc);
//        doc = Dom4jUtils.toDom4JDocument(w3cDoc);
        // Find all the nodes that have id attributes
        List nodesWithIdsList = doc.selectNodes("//node[@id]");
        logger.info("Nodes count: " + nodesWithIdsList.size());
        // Create a set just to hold the list of all the ids
        Set nodeIds = new LinkedHashSet();
        // Create a set just to hold the list of duplicate ids
        Set dupeIds = new LinkedHashSet();
        logger.info("Gathering all nodes ids...");
        // Try and put all the node ids into the set, if that fails it means
        // that a
        // dupe has been found so store the dupe
        for (int i = 0; i < nodesWithIdsList.size(); i++) {
            Element e = (Element) nodesWithIdsList.get(i);
            // Try and put the id into the set, if it fails - its a duplicate
            // so...
            if (!nodeIds.add(e.attributeValue("id"))) {
                // ... insert it into the dupeIds set
                dupeIds.add(e.attributeValue("id"));
            }
            // log.debug("Node id: " + e.attributeValue("id"));
        }
        // If no duplicate ids were found just return the document
        if (dupeIds.size() == 0) {
            logger.info("No duplicates returning original Document");
            return doc;
        }
        logger.info("Unique Node Count: " + nodeIds.size());
        logger.info("Duplicate id count: " + dupeIds.size());
        // Loop through the set getting the node duplicates
        for (Iterator dupeIdsIter = dupeIds.iterator(); dupeIdsIter.hasNext();) {
            String nodeId = (String) dupeIdsIter.next();
            List duplicateNodes = doc.selectNodes("//node[@id='" + nodeId + "']");
            logger.info("Node " + nodeId + " has " + duplicateNodes.size() + " duplicates. Ids will be renamed.");
            // Set a counter, will be used to append onto the id eg: dgjGhj52:1,
            // dgjGhj52:2, dgjGhj52:etc
            int idCounter = 1;
            // Go through the nodes and replace their ids with unique ids
            for (Iterator duplicateNodesIter = duplicateNodes.iterator(); duplicateNodesIter.hasNext();) {
                Element node = (Element) duplicateNodesIter.next();
                String currentId = node.attributeValue("id");
                String newNodeId = currentId + ":" + idCounter;
                node.addAttribute("id", newNodeId);
                idCounter++;
                logger.info("\t" + node.attributeValue("id"));
            }
        }
        logger.info("Document now has unique node ids.");
        logger.info("Execution Time: " + (System.currentTimeMillis() - startTime) + "ms");
        return doc;
    }
	
	}//sitemaputils class closes here



