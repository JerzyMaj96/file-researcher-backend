package com.jerzymaj.file_researcher_backend.tranlator;

import com.jerzymaj.file_researcher_backend.DTOs.*;
import com.jerzymaj.file_researcher_backend.models.*;

public class Translator {

    public static UserDTO convertUserToDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .creationDate(user.getCreationDate())
                .build();
    }

    public static FileSetDTO convertFileSetToDTO(FileSet fileSet) {
        return FileSetDTO.builder()
                .id(fileSet.getId())
                .name(fileSet.getName())
                .description(fileSet.getDescription())
                .recipientEmail(fileSet.getRecipientEmail())
                .status(fileSet.getStatus())
                .creationDate(fileSet.getCreationDate())
                .userId(fileSet.getUser().getId())
                .files(fileSet.getFiles().stream()
                        .map(Translator::convertFileEntryToDTO)
                        .toList())
                .build();
    }

    public static FileEntryDTO convertFileEntryToDTO(FileEntry fileEntry) {
        return FileEntryDTO.builder()
                .id(fileEntry.getId())
                .name(fileEntry.getName())
                .path(fileEntry.getPath())
                .size(fileEntry.getSize())
                .extension(fileEntry.getExtension())
                .build();
    }

    public static ZipArchiveDTO convertZipArchiveToDTO(ZipArchive zipArchive) {

        return ZipArchiveDTO.builder()
                .id(zipArchive.getId())
                .archiveName(zipArchive.getArchiveName())
                .archivePath(zipArchive.getArchivePath())
                .size(zipArchive.getSize())
                .creationDate(zipArchive.getCreationDate())
                .status(zipArchive.getStatus())
                .recipientEmail(zipArchive.getRecipientEmail())
                .fileSetId(zipArchive.getFileSet().getId())
                .userId(zipArchive.getUser().getId())
                .sentHistoryList(zipArchive.getSentHistoryList().stream()
                        .map(Translator::convertSentHistoryToDTO)
                        .toList())
                .build();
    }

    public static SentHistoryDTO convertSentHistoryToDTO(SentHistory sentHistory) {
        return SentHistoryDTO.builder()
                .id(sentHistory.getId())
                .zipArchiveId(sentHistory.getZipArchive().getId())
                .sentAttemptDate(sentHistory.getSendAttemptDate())
                .status(sentHistory.getStatus())
                .errorMessage(sentHistory.getErrorMessage())
                .sentToEmail(sentHistory.getSentToEmail())
                .build();
    }
}
