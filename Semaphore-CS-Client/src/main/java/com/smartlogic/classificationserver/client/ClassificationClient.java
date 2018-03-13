package com.smartlogic.classificationserver.client;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.IOUtils;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.FormBodyPart;
import org.apache.http.entity.mime.FormBodyPartBuilder;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * General purpose client for the classification server
 *
 * @author Smartlogic Semaphore
 *
 */
public class ClassificationClient {
	public static Logger logger = LoggerFactory.getLogger(ClassificationClient.class);
	
	/* Methods that are classification requests */
	

	/**
	 * Determine the version information as generated by Classification Server
	 * @return
	 * @throws ClassificationException
	 */
	public String getVersion() throws ClassificationException {
		logger.debug("getVersion - entry");

		String commandString = getCommandXML("version", null);
		CSVersion version = new CSVersion(sendPostRequest(commandString, null));
		return version.getVersion();
	}

	/**
	 * Return the rulebase classes that are currently configured on the
	 * classification server instance
	 *
	 * @return List of rulebases classes
	 * @throws ClassificationException Classification exception
	 */
	public Collection<RulebaseClass> getRulebaseClasses() throws ClassificationException {
		String commandString = getCommandXML("listrulenetclasses", null);
		RulebaseClassSet rulebaseClassSet = new RulebaseClassSet(sendPostRequest(commandString, null));
		return rulebaseClassSet.getRulebaseClasses();
	}

	/**
	 * Clear out a publish set so that new pack files can be uploaded.
	 * Until the publish set is committed, this will have no effect on what is currently live
	 * @param publishSetName
	 * @throws ClassificationException
	 */
	public void clearPublishSet(String publishSetName) throws ClassificationException {
		String commandString = getCommandXML("publish_set_init", publishSetName);
		sendPostRequest(commandString, null);
	}

	/**
	 * Upload the collection of pakfiles to the named publish set
	 * Until the publish set is committed, this will have no effect on what is currently live
	 * @param publishSetName
	 * @param pakFilePaths
	 * @throws ClassificationException
	 */
	public void sendPakfiles(String publishSetName, Collection<File> pakFiles) throws ClassificationException {
		for (File pakFile : pakFiles) {
			sendPakFile(publishSetName, pakFile);
		}
	}

	/**
	 * Upload the pakfile to the named publish set
	 * Until the publish set is committed, this will have no effect on what is currently live
	 * @param publishSetName
	 * @param pakFilePath
	 * @throws ClassificationException
	 */
	public void sendPakFile(String publishSetName, File pakFile) throws ClassificationException {
		String commandString = getCommandXML("publish_set_add", publishSetName);
		sendPostRequest(commandString, pakFile);
	}


	/**
	 * Instruct that a particular publish set should become live
	 * This command will affect the classification result 
	 * @param publishSetName
	 * @throws ClassificationException
	 */
	public void commitPublishSet(String publishSetName) throws ClassificationException {
		String commandString = getCommandXML("publish_set", publishSetName);
		sendPostRequest(commandString, null);
	}
	
	/**
	 * Remove a particular publish set from the classification servers rulebase set.
	 * @param publishSetName
	 * @throws ClassificationException
	 */
	public void deactivatePublishSet(String publishSetName) throws ClassificationException {
		String commandString = getCommandXML("publish_set_deactivate", publishSetName);
		sendPostRequest(commandString, null);
	}

	/**
	 * Return the information that CS makes available.
	 * @return Classification Server information
	 * @throws ClassificationException Classification exception
	 */
	public CSInfo getInfo() throws ClassificationException {
		logger.debug("getInfo");

		CSInfo csInfo = new CSInfo(sendPostRequest(getCommandXML("info", null), null));
		return csInfo;
	}

	/**
	 * Return the list of languages available on the cs instance
	 *
	 * @return List of languages
	 * @throws ClassificationException Classification exception
	 */
	public Collection<Language> getLanguages() throws ClassificationException {
		logger.debug("getLanguages - entry");

		LanguageSet langSet = new LanguageSet(sendPostRequest(getCommandXML("listlanguages", null), null));
		return langSet.getLanguages();
	}

	/**
	 * Return the map of default parameter values
	 *
	 * @return Map of default parameter values
	 * @throws ClassificationException Classification exception
	 */
	public Map<String, Parameter> getDefaults() throws ClassificationException {
		logger.debug("getDefaults - entry");
		Defaults defaults = new Defaults(sendPostRequest(getCommandXML("getparameterdefaults", null), null));
		return defaults.getDefaults();
	}
	
	/**
	 * Return the status of the classification server instance
	 *
	 * @return A classification status object
	 * @throws ClassificationException Classification exception
	 */
	@Deprecated // This response appears pretty useless
	public ClassificationServerStatus status() throws ClassificationException {
		if (logger.isDebugEnabled())
			logger.debug("status - entry");

		ClassificationServerStatus status = new ClassificationServerStatus(sendPostRequest(getCommandXML("stats", null), null));
		return status;

	}


	/* Plain getters and setters for this object */	
	
	private ClassificationConfiguration classificationConfiguration;
	/**
	 * Get the configuration of the classification server
	 *
	 * @return The configuration
	 */
	public ClassificationConfiguration getClassificationConfiguration() {
		return classificationConfiguration;
	}

	/**
	 * Set the configuration of the classification server
	 *
	 * @param classificationConfiguration The configuration to use
	 */
	public void setClassificationConfiguration(
			ClassificationConfiguration classificationConfiguration) {
		this.classificationConfiguration = classificationConfiguration;
	}

	private UUID auditUUID = null;
	/**
	 * Return the UUID object used to tag the request
	 *
	 * @return The UUID object
	 */
	public UUID getAuditUUID() {
		return auditUUID;
	}

	/**
	 * Set a UUID object that will be used to tag the request. If configured,
	 * this will be stored in the classification server log and so can be used
	 * for auditing purposes.
	 *
	 * @param auditGUID The audit GUID to use
	 */
	public void setAuditUUID(UUID auditGUID) {
		this.auditUUID = auditGUID;
	}

	private String proxyHost = null;

	/**
	 * The name of the proxy host in use.
	 *
	 * @return The proxy host. Null if no proxy is in use (the default)
	 */
	@Deprecated
	public String getProxyHost() {
		return proxyHost;
	}

	/**
	 * Set the proxy host to be used for all requests
	 *
	 * @param proxyHost The proxy host to use
	 */
	@Deprecated
	public void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}

	private int proxyPort;
	/**
	 * The port of the proxy being used
	 *
	 * @return The port number
	 */
	@Deprecated
	public int getProxyPort() {
		return proxyPort;
	}

	/**
	 * The port of the proxy being used
	 *
	 * @param proxyPort The port number to use
	 */
	@Deprecated
	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
	}
	
	
	private String proxyURL;
	private String getProxyURL() {
		if (proxyURL == null) {
			if ((proxyHost != null) && (proxyPort != 0)) {
				proxyURL = "http://" + proxyHost + ":" + proxyPort;
			}
		}
		return proxyURL;
	}
	
	public void setProxyURL(String proxyURL) {
		this.proxyURL = proxyURL;
	}

	/* Classification requests */

	/**
	 * Classify the supplied file
	 *
	 * @param inputFile The input file to classify
	 * @param fileType File type of "inputFile". If the file type is not supplied (i.e. is null) then it will be guessed by classification server.
	 * @return the classifications as returned by classification server.
	 * @throws ClassificationException Classification exception
	 */
	public Result getClassifiedDocument(File inputFile, String fileType) throws ClassificationException {
		return new Result(getStructuredDocument(inputFile, fileType));
	}
	
	public Document getStructuredDocument(File inputFile, String fileType) throws ClassificationException {
		return getStructuredDocument(inputFile, fileType, null, null);
	}

	/**
	 * Classify the supplied title and body as if they were a document
	 *
	 * @param fileName The file name of the document to classify
	 * @param title The document title
	 * @param body The document body
	 * @return the classifications as returned by classification server.
	 * @throws ClassificationException Classification exception
	 */
	public Result getClassifiedDocument(FileName fileName, Body body, Title title) throws ClassificationException {
		return new Result(getStructuredDocument(fileName, body, title));
	}

	public Document getStructuredDocument(FileName fileName, Body body, Title title) throws ClassificationException {
		return getStructuredDocument(fileName, body, title, null);
	}

	/**
	 * Classify the supplied title and body as if they were a document
	 *
	 * @param title The document title
	 * @param body The document body
	 * @return the classifications as returned by classification server.
	 * @throws ClassificationException Classification exception
	 */
	public Result getClassifiedDocument(Body body, Title title) throws ClassificationException {
		return new Result(getStructuredDocument(body, title));
	}
	
	public Document getStructuredDocument(Body body, Title title) throws ClassificationException {
		return getStructuredDocument(null, body, title, null);
	}
	
	/**
	 * Classify the supplied title and body as if they were a document
	 * @param fileName The file name of the document to classify
	 * @param body The document body
	 * @param title The document title
	 * @param metadata Map containing metadata
	 * @return the classifications as returned by classification server.
	 * @throws ClassificationException Classification exception
	 */
	public Result getClassifiedDocument(FileName fileName, Body body, Title title, Map<String, Collection<String>> metadata) throws ClassificationException {
		return new Result(getStructuredDocument(fileName, body, title, metadata));
	}
	
	public Document getStructuredDocument(FileName fileName, Body body, Title title, Map<String, Collection<String>> metadata) throws ClassificationException {
		logger.debug("Treating document: '" + title.getValue() + "'");

		// If there is no body, then don't bother attempting to classify the document
		if ((body == null) || (body.getValue() == null) || (body.getValue().trim().length() == 0)) {
			return getBlankStructuredDocument();
		}

		Collection<FormBodyPart> parts = new ArrayList<FormBodyPart>();

		addTitle(parts, title);
		addMetadata(parts, metadata);
		addByteArray(parts, body, fileName);
		return XMLReader.getDocument(getClassifications(parts));
	}
	
	public byte[] getClassificationServerResponse(FileName filename, Body body, Title title, Map<String, Collection<String>> metadata)
			throws ClassificationException {
		logger.debug("Treating document: '" + title.getValue() + "'");

		// If there is no body, then don't bother attempting to classify the
		// document
		if ((body == null) || (body.getValue() == null)
				|| (body.getValue().trim().length() == 0)) {
			return new byte[0];
		}

		Collection<FormBodyPart> parts = new ArrayList<FormBodyPart>();

		addTitle(parts, title);
		addMetadata(parts, metadata);
		addByteArray(parts, body, filename);
		return getClassificationServerResponse(parts);
	}

	/**
	 * Classify the supplied title and body as if they were a document
	 *
	 * @param body The document body
	 * @param title The document title
	 * @param metadata Map containing metadata
	 * @return the classifications as returned by classification server.
	 * @throws ClassificationException Classification exception
	 */
	public Result getClassifiedDocument(Body body, Title title, Map<String, Collection<String>> metadata) throws ClassificationException {
		return new Result(getStructuredDocument(body, title, metadata));
	}

	public Document getStructuredDocument(Body body, Title title, Map<String, Collection<String>> metadata) throws ClassificationException {
		return getStructuredDocument(null, body, title, metadata);
	}
	/**
	 * Classify the supplied url
	 *
	 * @param url The URL to classify
	 * @return the classifications as returned by classification server.
	 * @throws ClassificationException Classification exception
	 */
	public Result getClassifiedDocument(URL url) throws ClassificationException {
		return new Result(getStructuredDocument(url));
	}
	
	public Document getStructuredDocument(URL url) throws ClassificationException {
		return getStructuredDocument(url, null, null);
	}

	/**
	 * Classify the supplied url with the extra metadata
	 *
	 * @param url The URL to classify
	 * @param title The document title
	 * @param metadata Map containing metadata
	 * @return the classifications as returned by classification server.
	 * @throws ClassificationException Classification exception
	 */
	public Result getClassifiedDocument(URL url, Title title, Map<String, Collection<String>> metadata) throws ClassificationException {
		return new Result(getStructuredDocument(url, title, metadata));
	}

	public Document getStructuredDocument(URL url, Title title, Map<String, Collection<String>> metadata) throws ClassificationException {
		Collection<FormBodyPart> parts = new ArrayList<FormBodyPart>();
		addTitle(parts, title);
		addMetadata(parts, metadata);
		parts.add(getFormPart("path", url.toExternalForm()));
		return XMLReader.getDocument(getClassifications(parts));
	}
	

	private final static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
	/**
	 * Return the classification records for all requests between the two
	 * supplied dates
	 *
	 * @param startTime The earliest possible date for returned results
	 * @param endTime The latest possible date for returned results
	 * @return One record for each document classified in that date range
	 * @throws ClassificationException Classification exception
	 */
	public Collection<ClassificationRecord> getClassificationHistory(
			Date startTime, Date endTime) throws ClassificationException {
		logger.info("getClassificationHistory - entry");

		ArrayList<FormBodyPart> partsList = new ArrayList<FormBodyPart>();
		partsList.add(getFormPart("start_time", simpleDateFormat.format(startTime)));
		partsList.add(getFormPart("finish_time", simpleDateFormat.format(endTime)));
		partsList.add(getFormPart("operation", "getclassificationhistory"));

		ClassificationHistory classificationHistory = new ClassificationHistory(getClassificationServerResponse(partsList));
		return classificationHistory.getClassificationRecords();
	}



	public byte[] getClassificationServerResponse(Body body, Title title) throws ClassificationException {
		return getClassificationServerResponse(null, body, title, null);
	}

	/**
	 * Return directly the output from classification server with no analysis
	 *
	 * @param inputFile The input file to classify
	 * @param fileType File type of "inputFile". If the file type is not supplied (i.e. is null) then it will be guessed by classification server.
	 * @return The classification server response
	 * @throws ClassificationException Classification exception
	 */
	public byte[] getClassificationServerResponse(File inputFile, String fileType) throws ClassificationException {
		return getClassificationServerResponse(inputFile, fileType, null, null);
	}

	/**
	 * Return in a structured form the output of the classification process
	 *
	 * @param data Data to classify
	 * @param fileName A string containing the name of the file to classify
	 * @return The structured result of the classification
	 * @throws ClassificationException Classification exception
	 */
	public Result getClassifiedDocument(byte[] data, String fileName) throws ClassificationException {
		return new Result(getStructuredDocument(data, fileName));
	}
	
	public Document getStructuredDocument(byte[] data, String fileName) throws ClassificationException {
		Collection<FormBodyPart> parts = new ArrayList<FormBodyPart>();

		if ((data == null) || (data.length == 0)) return getBlankStructuredDocument();

		addByteArray(parts, data, fileName);

		return XMLReader.getDocument(getClassificationServerResponse(parts));
	}

	/**
	 * Return in a structured form the output of the classification process
	 *
	 * @param data Data to classify
	 * @param fileName A string containing the name of the file to classify
	 * @param title The document title
	 * @param metadata Map containing metadata
	 * @return The structured result of the classification
	 * @throws ClassificationException Classification exception
	 */
	public Result getClassifiedDocument(byte[] data, String fileName,  Title title, Map<String, Collection<String>> metadata) throws ClassificationException {
		return new Result(getStructuredDocument(data, fileName, title, metadata));
	}
	
	public Document getStructuredDocument(byte[] data, String fileName,  Title title, Map<String, Collection<String>> metadata) throws ClassificationException {
		logger.debug("Treating file: '" + fileName + "'");
		Collection<FormBodyPart> parts = new ArrayList<FormBodyPart>();

		addTitle(parts, title);
		addMetadata(parts, metadata);
		addByteArray(parts, data, fileName);

		return XMLReader.getDocument(getClassificationServerResponse(parts));
	}

	/**
	 * Return in a structured form the output of the classification process
	 *
	 * @param inputFile The input file to classify
	 * @param fileType File type of "inputFile". If the file type is not supplied (i.e. is null) then it will be guessed by classification server.
	 * @param title The document title
	 * @param metadata Map containing metadata
	 * @return The structured result of the classification
	 * @throws ClassificationException Classification exception
	 */
	public Result getClassifiedDocument(File inputFile, String fileType, Title title, Map<String, Collection<String>> metadata)
			throws ClassificationException {
		return new Result(getStructuredDocument(inputFile, fileType, title, metadata));
	}
	
	public Document getStructuredDocument(File inputFile, String fileType, Title title, Map<String, Collection<String>> metadata) throws ClassificationException {
		Collection<FormBodyPart> parts = new ArrayList<FormBodyPart>();

		addTitle(parts, title);
		addMetadata(parts, metadata);
		addFile(parts, inputFile, fileType);

		return XMLReader.getDocument(getClassificationServerResponse(parts));
	}

	private void addTitle(Collection<FormBodyPart> parts, Title title) {
		if ((title != null) && (title.getValue() != null) && (title.getValue().length() > 0)) {
			parts.add(title.asFormPart());
		}
	}

	private void addByteArray(Collection<FormBodyPart> parts, Body body, FileName filename) {
		if (filename == null) {
			parts.add(body.asFormPart());
		} else {
			addByteArray(parts, body.getValue().getBytes(Charset.forName("UTF-8")), filename.getValue());
		}
	}

	private void addByteArray(Collection<FormBodyPart> parts, byte[] data, String fileName) {
		parts.add(FormBodyPartBuilder.create("UploadFile", new ByteArrayBody(data, fileName)).build());
	}

	private void addFile(Collection<FormBodyPart> parts, File inputFile, String fileType) throws ClassificationException {
		if (inputFile == null) {
			throw new ClassificationException("Null input file provided");
		}
		if (!inputFile.exists()) {
			throw new ClassificationException("Input file not found: " + inputFile.getAbsolutePath());
		}

		parts.add(getFormPart("UploadFile", inputFile));
	}


	private void addMetadata(Collection<FormBodyPart> parts,
			Map<String, Collection<String>> metadata) {
		if (metadata != null) {
			for (String name : metadata.keySet()) {
				Collection<String> values = metadata.get(name);
				if (values != null) {
					int m = 0;
					for (String value : values) {
						if (m == 0) parts.add(getFormPart("meta_" + name,  value));
						else parts.add(getFormPart("meta_" + name + "__" + m,  value));
						m++;
					}
				}
			}
		}
	}

	private MultipartEntityBuilder getDefaultParts() {
		MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();

		for (String parameterName : classificationConfiguration.getAdditionalParameters().keySet()) {
			String value = classificationConfiguration.getAdditionalParameters().get(parameterName);
			if ((value != null) && (value.length() > 0)) {
				multipartEntityBuilder.addPart(getFormPart(parameterName, value));
			}
		}
		if (classificationConfiguration.isSingleArticle())
			multipartEntityBuilder.addPart(getFormPart("singlearticle", "on"));
		if (classificationConfiguration.isMultiArticle())
			multipartEntityBuilder.addPart(getFormPart("multiarticle", "on"));
		if (classificationConfiguration.isFeedback())
			multipartEntityBuilder.addPart(getFormPart("feedback", "on"));
		if (classificationConfiguration.isStylesheet())
			multipartEntityBuilder.addPart(getFormPart("stylesheet", "on"));
		if (classificationConfiguration.isUseGeneratedKeys())
			multipartEntityBuilder.addPart(getFormPart("use_generated_keys", "on"));
		if (classificationConfiguration.isReturnHashCode())
			multipartEntityBuilder.addPart(getFormPart("return_hash", "on"));
		return multipartEntityBuilder;
	}

	private final static ContentType contentType = ContentType.create("text/plain", Consts.UTF_8);
	private static FormBodyPart getFormPart(String name, String value) {
		return FormBodyPartBuilder.create(name, new StringBody(value, contentType)).build();
	}
	private static FormBodyPart getFormPart(String name, File file) {
		return FormBodyPartBuilder.create(name, new FileBody(file)).build();
	}

	private byte[] getClassificationServerResponse(Collection<FormBodyPart> parts) throws ClassificationException {
	
		MultipartEntityBuilder multipartEntityBuilder = getDefaultParts();
		for (FormBodyPart part: parts) multipartEntityBuilder.addPart(part);
		
		if (this.getAuditUUID() != null) {
			multipartEntityBuilder.addPart(getFormPart("audit_tag", this.getAuditUUID().toString()));
		}
		
		byte[] returnedData = sendPostRequest(multipartEntityBuilder.build());
			
		logger.debug("getClassificationServerResponse - exit: " + returnedData.length);
		return returnedData;
	}

	private byte[] getClassifications(Collection<FormBodyPart> partsList) throws ClassificationException {
		return getClassifications(partsList, null);
	}

	private byte[]  getClassifications(Collection<FormBodyPart> partsList, Map<String, String> outMeta) throws ClassificationException {
		byte[] returnedData = getClassificationServerResponse(partsList);
		if ((returnedData != null) && (outMeta != null)) {
			Result result = new Result(XMLReader.getDocument(returnedData));
			if (result.getMetadata() != null) {
				for (String meta : result.getMetadata().keySet()) {
					outMeta.put(meta, result.getMetadata().get(meta));
				}
			}
		}
		return returnedData;
	}

	public byte[] getClassificationServerResponse(File inputFile, String fileType, Title title, Map<String, Collection<String>> metadata)
			throws ClassificationException {
		logger.debug("Treating file: '" + inputFile + "'");

		Collection<FormBodyPart> parts = new ArrayList<FormBodyPart>();
		addFile(parts, inputFile, fileType);

		addTitle(parts, title);
		addMetadata(parts, metadata);
		return getClassificationServerResponse(parts);
	}



	private DocumentBuilder documentBuilder = null;

	private String getCommandXML(String command, String publishSetName) throws ClassificationException {
		if (documentBuilder == null) {
			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			try {
				documentBuilder = documentBuilderFactory.newDocumentBuilder();
			} catch (ParserConfigurationException e) {
				throw new ClassificationException(
						String.format("ParserConfigurationException building CS command: %s %s - %s", command,
								publishSetName, e.getMessage()));
			}
		}
		Document document = documentBuilder.newDocument();
		Element requestElement = document.createElement("request");
		requestElement.setAttribute("op", command);
		document.appendChild(requestElement);

		if (publishSetName != null) {
			Element publishSetElement = document.createElement("publish_set");
			publishSetElement.appendChild(document.createTextNode(publishSetName));
			requestElement.appendChild(publishSetElement);
		}
		
		StringWriter stringWriter = new StringWriter();
		try {
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, "utf-8");
			DOMSource source = new DOMSource(document);
			StreamResult destination = new StreamResult(stringWriter);
			transformer.transform(source, destination);
		} catch (TransformerException e) {
			throw new ClassificationException(String.format("TransformerException building CS command: %s %s - %s",
					command, publishSetName, e.getMessage()));
		}
		return stringWriter.toString();
	}
	
	private byte[] sendPostRequest(String commandString, File pakFile) throws ClassificationException {

		MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();

		if (pakFile != null) {
			FormBodyPart filePart = FormBodyPartBuilder.create("UploadFile", new FileBody(pakFile)).build();
			multipartEntityBuilder.addPart(filePart);
		}

		FormBodyPart commandPart = FormBodyPartBuilder.create("XML_INPUT", new StringBody(commandString, ContentType.TEXT_XML)).build();
		multipartEntityBuilder.addPart(commandPart);
			
		return sendPostRequest(multipartEntityBuilder.build());
	}
	
	private boolean initialized = false;
	private PoolingHttpClientConnectionManager poolingConnectionManager;
	private RequestConfig requestConfig;
	
	private int clientPoolSize = 2;
	public int getClientPoolSize() {
		return clientPoolSize;
	}

	public void setClientPoolSize(int clientPoolSize) {
		this.clientPoolSize = clientPoolSize;
	}

	private List<CloseableHttpClient> availableClients = new Vector<CloseableHttpClient>();
	private IdleConnectionMonitorThread idleConnectionMonitorThread;
	private void initialize() {
		poolingConnectionManager = new PoolingHttpClientConnectionManager();
		poolingConnectionManager.setValidateAfterInactivity(0);
		poolingConnectionManager.setMaxTotal(clientPoolSize);
		
		
		// Make sure that idle and stale connections are discarded
		IdleConnectionMonitorThread idleConnectionMonitorThread = new IdleConnectionMonitorThread(poolingConnectionManager);
		idleConnectionMonitorThread.start();
		
		RequestConfig.Builder requestConfigBuilder = RequestConfig.copy(RequestConfig.DEFAULT)
				.setSocketTimeout(classificationConfiguration.getSocketTimeoutMS())
				.setConnectTimeout(classificationConfiguration.getConnectionTimeoutMS())
				.setConnectionRequestTimeout(classificationConfiguration.getConnectionTimeoutMS());
		if (getProxyURL() != null) {
			HttpHost proxy = HttpHost.create(getProxyURL());
			requestConfigBuilder.setProxy(proxy);
		}
		requestConfig = requestConfigBuilder.build();
		initialized = true;
		
		for (int i = 0; i < clientPoolSize; i++) {
			CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(requestConfig)
					.setSSLHostnameVerifier(new NoopHostnameVerifier())
					.setConnectionManager(poolingConnectionManager)
					.build();
			availableClients.add(httpClient);
		}
		
	}
	
	public void close() {
		idleConnectionMonitorThread.shutdown = true;
	}
	
	private CloseableHttpClient getClient() throws ClassificationException {
		CloseableHttpClient client;
		synchronized (availableClients) {
			while (availableClients.size() == 0) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					throw new ClassificationException("InterruptedException whilst waiting for client");
				}
			}
			client = availableClients.remove(0);
		}
		return client;
	}
	
	private void returnClient(CloseableHttpClient client) {
		availableClients.add(client);
	}

	private byte[] sendPostRequest(HttpEntity requestEntity) throws ClassificationException {
		if (!initialized) initialize();
		
		CloseableHttpClient httpClient = getClient();
		
		HttpPost httpPost = null;
		byte[] responseData;
		try {
			httpPost = new HttpPost(classificationConfiguration.getUrl());
			addHeaders(httpPost);
			httpPost.setEntity(requestEntity);

			
			try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
				if (response == null)
					throw new ClassificationException(
							"Null response from http client: " + classificationConfiguration.getUrl());
				if (response.getStatusLine() == null)
					throw new ClassificationException(
							"Null status line from http client: " + classificationConfiguration.getUrl());

				int statusCode = response.getStatusLine().getStatusCode();

				HttpEntity responseEntity = response.getEntity();

				logger.debug("Status: " + statusCode);
				ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				IOUtils.copy(responseEntity.getContent(), byteArrayOutputStream);
				responseData = byteArrayOutputStream.toByteArray();
				if (statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
					throw new ClassificationException(
							"Internal classification server error: " + new String(responseData, "UTF-8"));
				} else if (statusCode != HttpStatus.SC_OK) {
					throw new ClassificationException(
							"HttpStatus: " + statusCode + " received from classification server ("
											+ classificationConfiguration.getUrl() + ") " + new String(responseData, "UTF-8"));						
				}
			}
		} catch (ClientProtocolException e) {
			throw new ClassificationException(
					"ClientProtocolException talking to classification server" + e.getMessage());
		} catch (IOException e) {
			throw new ClassificationException("IOException talking to classification server" + e.getMessage());
		} finally {
			if (httpPost != null) {
				httpPost.abort();
			}
			returnClient(httpClient);
		}

		return responseData;
	}
	


	private static Document blankDocument = null;
	private final Document getBlankStructuredDocument() throws ClassificationException {
		if (blankDocument == null) 
			blankDocument = XMLReader.getDocument("<response><STRUCTUREDDOCUMENT/></response>".getBytes(StandardCharsets.UTF_8));
		return blankDocument;
	}

	private void addHeaders(HttpRequest httpRequest) {
		if (classificationConfiguration.getApiToken() != null) {
			logger.trace("Adding authorization header: {}", classificationConfiguration.getApiToken());
			httpRequest.addHeader("Authorization", classificationConfiguration.getApiToken());
		}
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder(this.getClass() .getCanonicalName() + "\n");
		stringBuilder.append("  Host Name: '" + this.getClassificationConfiguration().getHostName() + "'\n");
		stringBuilder.append("  Host Path: '" + this.getClassificationConfiguration().getHostPath() + "'\n");
		stringBuilder.append("  Host Port: '" + this.getClassificationConfiguration().getHostPort() + "'\n");
		stringBuilder.append("  Connection Timeout MS: '" + this.getClassificationConfiguration().getConnectionTimeoutMS() + "'\n");
		stringBuilder.append("  Socket Timeout MS: '" + this.getClassificationConfiguration().getSocketTimeoutMS() + "'\n");
		stringBuilder.append("  Protocol: '" + this.getClassificationConfiguration().getProtocol() + "'\n");
		stringBuilder.append("  Proxy Host: '" + this.getProxyHost() + "'\n");
		stringBuilder.append("  Proxy Port: '" + this.getProxyPort() + "'\n");
		return stringBuilder.toString();
	}

	public static class IdleConnectionMonitorThread extends Thread {
	    
	    private final HttpClientConnectionManager connMgr;
	    private volatile boolean shutdown;
	    
	    public IdleConnectionMonitorThread(HttpClientConnectionManager connMgr) {
	        super();
	        this.connMgr = connMgr;
	    }

	    @Override
	    public void run() {
	        try {
	            while (!shutdown) {
	                synchronized (this) {
	                    wait(5000);
	                    // Close expired connections
	                    connMgr.closeExpiredConnections();
	                    // Optionally, close connections
	                    // that have been idle longer than 30 sec
	                    connMgr.closeIdleConnections(30, TimeUnit.SECONDS);
	                }
	            }
	        } catch (InterruptedException ex) {
	            // terminate
	        }
	    }
	    
	    public void shutdown() {
	        shutdown = true;
	        synchronized (this) {
	            notifyAll();
	        }
	    }
	    
	}
}
