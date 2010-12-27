package org.mobicents.ss7.linkset.oam;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import javolution.text.TextBuilder;
import javolution.util.FastMap;
import javolution.xml.XMLBinding;
import javolution.xml.XMLObjectReader;
import javolution.xml.XMLObjectWriter;
import javolution.xml.stream.XMLStreamException;

import org.apache.log4j.Logger;


/**
 * 
 * @author amit bhayani
 * 
 */
public class LinksetManager {

    private static final Logger logger = Logger.getLogger(LinksetManager.class);

    private static final String LINKSET = "linkset";
    private static final String LINK = "link";
    private static final String CLASS_ATTRIBUTE = "type";

    private static final String LINKSET_PERSIST_DIR_KEY = "linkset.persist.dir";
    private static final String USER_DIR_KEY = "user.dir";
    private static final String PERSIST_FILE_NAME = "linksetmanager.xml";

    private static final String TAB_INDENT = "\t";

    private static final XMLBinding binding = new XMLBinding();

    private final TextBuilder persistFile = TextBuilder.newInstance();

    private String persistDir = null;

    // Hold LinkSet here. LinkSet's name as key and actual LinkSet as Object
    private FastMap<String, Linkset> linksets = new FastMap<String, Linkset>();

    private LinksetFactoryFactory linksetFactoryFactory = null;

    public LinksetManager() {
        binding.setAlias(Linkset.class, LINKSET);
        binding.setAlias(Link.class, LINK);
        binding.setClassAttribute(CLASS_ATTRIBUTE);
    }

    public LinksetFactoryFactory getLinksetFactoryFactory() {
        return linksetFactoryFactory;
    }

    public void setLinksetFactoryFactory(
            LinksetFactoryFactory linksetFactoryFactory) {
        this.linksetFactoryFactory = linksetFactoryFactory;
    }

    public FastMap<String, Linkset> getLinksets() {
        return linksets;
    }

    public String getPersistDir() {
        return persistDir;
    }

    public void setPersistDir(String persistDir) {
        this.persistDir = persistDir;
    }

    public void start() {
        this.persistFile.clear();

        if (persistDir != null) {
            this.persistFile.append(persistDir).append(File.separator).append(
                    PERSIST_FILE_NAME);
        } else {
            persistFile.append(
                    System.getProperty(LINKSET_PERSIST_DIR_KEY, System
                            .getProperty(USER_DIR_KEY))).append(File.separator)
                    .append(PERSIST_FILE_NAME);
        }

        logger.info(String.format("SS7 configuration file path %s", persistFile
                .toString()));

        try {
            this.load();
        } catch (FileNotFoundException e) {
            logger.warn(String.format(
                    "Failed to load the SS7 configuration file. \n%s", e
                            .getMessage()));
        }
    }

    public void stop() {
        this.store();
    }

    /**
     * Operations
     */
    public String createLinkset(String[] options) {

        Linkset linkset = this.linksetFactoryFactory.createLinkset(options);

        if (linkset == null) {
            return "Invalid Command";
        }

        // huh, we parse first and then check for name :(
        if (this.linksets.containsKey(linkset.getLinksetName())) {
            return LinkOAMMessages.LINKSET_ALREADY_EXIST;
        }

        this.linksets.put(linkset.getLinksetName(), linkset);
        this.store();

        return LinkOAMMessages.LINKSET_SUCCESSFULLY_ADDED;
    }

    /**
     * Persist
     */
    protected void store() {

        // TODO : Should we keep reference to Objects rather than recreating
        // everytime?
        try {
            XMLObjectWriter writer = XMLObjectWriter
                    .newInstance(new FileOutputStream(persistFile.toString()));
            writer.setBinding(binding);
            // Enables cross-references.
            // writer.setReferenceResolver(new XMLReferenceResolver());
            writer.setIndentation(TAB_INDENT);

            for (FastMap.Entry<String, Linkset> e = this.linksets.head(), end = this.linksets
                    .tail(); (e = e.getNext()) != end;) {
                Linkset value = e.getValue();
                writer.write(value, LINKSET, value.getClass().getName());
            }

            writer.close();
        } catch (Exception e) {
            this.logger.error("Error while persisting the state in file", e);
        }
    }

    /**
     * Load and create LinkSets and Link from persisted file
     * 
     * @throws Exception
     */
    protected void load() throws FileNotFoundException {

        XMLObjectReader reader = null;
        try {
            reader = XMLObjectReader.newInstance(new FileInputStream(
                    persistFile.toString()));

            reader.setBinding(binding);
            // reader.setReferenceResolver(new XMLReferenceResolver());

            // FIXME : Bug in .hasNext()
            // http://markmail.org/message/c6lsehxlxv2hua5p. It shouldn't throw
            // Exception
            while (reader.hasNext()) {
                Linkset linkset = reader.read(LINKSET);
                this.linksets.put(linkset.getLinksetName(), linkset);
            }
        } catch (XMLStreamException ex) {
            // this.logger.info(
            // "Error while re-creating Linksets from persisted file", ex);
        }
    }

    public static void main(String args[]) throws Exception {
        LinksetManager linkSetManager = new LinksetManager();

        LinksetFactoryFactory linksetFactoryFactory = new LinksetFactoryFactory();
//        linksetFactoryFactory.addFactory(new DahdiLinksetFactory());
//        linksetFactoryFactory.addFactory(new DialogicLinksetFactory());
//        linksetFactoryFactory.addFactory(new M3UALinksetFactory());

        linkSetManager.setLinksetFactoryFactory(linksetFactoryFactory);

        linkSetManager.createLinkset("dahdi opc 1 dpc 2 ni 3 linkset1"
                .split(" "));
        linkSetManager.store();

        LinksetManager linkSetManager1 = new LinksetManager();
        linkSetManager1.load();

        System.out.println(linkSetManager1.linksets.size());
    }

}
