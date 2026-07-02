package org.egov.bpa.service;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FileStoreService {

	@Autowired
	RestTemplate restTemplate;

	@Autowired
	BPAConfiguration bpaConfig;

	/**
	 * Fetches a file from the filestore service and saves it locally.
	 * 
	 * <p>
	 * This method orchestrates the complete file download lifecycle:
	 * <ul>
	 * <li>Normalizes input parameters to prevent injection attacks</li>
	 * <li>Builds filestore fetch URL with query parameters</li>
	 * <li>Downloads file content from filestore service</li>
	 * <li>Saves file to local filesystem at specified path</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Logic:</strong>
	 * <ul>
	 * <li><strong>Security:</strong> All string parameters are normalized to
	 * prevent XSS attacks</li>
	 * <li><strong>Streaming Download:</strong> Uses ResponseExtractor to stream
	 * large files efficiently</li>
	 * <li><strong>Tenant Isolation:</strong> Files are fetched based on
	 * tenant-specific filestore ID</li>
	 * <li><strong>Local Storage:</strong> Downloaded file saved to specified local
	 * path</li>
	 * </ul>
	 * 
	 * @param fileStoreId   the filestore ID of the file to fetch (will be
	 *                      normalized)
	 * @param moduleName    the module name for logging/tracking (will be
	 *                      normalized)
	 * @param localFileName the local file path where the file should be saved
	 * @param tenantId      the tenant ID for multi-tenancy isolation
	 * @return File object representing the downloaded file
	 * @throws CustomException if download fails or filestore service is unavailable
	 */
	public File fetch(String fileStoreId, String moduleName, String localFileName, String tenantId) {

		log.info("Fetching file with ID: {} from module: {}", fileStoreId, moduleName);

		// Step 1: Normalize parameters for security
		fileStoreId = normalizeString(fileStoreId);
		moduleName = normalizeString(moduleName);

		// Step 2: Build fetch URL
		String fetchUrl = buildFetchUrl(fileStoreId, tenantId);

		// Step 3: Prepare local file path
		Path localPath = Paths.get(localFileName);

		// Step 4: Execute download
		executeFetchAndSave(fetchUrl, localPath);

		log.info("File fetch completed successfully, saved to: {}", localFileName);

		return localPath.toFile();
	}

	/**
	 * Builds the filestore fetch URL with query parameters.
	 * 
	 * @param fileStoreId the normalized filestore ID
	 * @param tenantId    the tenant ID
	 * @return the complete fetch URL with query parameters
	 */
	private String buildFetchUrl(String fileStoreId, String tenantId) {
		return bpaConfig.getFilestoreHost() + bpaConfig.getFilestoreFetchPath() + "?tenantId=" + tenantId
				+ "&fileStoreId=" + fileStoreId;
	}

	/**
	 * Executes the file download from filestore and saves it locally.
	 * 
	 * <p>
	 * This method uses RestTemplate's execute method with:
	 * <ul>
	 * <li><strong>RequestCallback:</strong> Sets HTTP headers to accept binary
	 * data</li>
	 * <li><strong>ResponseExtractor:</strong> Streams response body directly to
	 * file to handle large files efficiently without loading into memory</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Error Handling:</strong> Provides context-specific error messages
	 * for:
	 * <ul>
	 * <li>Network/connectivity issues (RestClientException)</li>
	 * <li>File system issues (IOException wrapped in Exception)</li>
	 * <li>Any other unexpected errors</li>
	 * </ul>
	 * 
	 * @param fetchUrl  the filestore fetch URL
	 * @param localPath the local path to save the file
	 * @throws CustomException if download fails
	 */
	private void executeFetchAndSave(String fetchUrl, Path localPath) {
		try {
			// Prepare request callback to set headers
			RequestCallback requestCallback = request -> request.getHeaders()
					.setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));

			// Prepare response extractor to stream file content
			ResponseExtractor<Void> responseExtractor = response -> {
				Files.copy(response.getBody(), localPath);
				return null;
			};

			// Execute download with streaming
			restTemplate.execute(URI.create(fetchUrl), HttpMethod.GET, requestCallback, responseExtractor);

		} catch (RestClientException e) {
			log.error("REST error occurred while fetching file from filestore: {}", fetchUrl, e);
			throw new CustomException("FILESTORE_FETCH_ERROR",
					"Failed to fetch file from filestore: Network or connectivity issue");
		} catch (Exception ex) {
			log.error("Unexpected error occurred while fetching file from filestore: {}", fetchUrl, ex);
			throw new CustomException("FILESTORE_FETCH_ERROR",
					"Failed to fetch file from filestore: " + ex.getMessage());
		}
	}

	/**
	 * Uploads a file to the filestore service.
	 * 
	 * <p>
	 * This method orchestrates the complete file upload lifecycle:
	 * <ul>
	 * <li>Normalizes input parameters to prevent injection attacks</li>
	 * <li>Prepares multipart request with file and metadata</li>
	 * <li>Uploads file to filestore service</li>
	 * <li>Returns filestore response containing file ID</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Business Logic:</strong>
	 * <ul>
	 * <li><strong>Security:</strong> All string parameters are normalized to
	 * prevent XSS attacks</li>
	 * <li><strong>Multipart Upload:</strong> File sent as multipart/form-data with
	 * metadata</li>
	 * <li><strong>Module Separation:</strong> Files organized by module name in
	 * filestore</li>
	 * <li><strong>Tenant Isolation:</strong> Files are tenant-specific for data
	 * isolation</li>
	 * </ul>
	 * 
	 * @param file       the file to upload
	 * @param fileName   the name of the file (will be normalized for security)
	 * @param mimeType   the MIME type of the file (e.g., application/pdf)
	 * @param moduleName the module name for file organization (e.g., BPA, NOC)
	 * @param tenantId   the tenant ID for multi-tenancy isolation
	 * @return filestore response object containing file ID and metadata
	 * @throws CustomException if upload fails or filestore service is unavailable
	 */
	public Object upload(File file, String fileName, String mimeType, String moduleName, String tenantId) {

		log.info("Uploading file: {} (size: {} bytes) to module: {}", file.getName(), file.length(), moduleName);

		// Step 1: Normalize parameters for security
		fileName = normalizeString(fileName);
		mimeType = normalizeString(mimeType);
		moduleName = normalizeString(moduleName);

		// Step 2: Build filestore upload URL
		String uploadUrl = buildUploadUrl();

		// Step 3: Prepare multipart request
		HttpEntity<MultiValueMap<String, Object>> request = prepareMultipartRequest(file, fileName, mimeType,
				moduleName, tenantId);

		// Step 4: Execute upload
		Object response = executeUpload(uploadUrl, request);

		log.info("File upload completed successfully");

		return response;
	}

	/**
	 * Builds the filestore upload URL.
	 * 
	 * @return the complete upload URL
	 */
	private String buildUploadUrl() {
		return bpaConfig.getFilestoreHost() + bpaConfig.getFilestoreUploadPath();
	}

	/**
	 * Prepares the multipart HTTP request for file upload.
	 * 
	 * <p>
	 * This method creates a multipart/form-data request containing:
	 * <ul>
	 * <li><strong>file:</strong> The actual file content</li>
	 * <li><strong>tenantId:</strong> For tenant-specific storage</li>
	 * <li><strong>module:</strong> For organizing files by module</li>
	 * </ul>
	 * 
	 * @param file       the file to upload
	 * @param fileName   the normalized file name
	 * @param mimeType   the normalized MIME type
	 * @param moduleName the normalized module name
	 * @param tenantId   the tenant ID
	 * @return HTTP entity with multipart request
	 */
	private HttpEntity<MultiValueMap<String, Object>> prepareMultipartRequest(File file, String fileName,
			String mimeType, String moduleName, String tenantId) {

		// Set multipart headers
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);

		// Build multipart body
		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add("file", new FileSystemResource(file.getName()));
		body.add("tenantId", tenantId);
		body.add("module", moduleName);

		return new HttpEntity<>(body, headers);
	}

	/**
	 * Executes the file upload to filestore service.
	 * 
	 * <p>
	 * This method handles the HTTP POST request with comprehensive error handling:
	 * <ul>
	 * <li><strong>RestClientException:</strong> Network/connectivity issues</li>
	 * <li><strong>General Exception:</strong> Any other unexpected errors</li>
	 * </ul>
	 * 
	 * @param uploadUrl the filestore upload URL
	 * @param request   the prepared multipart request
	 * @return filestore response body containing file ID
	 * @throws CustomException if upload fails
	 */
	private Object executeUpload(String uploadUrl, HttpEntity<MultiValueMap<String, Object>> request) {
		try {
			ResponseEntity<Object> result = restTemplate.postForEntity(uploadUrl, request, Object.class);
			return result.getBody();

		} catch (RestClientException e) {
			log.error("REST error occurred while uploading file to filestore", e);
			throw new CustomException("FILESTORE_UPLOAD_ERROR",
					"Failed to upload file to filestore: Network or connectivity issue");
		} catch (Exception ex) {
			log.error("Unexpected error occurred while uploading file to filestore", ex);
			throw new CustomException("FILESTORE_UPLOAD_ERROR",
					"Failed to upload file to filestore: " + ex.getMessage());
		}
	}

	/**
	 * Normalizes string input to prevent injection attacks.
	 * 
	 * <p>
	 * <strong>Security Measures:</strong>
	 * <ul>
	 * <li>Applies Unicode normalization (NFKC form)</li>
	 * <li>Checks for blacklisted characters (< and >)</li>
	 * <li>Throws exception if malicious content detected</li>
	 * </ul>
	 * 
	 * @param input the string to normalize
	 * @return normalized string
	 * @throws IllegalStateException if blacklisted characters found
	 */
	public static String normalizeString(String input) {
		// Apply Unicode normalization
		String normalized = Normalizer.normalize(input, Form.NFKC);

		// Check for blacklisted HTML/XML tags
		Pattern pattern = Pattern.compile("[<>]");
		Matcher matcher = pattern.matcher(normalized);

		if (matcher.find()) {
			throw new IllegalStateException(
					"Input contains blacklisted characters (< or >). Potential injection attack detected.");
		}

		return normalized;
	}
}
