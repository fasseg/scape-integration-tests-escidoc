package eu.scapeproject.integration.escidoc;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.BeforeClass;
import org.junit.Test;
import org.purl.dc.elements._1.ElementContainer;
import org.purl.dc.elements._1.SimpleLiteral;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.scapeproject.model.File;
import eu.scapeproject.model.IntellectualEntity;
import eu.scapeproject.model.IntellectualEntityCollection;
import eu.scapeproject.model.LifecycleState;
import eu.scapeproject.util.ScapeMarshaller;

public class IntellectualentityIT {

	private static final Logger logger = LoggerFactory.getLogger(IntellectualentityIT.class);

	private static final String ESCIDOC_URI = "http://localhost:8080";
	private static final String ENDPOINT_AUTH = ESCIDOC_URI + "/aa/j_spring_security_check";
	private static final String ENDPOINT_LOGIN = ESCIDOC_URI + "/aa/login";
	private static final String ENDPOINT_ENTITY = ESCIDOC_URI + "/scape/entity";
	private static final String ENDPOINT_ENTITY_ASYNC = ESCIDOC_URI + "/scape/entity-async";
	private static final String ENDPOINT_ENTITY_SET = ESCIDOC_URI + "/scape/entity-list";
    private static final String ENDPOINT_ENTITY_SEARCH = ESCIDOC_URI + "/scape/sru/entities";
    private static final String ENDPOINT_FILE = ESCIDOC_URI + "/scape/file";
	private static final String ENDPOINT_METADATA = ESCIDOC_URI + "/scape/metadata";
	private static final String ENDPOINT_LIFECYCLE = ESCIDOC_URI + "/scape/lifecycle";
	private static final String ESCIDOC_USER = "sysadmin";
	private static final String ESCIDOC_PASS = "sys";

	private static DefaultHttpClient client = new DefaultHttpClient();

	@BeforeClass
	public static void setup() throws Exception {
		/* first a GET to the login url to get a cookie */
		HttpGet get = new HttpGet(ENDPOINT_LOGIN);
		HttpResponse resp = client.execute(get);
		get.releaseConnection();

		/* then POST the username password combo using the cookie from the previuous GET */
		HttpPost post = new HttpPost(ENDPOINT_AUTH);
		post.setEntity(new StringEntity("j_username=" + ESCIDOC_USER + "&j_password=" + ESCIDOC_PASS));
		post.setHeader("Content-Type", "application/x-www-form-urlencoded");
		resp = client.execute(post);
		post.releaseConnection();

		/* now get the login url *AGAIN* to get a escidoc token */
		get = new HttpGet(ENDPOINT_LOGIN);
		resp = client.execute(get);
		get.releaseConnection();
	}

	@Test
	public void ingestAndRetrieveIntellectualEntity() throws Exception {
		InputStream src = this.getClass().getClassLoader().getResourceAsStream("entity-noids.xml");
		HttpPost post = new HttpPost(ENDPOINT_ENTITY);
		post.setEntity(new InputStreamEntity(src, -1));
		logger.debug("ingesting entity at " + post.getURI().toASCIIString());
		HttpResponse resp = client.execute(post);
		if (resp.getStatusLine().getStatusCode() != 200) {
			logger.error(IOUtils.toString(resp.getEntity().getContent()));
			post.releaseConnection();
			fail("server returned " + resp.getStatusLine().getStatusCode());
		}

		/* get the pid from the response */
		String id = TestUtil.getPidFromResponse(resp);
		logger.debug("ingested object with id " + id);

		post.releaseConnection();

		/* load the intellectual entity from the local xml document for comparison */
		src = this.getClass().getClassLoader().getResourceAsStream("entity-noids.xml");
		IntellectualEntity orig = ScapeMarshaller.newInstance().deserialize(IntellectualEntity.class, src);
		IOUtils.closeQuietly(src);

		Thread.sleep(1000);

		/* fetch the newly ingested entity from escidoc */
		HttpGet get = new HttpGet(ENDPOINT_ENTITY + "/" + id);
		logger.debug("fetching entity from " + get.getURI());
		resp = client.execute(get);
		if (resp.getStatusLine().getStatusCode() != 200) {
			logger.error(IOUtils.toString(resp.getEntity().getContent()));
			get.releaseConnection();
			fail("server returned " + resp.getStatusLine().getStatusCode());
		}
		IntellectualEntity fetched = ScapeMarshaller.newInstance().deserialize(IntellectualEntity.class, resp.getEntity().getContent());
		get.releaseConnection();
	}

	@Test
	public void retrieveMetadata() throws Exception {
		ScapeMarshaller marshaller = ScapeMarshaller.newInstance();
		/* ingest a test entity */
		IntellectualEntity e = TestUtil.createTestEntity();
		ByteArrayOutputStream sink = new ByteArrayOutputStream();
		marshaller.serialize(e, sink);
		HttpPost post = new HttpPost(ENDPOINT_ENTITY);
		post.setEntity(new InputStreamEntity(new ByteArrayInputStream(sink.toByteArray()), -1));
		logger.debug("ingesting entity at " + post.getURI().toASCIIString());
		HttpResponse resp = client.execute(post);
		if (resp.getStatusLine().getStatusCode() != 200) {
			logger.error(IOUtils.toString(resp.getEntity().getContent()));
			post.releaseConnection();
			fail("server returned " + resp.getStatusLine().getStatusCode());
		}

		/* get the pid from the response */
		String id = TestUtil.getPidFromResponse(resp);
		logger.debug("ingested object with id " + id);
		post.releaseConnection();

		/* Wait a bit for the metadata to be indexed */
		Thread.sleep(1000);

		/* fetch descriptive metadata from container */
		HttpGet get = new HttpGet(ENDPOINT_METADATA + "/" + id + "/DESCRIPTIVE/1");
		resp = client.execute(get);
		if (resp.getStatusLine().getStatusCode() != 200) {
			logger.error(IOUtils.toString(resp.getEntity().getContent()));
			get.releaseConnection();
			fail("server returned " + resp.getStatusLine().getStatusCode());
		}
		Object md = marshaller.deserialize(resp.getEntity().getContent());
		assertTrue("Object is not of DC metadata type as excpected", md instanceof ElementContainer);
		ElementContainer c = (ElementContainer) md;
		boolean dcSuccess = false;
		for (JAXBElement<SimpleLiteral> lit : c.getAny()) {
			if (lit.getName().getLocalPart().equals("title") && lit.getValue().getContent().get(0).equals("Object 1")) {
				dcSuccess = true;
			}
		}
		assertTrue("DC record could not be verified", dcSuccess);
		get.releaseConnection();
	}

	@Test
	public void retrieveIntellectualEntitySet() throws Exception {
		List<String> pids = new ArrayList<String>();
		ScapeMarshaller marshaller = ScapeMarshaller.newInstance();

		/* ingest test entity 1 */
		IntellectualEntity e = TestUtil.createTestEntity();
		ByteArrayOutputStream sink = new ByteArrayOutputStream();
		marshaller.serialize(e, sink);
		HttpPost post = new HttpPost(ENDPOINT_ENTITY);
		post.setEntity(new InputStreamEntity(new ByteArrayInputStream(sink.toByteArray()), -1));
		logger.debug("ingesting entity at " + post.getURI().toASCIIString());
		HttpResponse resp = client.execute(post);
		if (resp.getStatusLine().getStatusCode() != 200) {
			logger.error(IOUtils.toString(resp.getEntity().getContent()));
			post.releaseConnection();
			fail("server returned " + resp.getStatusLine().getStatusCode());
		}

		/* get the pid from the response */
		String id = TestUtil.getPidFromResponse(resp);
		logger.debug("ingested object with id " + id);
		pids.add(id);
		post.releaseConnection();

		/* ingest test entity 2 */
		e = TestUtil.createTestEntity();
		sink = new ByteArrayOutputStream();
		marshaller.serialize(e, sink);
		post = new HttpPost(ENDPOINT_ENTITY);
		post.setEntity(new InputStreamEntity(new ByteArrayInputStream(sink.toByteArray()), -1));
		logger.debug("ingesting entity at " + post.getURI().toASCIIString());
		resp = client.execute(post);
		if (resp.getStatusLine().getStatusCode() != 200) {
			logger.error(IOUtils.toString(resp.getEntity().getContent()));
			post.releaseConnection();
			fail("server returned " + resp.getStatusLine().getStatusCode());
		}

		/* get the pid from the response */
		id = TestUtil.getPidFromResponse(resp);
		logger.debug("ingested object with id " + id);
		pids.add(id);
		post.releaseConnection();

		/* Wait a bit for the metadata to be indexed */
		Thread.sleep(5000);

		/* retrieve the set consisting of the two entities */
		post = new HttpPost(ENDPOINT_ENTITY_SET);
		post.setEntity(new StringEntity("/scape/entity/" + pids.get(0) + "\n" + "/scape/entity/" + pids.get(1)));
		resp = client.execute(post);
		if (resp.getStatusLine().getStatusCode() != 200) {
			logger.error(IOUtils.toString(resp.getEntity().getContent()));
			post.releaseConnection();
			fail("server returned " + resp.getStatusLine().getStatusCode());
		}
		IntellectualEntityCollection coll = marshaller.deserialize(IntellectualEntityCollection.class, resp.getEntity().getContent());
		post.releaseConnection();
		assertTrue("Not all entities could be read from the server", coll.getEntities().size() == 2);
	}

	@Test
	public void ingestAndRetrieveIntellectualEntityAsync() throws Exception {
		ScapeMarshaller marshaller = ScapeMarshaller.newInstance();
		/* ingest test entity 1 */
		IntellectualEntity e = TestUtil.createTestEntity();
		ByteArrayOutputStream sink = new ByteArrayOutputStream();
		marshaller.serialize(e, sink);
		HttpPost post = new HttpPost(ENDPOINT_ENTITY_ASYNC);
		post.setEntity(new InputStreamEntity(new ByteArrayInputStream(sink.toByteArray()), -1));
		logger.debug("ingesting entity at " + post.getURI().toASCIIString());
		HttpResponse resp = client.execute(post);
		if (resp.getStatusLine().getStatusCode() != 200) {
			logger.error(IOUtils.toString(resp.getEntity().getContent()));
			post.releaseConnection();
			fail("server returned " + resp.getStatusLine().getStatusCode());
		}
		String id = TestUtil.getPidFromResponse(resp);
		logger.debug("ingested entity with id " + id);
		post.releaseConnection();

		HttpGet get = new HttpGet(ENDPOINT_LIFECYCLE + "/" + id);
		resp = client.execute(get);
		if (resp.getStatusLine().getStatusCode() != 200) {
			logger.error(IOUtils.toString(resp.getEntity().getContent()));
			post.releaseConnection();
			fail("server returned " + resp.getStatusLine().getStatusCode());
		}
		System.out.println(IOUtils.toString(resp.getEntity().getContent()));
//		LifecycleState state = (LifecycleState) marshaller.deserialize(resp.getEntity().getContent());
		get.releaseConnection();

	}

	@Test
	public void retrieveFile() throws Exception {
        ScapeMarshaller marshaller = ScapeMarshaller.newInstance();
        
        /* ingest test entity 1 */
        IntellectualEntity e = TestUtil.createTestEntity();
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        marshaller.serialize(e, sink);
        HttpPost post = new HttpPost(ENDPOINT_ENTITY);
        post.setEntity(new InputStreamEntity(new ByteArrayInputStream(sink.toByteArray()), -1));
        logger.debug("ingesting entity at " + post.getURI().toASCIIString());
        HttpResponse resp = client.execute(post);
        if (resp.getStatusLine().getStatusCode() != 200) {
            logger.error(IOUtils.toString(resp.getEntity().getContent()));
            post.releaseConnection();
            fail("server returned " + resp.getStatusLine().getStatusCode());
        }
        String id = TestUtil.getPidFromResponse(resp);
        
        /* wait a bit so so escidoc has time to add the entity to it's index */
        Thread.sleep(1000);

        /* fetch the newly ingested entity from escidoc */
        HttpGet get = new HttpGet(ENDPOINT_ENTITY + "/" + id);
        logger.debug("fetching entity from " + get.getURI());
        resp = client.execute(get);
        if (resp.getStatusLine().getStatusCode() != 200) {
            logger.error(IOUtils.toString(resp.getEntity().getContent()));
            get.releaseConnection();
            fail("server returned " + resp.getStatusLine().getStatusCode());
        }
        IntellectualEntity fetched = ScapeMarshaller.newInstance().deserialize(IntellectualEntity.class, resp.getEntity().getContent());
        get.releaseConnection();

        File f = fetched.getRepresentations().get(0).getFiles().get(0);
        get = new HttpGet(ENDPOINT_FILE + "/" + f.getIdentifier().getValue());
        client.execute(get);
        if (resp.getStatusLine().getStatusCode() != 200) {
            logger.error(IOUtils.toString(resp.getEntity().getContent()));
            get.releaseConnection();
            fail("server returned " + resp.getStatusLine().getStatusCode());
        }
        get.releaseConnection();
        
	}

	@Test
	public void searchIntellectualEntity() throws Exception {
        List<String> pids = new ArrayList<String>();
        ScapeMarshaller marshaller = ScapeMarshaller.newInstance();

        /* ingest test entity 1 */
        IntellectualEntity e = TestUtil.createTestEntity();
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        marshaller.serialize(e, sink);
        HttpPost post = new HttpPost(ENDPOINT_ENTITY);
        post.setEntity(new InputStreamEntity(new ByteArrayInputStream(sink.toByteArray()), -1));
        logger.debug("ingesting entity at " + post.getURI().toASCIIString());
        HttpResponse resp = client.execute(post);
        if (resp.getStatusLine().getStatusCode() != 200) {
            logger.error(IOUtils.toString(resp.getEntity().getContent()));
            post.releaseConnection();
            fail("server returned " + resp.getStatusLine().getStatusCode());
        }

        /* get the pid from the response */
        String id = TestUtil.getPidFromResponse(resp);
        logger.debug("ingested object with id " + id);
        pids.add(id);
        post.releaseConnection();

        /* ingest test entity 2 */
        e = TestUtil.createTestEntity();
        sink = new ByteArrayOutputStream();
        marshaller.serialize(e, sink);
        post = new HttpPost(ENDPOINT_ENTITY);
        post.setEntity(new InputStreamEntity(new ByteArrayInputStream(sink.toByteArray()), -1));
        logger.debug("ingesting entity at " + post.getURI().toASCIIString());
        resp = client.execute(post);
        if (resp.getStatusLine().getStatusCode() != 200) {
            logger.error(IOUtils.toString(resp.getEntity().getContent()));
            post.releaseConnection();
            fail("server returned " + resp.getStatusLine().getStatusCode());
        }

        /* get the pid from the response */
        id = TestUtil.getPidFromResponse(resp);
        logger.debug("ingested object with id " + id);
        pids.add(id);
        post.releaseConnection();
        
        HttpGet get = new HttpGet(ENDPOINT_ENTITY_SEARCH);
        resp = client.execute(get);
        IntellectualEntityCollection coll = marshaller.deserialize(IntellectualEntityCollection.class, resp.getEntity().getContent());
        assertTrue(coll.getEntities().size() >= 2);
        get.releaseConnection();
	}


	@Test
	public void retrieveEntityLifecycleState() throws Exception {
	    ScapeMarshaller marshaller = ScapeMarshaller.newInstance();
        InputStream src = this.getClass().getClassLoader().getResourceAsStream("entity-noids.xml");
        HttpPost post = new HttpPost(ENDPOINT_ENTITY);
        post.setEntity(new InputStreamEntity(src, -1));
        logger.debug("ingesting entity at " + post.getURI().toASCIIString());
        HttpResponse resp = client.execute(post);
        if (resp.getStatusLine().getStatusCode() != 200) {
            logger.error(IOUtils.toString(resp.getEntity().getContent()));
            post.releaseConnection();
            fail("server returned " + resp.getStatusLine().getStatusCode());
        }

        /* get the pid from the response */
        String id = TestUtil.getPidFromResponse(resp);
        logger.debug("ingested object with id " + id);

        post.releaseConnection();
        
        Thread.sleep(3000);
        
        /* now check for the lifecycle state */
        HttpGet get = new HttpGet(ENDPOINT_LIFECYCLE + "/" + id);
        resp = client.execute(get);
        if (resp.getStatusLine().getStatusCode() != 200) {
            logger.error(IOUtils.toString(resp.getEntity().getContent()));
            get.releaseConnection();
            fail("server returned " + resp.getStatusLine().getStatusCode());
        }
        LifecycleState state = (LifecycleState) marshaller.getJaxbUnmarshaller().unmarshal(resp.getEntity().getContent());
        get.releaseConnection();
	}

    @Test
    public void updateAndRetrieveIntellectualEntity() throws Exception {
        InputStream src = this.getClass().getClassLoader().getResourceAsStream("entity-noids.xml");
        HttpPost post = new HttpPost(ENDPOINT_ENTITY);
        post.setEntity(new InputStreamEntity(src, -1));
        logger.debug("ingesting entity at " + post.getURI().toASCIIString());
        HttpResponse resp = client.execute(post);
        if (resp.getStatusLine().getStatusCode() != 200) {
            logger.error(IOUtils.toString(resp.getEntity().getContent()));
            post.releaseConnection();
            fail("server returned " + resp.getStatusLine().getStatusCode());
        }

        /* get the pid from the response */
        String id = TestUtil.getPidFromResponse(resp);
        logger.debug("ingested object with id " + id);
        post.releaseConnection();
        
        /* wait a bit so escidoc can index the entity */
        Thread.sleep(1000);
        
        /* update the entity */
        src = this.getClass().getClassLoader().getResourceAsStream("entity-noids.xml");
        HttpPut put = new HttpPut(ENDPOINT_ENTITY + "/" + id);
        put.setEntity(new InputStreamEntity(src, -1));
        resp = client.execute(put);
        if (resp.getStatusLine().getStatusCode() != 200) {
            logger.error(IOUtils.toString(resp.getEntity().getContent()));
            post.releaseConnection();
            fail("server returned " + resp.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void retrieveEntityVersionList() throws Exception {
        fail("Test not yet implemented");
    }
    @Test
    public void retrieveBitStream() throws Exception {
        fail("Test not yet implemented");
    }
    @Test
    public void searchFiles() throws Exception {
        fail("Test not yet implemented");
    }

    @Test
	public void retrieveRepresentation() throws Exception {
        fail("Test not yet implemented");
	}

	@Test
	public void updateRepresentation() throws Exception {
        fail("Test not yet implemented");
	}

	@Test
	public void updateMetadata() throws Exception {
        fail("Test not yet implemented");
	}

}
