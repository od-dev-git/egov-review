package org.egov.dsc.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PdfSigningArtifactStoreTest {
	@TempDir
	Path sharedRoot;

	private PdfSigningArtifactStore store;

	@BeforeEach
	void initializeStore() throws IOException {
		store = new PdfSigningArtifactStore(sharedRoot.toString());
		store.initialize();
	}

	@Test
	void resolvesOnlySessionOwnedArtifactAndDeletesSession() throws IOException {
		String sessionId = store.createSession("7", "od", "channel");
		Path sigFile = store.getSessionDirectory(sessionId).resolve("input.sig");
		Files.createFile(sigFile);

		assertEquals(sigFile, store.resolveSigFile(sessionId, "7", "od", "channel"));
		assertEquals(sigFile, store.validateGeneratedSig(sessionId, sigFile.toString()));
		PdfSigningArtifactStore otherPod = new PdfSigningArtifactStore(sharedRoot.toString());
		otherPod.initialize();
		assertEquals(sigFile, otherPod.resolveSigFile(sessionId, "7", "od", "channel"));
		assertThrows(SecurityException.class, () -> store.resolveSigFile(sessionId, "8", "od", "channel"));

		store.deleteSession(sessionId);

		assertFalse(Files.exists(sigFile));
		assertFalse(Files.exists(store.getSessionDirectory(sessionId)));
	}

	@Test
	void rejectsMalformedIdsAndArtifactsOutsideSessionDirectory() throws IOException {
		String sessionId = store.createSession("7", "od", "channel");
		Path outsideSig = Files.createFile(sharedRoot.resolve("outside.sig"));

		assertThrows(IllegalArgumentException.class,
				() -> store.resolveSigFile("../../outside.sig", "7", "od", "channel"));
		assertThrows(IOException.class, () -> store.validateGeneratedSig(sessionId, outsideSig.toString()));
	}
}
