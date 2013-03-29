package eu.scapeproject.integration.escidoc;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.impl.cookie.BrowserCompatSpec;
import org.apache.http.impl.cookie.BrowserCompatSpecFactory;
import org.apache.http.impl.cookie.CookieSpecBase;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.scapeproject.model.IntellectualEntity;
import eu.scapeproject.util.ScapeMarshaller;

public class IntellectualentityIT {

	private static final Logger logger = LoggerFactory.getLogger(IntellectualentityIT.class);

	private static final String ESCIDOC_URI = "http://localhost:8080";
	private static final String ENDPOINT_AUTH = ESCIDOC_URI + "/aa/j_spring_security_check";
	private static final String ENDPOINT_LOGIN = ESCIDOC_URI + "/aa/login";
	private static final String ENDPOINT_ENTITY = ESCIDOC_URI + "/scape/entity";
	private static final String ESCIDOC_USER = "sysadmin";
	private static final String ESCIDOC_PASS = "sys";

	private static DefaultHttpClient client = new DefaultHttpClient();

	@BeforeClass
	public static void setup() throws Exception {
		CookieStore cs = client.getCookieStore();

		/* first a GET to the login url to get a cookie */
		System.out.println("--------------------GET--------------------------");
		System.out.println("-------------------------------------------------");
		HttpGet get = new HttpGet(ENDPOINT_LOGIN);
		HttpResponse resp = client.execute(get);
		System.out.println("Response code: " + resp.getStatusLine());
		get.releaseConnection();
		for (Cookie c: client.getCookieStore().getCookies()){
			System.out.println(c.getName() + ": " + c.getValue());
		}
		
		/* then POST the username password combo using the cookie from the previuous GET */
		System.out.println("--------------------POST-------------------------");
		System.out.println("-------------------------------------------------");
		HttpPost post = new HttpPost(ENDPOINT_AUTH);
		post.setEntity(new StringEntity("j_username=" + ESCIDOC_USER + "&j_password=" + ESCIDOC_PASS));
		post.setHeader("Content-Type","application/x-www-form-urlencoded");
		resp = client.execute(post);
		post.releaseConnection();
		for (Cookie c: client.getCookieStore().getCookies()){
			System.out.println(c.getName() + ": " + c.getValue());
		}

		/* now get the login url *AGAIN* to get a escidoc token */
		System.out.println("--------------------GET--------------------------");
		System.out.println("-------------------------------------------------");
		get = new HttpGet(ENDPOINT_LOGIN);
		resp = client.execute(get);
		System.out.println("Response code: " + resp.getStatusLine());
		get.releaseConnection();
		for (Cookie c: client.getCookieStore().getCookies()){
			System.out.println(c.getName() + ": " + c.getValue());
		}
	}

	@Test
	public void ingestAndRetrieveIntellectualEntity() throws Exception {
		InputStream src = this.getClass().getClassLoader().getResourceAsStream("entity_serialized.xml");
		HttpPost post = new HttpPost(ENDPOINT_ENTITY);
		post.setEntity(new InputStreamEntity(src, -1));
		logger.debug("ingesting entity at " + post.getURI().toASCIIString());
		HttpResponse resp = client.execute(post);
		assertTrue("Server returned " + resp.getStatusLine().toString(), resp.getStatusLine().getStatusCode() == 200);

		/* get the pid from the response */
		String id = IOUtils.toString(resp.getEntity().getContent());
		EntityUtils.consumeQuietly(resp.getEntity());
		id = id.substring(id.indexOf("<scape:value>") + 13, id.indexOf("</scape:value")).trim();
		logger.debug("ingested object with id " + id);

		post.releaseConnection();

		/* load the intellectual entity from the local xml document for comparison */
		src = this.getClass().getClassLoader().getResourceAsStream("entity_serialized.xml");
		IntellectualEntity orig = ScapeMarshaller.newInstance().deserialize(IntellectualEntity.class, src);
		IOUtils.closeQuietly(src);

		Thread.sleep(1000);

		/* fetch the newly ingested entity from escidoc */
		HttpGet get = new HttpGet(ENDPOINT_ENTITY + "/" + id);
		logger.debug("fetching entity from " + get.getURI());
		resp = client.execute(get);
		if (resp.getStatusLine().getStatusCode() != 200) {
			logger.error(IOUtils.toString(resp.getEntity().getContent()));
		}
		assertTrue("Server returned " + resp.getStatusLine().toString(), resp.getStatusLine().getStatusCode() == 200);
		IntellectualEntity fetched = ScapeMarshaller.newInstance().deserialize(IntellectualEntity.class, resp.getEntity().getContent());
		get.releaseConnection();
		/* compare the fetched with the local entity */
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
