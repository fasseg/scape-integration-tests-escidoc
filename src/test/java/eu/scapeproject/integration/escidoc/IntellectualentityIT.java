package eu.scapeproject.integration.escidoc;

import static org.junit.Assert.*;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.scapeproject.model.IntellectualEntity;
import eu.scapeproject.util.ScapeMarshaller;

public class IntellectualentityIT {

    
    private static final Logger logger = LoggerFactory.getLogger(IntellectualentityIT.class);
    
    private HttpClient client = new DefaultHttpClient();
    private static final String ESCIDOC_URI = "http://localhost:8080";
    private static final String ENDPOINT_ENTITY = ESCIDOC_URI + "/scape/entity";

    @Test
    public void ingestAndRetrieveIntellectualEntity() throws Exception {
        InputStream src = this.getClass().getClassLoader().getResourceAsStream("SCAPE_entity_example.xml");
        HttpPost post = new HttpPost(ENDPOINT_ENTITY);
        post.setEntity(new InputStreamEntity(src, -1));
        HttpResponse resp = client.execute(post);
        assertTrue("Server returned " + resp.getStatusLine().toString(),resp.getStatusLine().getStatusCode() == 200);
        
        /* get the pid from the response */
        String id = IOUtils.toString(resp.getEntity().getContent());
        id = id.substring(id.indexOf("<scape:value>") + 13, id.indexOf("</scape:value")).trim();
        logger.debug("ingested object with id " + id);
        
        /* load the intellectual entity from the local xml document for comparison */
        src = this.getClass().getClassLoader().getResourceAsStream("SCAPE_entity_example.xml");
        IntellectualEntity orig = ScapeMarshaller.newInstance().deserialize(IntellectualEntity.class, src);
        IOUtils.closeQuietly(src);
        
        /* fetch the newly ingested entity from escidoc */
        HttpGet get = new HttpGet(ENDPOINT_ENTITY + "/" + id);
        resp = client.execute(get);
        assertTrue("Server returned " + resp.getStatusLine().toString(), resp.getStatusLine().getStatusCode() == 200);
        IntellectualEntity fetched = ScapeMarshaller.newInstance().deserialize(IntellectualEntity.class, resp.getEntity().getContent());
        
        /* compare the fetched with the local entity */
        assertTrue("Identifier is not equal", orig.getIdentifier().getValue().equals(fetched.getIdentifier().getValue()));
    }
    
    @Test
    public void retrieveMetadata() throws Exception {
        fail("Not yet implemented!");
    }
    
    @Test
    public void retrieveIntellectualEntitySet() throws Exception {
        fail("Not yet implemented!");
    }
    
    @Test
    public void ingestAndRetrieveIntellectualEntityAsync() throws Exception {
        fail("Not yet implemented!");
    }
    
    @Test
    public void updateAndRetrieveIntellectualEntity() throws Exception {
        fail("Not yet implemented!");
    }
    
    @Test
    public void retrieveEntityVersionList() throws Exception {
        fail("Not yet implemented!");
    }
    
    @Test
    public void retrieveFile() throws Exception {
        fail("Not yet implemented!");
    }
    
    @Test
    public void retrieveBitStream() throws Exception {
        fail("Not yet implemented!");
    }
    
    @Test
    public void searchIntellectualEntity() throws Exception {
        fail("Not yet implemented!");
    }
    
    @Test
    public void searchFiles() throws Exception {
        fail("Not yet implemented!");
    }
    
    @Test
    public void retrieveEntityLifecycleState() throws Exception {
        fail("Not yet implemented!");
    }
    
    @Test
    public void retrieveRepresentation() throws Exception {
        fail("Not yet implemented!");
    }
    
    @Test
    public void updateRepresentation() throws Exception {
        fail("Not yet implemented!");
    }
    
    @Test
    public void updateMetadata() throws Exception {
        fail("Not yet implemented!");
    }

}
