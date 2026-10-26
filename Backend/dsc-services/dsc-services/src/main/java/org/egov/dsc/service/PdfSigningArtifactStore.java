package org.egov.dsc.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Properties;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PdfSigningArtifactStore {
	private static final Logger log = LoggerFactory.getLogger(PdfSigningArtifactStore.class);
	private static final String SESSION_FILE = ".session";
	private static final Duration STALE_SESSION_AGE = Duration.ofHours(24);
	private static final EnumSet<PosixFilePermission> PRIVATE_DIRECTORY_PERMISSIONS = EnumSet.of(
			PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE);
	private static final EnumSet<PosixFilePermission> PRIVATE_FILE_PERMISSIONS = EnumSet.of(
			PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);

	private final Path root;

	public PdfSigningArtifactStore(@Value("${dsc.pdf.sig.shared-path}") String sharedPath) {
		if (sharedPath == null || sharedPath.trim().isEmpty()) {
			throw new IllegalArgumentException("dsc.pdf.sig.shared-path must be configured");
		}
		Path configuredPath = Paths.get(sharedPath);
		if (!configuredPath.isAbsolute()) {
			throw new IllegalArgumentException("PDF signing artifact path must be absolute");
		}
		this.root = configuredPath.normalize();
		if (this.root.getParent() == null) {
			throw new IllegalArgumentException("PDF signing artifact path must not be a filesystem root");
		}
	}

	@PostConstruct
	public void initialize() throws IOException {
		Files.createDirectories(root);
		if (!Files.isDirectory(root) || Files.isSymbolicLink(root)) {
			throw new IOException("PDF signing artifact path must be a real directory: " + root);
		}
		if (!Files.isWritable(root)) {
			throw new IOException("PDF signing artifact path is not writable: " + root);
		}
		Files.setPosixFilePermissions(root, PRIVATE_DIRECTORY_PERMISSIONS);
		purgeStaleSessions();
	}

	public String createSession(String userId, String tenantId, String channelId) throws IOException {
		String sessionId = UUID.randomUUID().toString();
		Path sessionDirectory = root.resolve(sessionId);
		Files.createDirectory(sessionDirectory);
		try {
			Files.setPosixFilePermissions(sessionDirectory, PRIVATE_DIRECTORY_PERMISSIONS);
			Properties metadata = new Properties();
			metadata.setProperty("userId", userId);
			metadata.setProperty("tenantId", tenantId);
			metadata.setProperty("channelId", channelId);
			Path metadataFile = sessionDirectory.resolve(SESSION_FILE);
			try (OutputStream output = Files.newOutputStream(metadataFile)) {
				metadata.store(output, null);
			}
			Files.setPosixFilePermissions(metadataFile, PRIVATE_FILE_PERMISSIONS);
			return sessionId;
		} catch (IOException | UnsupportedOperationException | SecurityException e) {
			deleteSession(sessionId);
			throw e;
		}
	}

	public Path getSessionDirectory(String sessionId) {
		return root.resolve(validateSessionId(sessionId));
	}

	public Path validateGeneratedSig(String sessionId, String generatedPath) throws IOException {
		Path sessionDirectory = getSessionDirectory(sessionId);
		Path sigFile = Paths.get(generatedPath).toAbsolutePath().normalize();
		if (Files.isSymbolicLink(sessionDirectory) || !sessionDirectory.equals(sigFile.getParent())
				|| !sigFile.getFileName().toString().endsWith(".sig")
				|| !Files.isRegularFile(sigFile) || Files.isSymbolicLink(sigFile)) {
			throw new IOException("emBridge returned a signature file outside its signing session directory");
		}
		Files.setPosixFilePermissions(sigFile, PRIVATE_FILE_PERMISSIONS);
		return sigFile;
	}

	public Path resolveSigFile(String sessionId, String userId, String tenantId, String channelId) throws IOException {
		Path sessionDirectory = getSessionDirectory(sessionId);
		if (Files.isSymbolicLink(sessionDirectory) || !Files.isDirectory(sessionDirectory)) {
			throw new IOException("PDF signing session is missing or invalid");
		}

		Path metadataFile = sessionDirectory.resolve(SESSION_FILE);
		if (Files.isSymbolicLink(metadataFile) || !Files.isRegularFile(metadataFile)) {
			throw new IOException("PDF signing session metadata is missing or invalid");
		}
		Properties metadata = new Properties();
		try (InputStream input = Files.newInputStream(metadataFile)) {
			metadata.load(input);
		}
		if (!userId.equals(metadata.getProperty("userId")) || !tenantId.equals(metadata.getProperty("tenantId"))
				|| !channelId.equals(metadata.getProperty("channelId"))) {
			throw new SecurityException("PDF signing session does not belong to this request");
		}

		Path sigFile = null;
		try (DirectoryStream<Path> files = Files.newDirectoryStream(sessionDirectory, "*.sig")) {
			for (Path file : files) {
				if (!Files.isRegularFile(file) || Files.isSymbolicLink(file) || sigFile != null) {
					throw new IOException("PDF signing session contains an invalid signature artifact");
				}
				sigFile = file;
			}
		}
		if (sigFile == null) {
			throw new IOException("PDF signing artifact was not found");
		}
		return sigFile;
	}

	public void deleteSession(String sessionId) throws IOException {
		Path sessionDirectory = getSessionDirectory(sessionId);
		if (Files.isSymbolicLink(sessionDirectory) || !Files.exists(sessionDirectory)) {
			return;
		}
		Files.walkFileTree(sessionDirectory, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path directory, IOException error) throws IOException {
				if (error != null) {
					throw error;
				}
				Files.delete(directory);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	private String validateSessionId(String sessionId) {
		if (sessionId == null) {
			throw new IllegalArgumentException("PDF signing session ID is required");
		}
		try {
			String canonicalId = UUID.fromString(sessionId).toString();
			if (!canonicalId.equals(sessionId)) {
				throw new IllegalArgumentException("Invalid PDF signing session ID");
			}
			return canonicalId;
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid PDF signing session ID", e);
		}
	}

	private void purgeStaleSessions() throws IOException {
		Instant cutoff = Instant.now().minus(STALE_SESSION_AGE);
		try (DirectoryStream<Path> sessions = Files.newDirectoryStream(root)) {
			for (Path session : sessions) {
				if (Files.isSymbolicLink(session) || !Files.isDirectory(session)
						|| !isSessionId(session.getFileName().toString())) {
					continue;
				}
				try {
					if (Files.getLastModifiedTime(session).toInstant().isBefore(cutoff)) {
						deleteSession(session.getFileName().toString());
					}
				} catch (IOException e) {
					log.warn("Failed to remove stale PDF signing session: {}", session, e);
				}
			}
		}
	}

	private boolean isSessionId(String value) {
		try {
			return UUID.fromString(value).toString().equals(value);
		} catch (IllegalArgumentException e) {
			return false;
		}
	}
}
