package com.jerzymaj.file_researcher_backend.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
public class ZipArchiveCreator {

    /**
     * Compresses files from given Paths into a single ZIP archive.
     *
     * @param filesToZip       List of staged file paths.
     * @param zipPath          Target path for the .zip file.
     * @param sourceDir        The base directory used to calculate relative paths inside the ZIP.
     * @param progressCallback Callback for real-time progress updates.
     */

    public void createZipArchiveFromPaths(List<Path> filesToZip, Path zipPath, Path sourceDir,
                                          ProgressCallback progressCallback) throws IOException {

        long totalFileSizeBytes = 0;

        for (Path file : filesToZip) {
            totalFileSizeBytes += Files.size(file);
        }

        final long[] totalBytesProcessed = {0};
        final int[] lastPercent = {0};
        final long totalSizeFinal = totalFileSizeBytes > 0 ? totalFileSizeBytes : 1;

        Set<String> addedEntries = new HashSet<>();

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {

            for (Path file : filesToZip) {
                String relativePath = sourceDir.relativize(file).toString().replace("\\", "/");

                if (!addedEntries.add(relativePath)) {
                    log.warn("Skipping duplicate entry in ZIP: {}", relativePath);
                    continue;
                }

                ZipEntry entry = new ZipEntry(relativePath);
                zos.putNextEntry(entry);

                try (InputStream inputStream = Files.newInputStream(file)) {
                    copyInputStreamWithProgress(inputStream, zos, totalSizeFinal, totalBytesProcessed, lastPercent,
                            progressCallback, relativePath);
                }

                zos.closeEntry();
            }
        }
    }

    /**
     * Copies data from an input stream to the ZIP output stream while calculating
     * and reporting real-time progress.
     * <p>
     * <b>Progress Logic:</b>
     * The method scales the raw copy progress to 90% of the total task,
     * leaving the remaining 10% for finalization and email dispatch.
     * Updates are throttled to every 150ms or every percentage increase
     * to prevent flooding the WebSocket broker.
     * </p>
     *
     * @param inputStream      The source stream of the file being compressed.
     * @param zos              The target ZIP output stream.
     * @param totalSize        Total size of all files in the batch (for percentage calculation).
     * @param bytesProcessed   A single-element array tracking cumulative bytes across multiple files.
     * @param lastPercent      A single-element array tracking the last reported percentage to avoid redundant updates.
     * @param progressCallback The functional interface used to push updates to the frontend.
     * @param currFinalName    The name of the file currently being processed (for status messages).
     * @throws IOException     If a read/write error occurs during the copy process.
     */

    private void copyInputStreamWithProgress(InputStream inputStream, ZipOutputStream zos, long totalSize,
                                             long[] bytesProcessed, int[] lastPercent, ProgressCallback progressCallback,
                                             String currFinalName) throws IOException {
        byte[] buffer = new byte[8192];
        int length;
        long lastMessageTime = 0;

        while ((length = inputStream.read(buffer)) != -1) {
            zos.write(buffer, 0, length);
            bytesProcessed[0] += length;

            int rawPercent = (int) ((bytesProcessed[0] * 100) / totalSize);
            int currPercent = (int) (rawPercent * 0.9);

            long currTime = System.currentTimeMillis();

            if (currTime - lastMessageTime > 150 || currPercent > lastPercent[0]) {
                if (currPercent > lastPercent[0]) {
                    progressCallback.onUpdate(currPercent, "Processing " + currFinalName);
                    lastPercent[0] = currPercent;
                    lastMessageTime = currTime;
                }
            }
        }

    }

    public Path prepareTempPath(Long fileSetId, int sendCounter) {
        String name = "fileset-" + fileSetId + "-" + sendCounter + ".zip";
        return Path.of(System.getProperty("java.io.tmpdir"), name);
    }
}
