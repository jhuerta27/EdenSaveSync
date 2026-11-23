package com.jotasoft.edensavesync.data.storage.shizuku;

interface IPeanutsFileService {
    void destroy() = 16777114;
    boolean isDirectoryAccessible(String savesDirPath) = 1;
    String scanSavesJson(String savesDirPath) = 2;
    String scanSingleSaveJson(String savesDirPath, String titleId) = 3;
    boolean exportTitleSaveToZip(String savesDirPath, String titleId, String targetZipPath) = 4;
    boolean importTitleSaveFromZip(String savesDirPath, String titleId, String sourceZipPath) = 5;
}
