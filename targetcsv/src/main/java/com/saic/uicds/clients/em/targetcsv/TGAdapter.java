/**
 * 
 */
package com.saic.uicds.clients.em.targetcsv;

import java.io.FileNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.core.io.FileSystemResource;

/**
 * @author dsh
 * 
 */
public class TGAdapter {

    private static final String APP_CONTEXT_FILE = "target-context.xml";

    private static Logger logger = LoggerFactory.getLogger(TGAdapter.class);

    /**
     * @param args
     */
    public static void main(String[] args) {

        if (args.length >= 1) {
            usage();
            return;
        }

        ApplicationContext context = getApplicationContext();

        //Read the Store Data
        /*FileSystemResource inboundStoreDirectoryResource = null;
        Object storeBean = context.getBean("inboundStoreFileDirectory");
        if (storeBean != null){
        	if (storeBean instanceof FileSystemResource){
        		inboundStoreDirectoryResource = (FileSystemResource) storeBean;
        	}
        }
        
        if (inboundStoreDirectoryResource != null) {
        	logger.info("Loading Walgreens Stores Data from: "
        			+ inboundStoreDirectoryResource.getPath());
        }*/
        
        //Monitor the report
        FileSystemResource inboundDirectoryResource = null;
        Object fsrBean = context.getBean("inboundFileDirectory");
        if (fsrBean != null) {
            if (fsrBean instanceof FileSystemResource) {
                inboundDirectoryResource = (FileSystemResource) fsrBean;
            }
        }

        if (inboundDirectoryResource != null) {
            logger.info("Target Adapter is now monitoring the directory: "
                + inboundDirectoryResource.getPath());
        }
    }

    private static ApplicationContext getApplicationContext() {

        ApplicationContext context = null;
        try {
            context = new FileSystemXmlApplicationContext("./" + APP_CONTEXT_FILE);
            System.out.println("Using local application context file: " + APP_CONTEXT_FILE);
        } catch (BeansException e) {
            if (e.getCause() instanceof FileNotFoundException) {
                System.out.println("Local application context file not found so using file from jar: contexts/"
                    + APP_CONTEXT_FILE);
            } else {
                // System.out.println("Error reading local file context: " +
                // e.getCause().getMessage());
                e.printStackTrace();
            }
        }

        if (context == null) {
            context = new ClassPathXmlApplicationContext(new String[] { "contexts/"
                + APP_CONTEXT_FILE });
        }

        return context;
    }

    private static void usage() {

        System.out.println("");
        System.out.println("This is the UICDS Target Adapter.");
        System.out.println("Execution of this client depends on a functioning UICDS server. The default is http://localhost/uicds/core/ws/services");
        System.out.println("To verify that a UICDS server is accessible, use a browser to navigate to http://localhost/uicds/Console.html");
        System.out.println("");
        System.out.println("Usage: java -jar targetAdapter.jar");
        System.out.println("");
        System.out.println("Parameters for the targetAdapter can be configued in Spring context file");
        System.out.println("in the current directory or classpath named: " + APP_CONTEXT_FILE);
    }

}
