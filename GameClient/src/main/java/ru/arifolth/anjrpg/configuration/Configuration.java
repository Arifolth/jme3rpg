package ru.arifolth.anjrpg.configuration;

import org.w3c.dom.Document;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 20.12.12
 * Time: 3:35
 * To change this template use File | Settings | File Templates.
 */
public class Configuration {
    private List<ConfigurationUpdateListener> subscribersList = new ArrayList<ConfigurationUpdateListener>();

    public static final String SEPARATOR = ";";

    private Properties properties;
    private HashMap<String, Document> xmlConfigs;
    private String path;
    private static final Configuration config = new Configuration();

    private Configuration() //made it public for testing
    {
        properties = new Properties();
        xmlConfigs = new HashMap<String, Document>();
    }

    public static Configuration getInstance()
    {
        return config;
    }

    public void init(String path) throws IOException {
        //System.out.println("Initializing Configuration from file: "+path);
        this.path=path;
        FileInputStream fs = new FileInputStream(path);
        try
        {
            init(fs);
        }
        finally
        {
            try
            {
                fs.close();
            }
            catch( IOException e )
            {
                e.printStackTrace(System.err);
            }
        }
    }

    public void init(InputStream inputStream) throws IOException {
        properties.clear();
        properties.load(inputStream);
        xmlConfigs.clear();
    }

    /**
     *
     * @param key configuration param key
     * @return param value
     */
    public String getParam(ConfigurationKey key)
    {
        return getParam(key.name(), key.defaultValue);
    }

    public String getParam(ConfigurationKey key, String defparam)
    {
        return getParam(key.name(), defparam);
    }

    public String getParam(String param, String defparam)
    {
        String tString;
        String tKey;
        tString = properties.getProperty(param);
        if( null == tString )
        {
            Enumeration tEnum = properties.keys();
            while( tEnum.hasMoreElements() )
            {
                tKey = (String)tEnum.nextElement();
                if( ( null != tKey ) && ( tKey.toLowerCase().equals(param.toLowerCase()) ) )
                {
                    tString = properties.getProperty(tKey);
                    break;
                }
            }

            if( null == tString )
            {
                tString = defparam;

                if (null == tString || tString.length() == 0) {
                    System.err.println("Warning: value of param \"{}\" is null or empty!" + param);
                }
            }
        }
        return tString;
    }

    public Document getXMLConfig(String filename) throws Exception {
        Document doc = null;

        /*if (xmlConfigs.containsKey(filename)) {
            doc = xmlConfigs.get(filename);
        } else {
            String filePath = Configuration.getInstance().getParam(ConfigurationKey.sAppDataPath) + filename;
            File xmlFile = new File(filePath);

            if (!xmlFile.exists()) {
                filePath = Configuration.getInstance().getParam(ConfigurationKey.sLangPath) + filename;
                xmlFile = new File(filePath);
            }

            if (xmlFile.exists()) {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                doc = db.parse(xmlFile);
                //add document to cache
                xmlConfigs.put("filename", doc);
            } else {
                System.err.println("Cannot read xml config from {}: file does not exist." + filename);
            }
        }*/

        return doc;
    }

    public void subscribe(ConfigurationUpdateListener subscriber) {
        subscribersList.add(subscriber);
        subscriberReloadConfiguration(subscriber);
    }

    public void notifyConfigurationReloaded() {
        for(ConfigurationUpdateListener subscriber : subscribersList) {
            subscriberReloadConfiguration(subscriber);
        }
    }

    private void subscriberReloadConfiguration(ConfigurationUpdateListener subscriber) {
        try {
            subscriber.reloadConfiguration();
        } catch( Throwable e ) {
            System.err.println("Error occured during configuration reload:" + e);
        }
    }
}
