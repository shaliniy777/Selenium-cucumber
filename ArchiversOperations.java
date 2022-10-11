/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.helpers;

import com.experian.automation.logger.Logger;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.testng.reporters.Files;

/**
 * The type Archivers operations.
 */
public class ArchiversOperations {

  private final static Logger logger = Logger.getLogger(ArchiversOperations.class);

  /**
   * Extract a given TAR archive to some destination folder.
   *
   * @param src The full path to the archive (e.g C:/Temp/file.tar)
   * @param dst The path to the destination folder (e.g C:/Temp/Archives )
   * @return List of extracted files
   * @throws IOException      the io exception
   * @throws ArchiveException the archive exception
   */
  public static List<String> extractTar(String src, String dst) throws IOException, ArchiveException {

    List<String> extractedList = new ArrayList<String>();
    logger.info(String.format("Extracting file: %s to: %s", src, dst));

    try (TarArchiveInputStream inStream = new TarArchiveInputStream(new FileInputStream(new File(src)), StandardCharsets.UTF_8.name())) {

      TarArchiveEntry tarFile;
      while ((tarFile = (TarArchiveEntry) inStream.getNextEntry()) != null) {

        File dstFile = new File(dst, tarFile.getName());
        String canonicalDestinationPath = dstFile.getCanonicalPath();

        File dstCanonical = new File(dst);
        String canonicalDestinationFolder = dstCanonical.getCanonicalPath();

        if (canonicalDestinationPath.startsWith(canonicalDestinationFolder)) {

          if (tarFile.isDirectory()) {
            dstFile.mkdirs();
          } else {
            Files.copyFile(inStream, dstFile);
          }
          extractedList.add(dstFile.getAbsolutePath());

        } else {
          throw new IOException("Entry is outside of the target directory");
        }
      }
    }

    logger.info(String.format("Extraction ended with total of %s extracted", extractedList.size()));
    return extractedList;
  }


  /**
   * The method unzip a given archive (e.g .zip,.rar,.wra) in some destination folder.
   *
   * @param src The full path to the archive (e.g C:/Temp/file.zip)
   * @param dst The path to the destination folder (e.g C:/Temp/Archives )
   * @throws IOException the io exception
   */
  public void unzip(String src, String dst) throws IOException {
    byte[] buffer = new byte[1024];

    try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(src), StandardCharsets.UTF_8)) {
      File folder = new File(dst);
      if (!folder.exists()) {
        folder.mkdir();
      }

      ZipEntry zipEntry = zipInputStream.getNextEntry();
      logger.info(String.format("Starting unzipping file: %s", src));
      while (zipEntry != null) {
        String fileName = zipEntry.getName();
        File newFile = new File(dst + File.separator + fileName);
        String canonicalDestinationPath = newFile.getCanonicalPath();

        File dstCanonical = new File(dst);
        String canonicalDestinationFolder = dstCanonical.getCanonicalPath();

        if(canonicalDestinationPath.startsWith(canonicalDestinationFolder)){
          new File(newFile.getParent()).mkdirs();
          if (!zipEntry.isDirectory()) {
            try (FileOutputStream fos = new FileOutputStream(newFile)) {
              int len;
              while ((len = zipInputStream.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
              }
            }
          }
          zipEntry = zipInputStream.getNextEntry();
        } else {
          throw new IOException("Entry is outside of the target directory");
        }
      }

      zipInputStream.closeEntry();
      zipInputStream.close();

      logger.info(String.format("Finished unzipping file: %s", src));

    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  /**
   * The method zip a given directory to zip file.
   *
   * @param src The full path to the archive (e.g C:/Temp)
   * @param dst The path to the destination folder (e.g C:/Temp.zip )
   * @throws IOException      the io exception
   * @throws ArchiveException the archive exception
   */
  public static void zip(String src, String dst) throws IOException, ArchiveException {
    try (OutputStream archiveStream = new FileOutputStream(
        dst); ArchiveOutputStream archive = new ArchiveStreamFactory().createArchiveOutputStream(
        ArchiveStreamFactory.ZIP, archiveStream, StandardCharsets.UTF_8.name())) {

      Collection<File> fileList = FileUtils.listFiles(new File(src), null, true);

      for (File file : fileList) {
        String entryName = file.getCanonicalPath().substring(new File(src).getAbsolutePath().length() + 1);
        ZipArchiveEntry entry = new ZipArchiveEntry(entryName);
        archive.putArchiveEntry(entry);

        try (BufferedInputStream input = new BufferedInputStream(new FileInputStream(file))) {
          IOUtils.copy(input, archive);
        }
        archive.closeArchiveEntry();
      }
    }
  }
}


