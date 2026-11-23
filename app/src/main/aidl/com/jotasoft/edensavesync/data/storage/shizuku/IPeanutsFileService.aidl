package com.jotasoft.edensavesync.data.storage.shizuku;

interface IPeanutsFileService {
    void destroy() = 16777114;
    String scanSavesJson(String savesDirPath) = 1;
    String scanSingleSaveJson(String savesDirPath, String titleId) = 2;
    boolean exportTitleSaveToZip(String savesDirPath, String titleId, String targetZipPath) = 3;
    boolean importTitleSaveFromZip(String savesDirPath, String titleId, String sourceZipPath) = 4;
}
