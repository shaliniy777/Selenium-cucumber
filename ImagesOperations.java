/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.helpers;

import com.experian.automation.logger.Logger;

import java.io.DataInputStream;
import java.io.FileInputStream;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.File;
import java.io.IOException;

/**
 * The type Images operations.
 */
public class ImagesOperations {

  private final Logger logger = Logger.getLogger(this.getClass());

  /**
   * Compare images boolean.
   *
   * @param firstImageSrc  the first image src
   * @param secondImageSrc the second image src
   * @return the boolean
   * @throws IOException the io exception
   */
  public boolean compareImages(File firstImageSrc, File secondImageSrc) throws IOException {

    BufferedImage imageOne = ImageIO.read(firstImageSrc);
    DataBuffer imageOneBuffer = imageOne.getData().getDataBuffer();
    int sizeA = imageOneBuffer.getSize();
    BufferedImage imageTwo = ImageIO.read(secondImageSrc);
    DataBuffer imageTwoBuffer = imageTwo.getData().getDataBuffer();
    int sizeB = imageTwoBuffer.getSize();
    if (sizeA == sizeB) {
      for (int i = 0; i < sizeA; i++) {
        if (imageOneBuffer.getElem(i) != imageTwoBuffer.getElem(i)) {
          return false;
        }
      }
      return true;
    } else {
      logger.info("Images have different sizes...");
      return false;
    }

  }

  /**
   * Compares files difference based on a given percentage (Default is 1)
   *
   * @param firstImage           - The path to the first image which we want to compare
   * @param secondImage          - The path to the second image which we want to compare
   * @param differencePercentage - How similar do we want the images to be
   * @return A boolean value indicating whether the images are similar or not
   * @throws IOException the io exception
   */
  public boolean compareSimilarImages(String firstImage, String secondImage, double differencePercentage)
      throws IOException {
    BufferedImage imgA = null;
    BufferedImage imgB = null;

    try {
      File fileA = new File(firstImage);
      File fileB = new File(secondImage);

      imgA = ImageIO.read(fileA);
      imgB = ImageIO.read(fileB);
    } catch (IOException e) {
      System.out.println(e);
    }

    int widthImgA = imgA.getWidth();
    int widthImgB = imgB.getWidth();
    int heightImgA = imgA.getHeight();
    int heightImgB = imgB.getHeight();

    if ((widthImgA != widthImgB) || (heightImgA != heightImgB)) {
      return false;
    } else {
      long difference = 0;
      for (int y = 0; y < heightImgA; y++) {
        for (int x = 0; x < widthImgB; x++) {
          int rgbA = imgA.getRGB(x, y);
          int rgbB = imgB.getRGB(x, y);
          int redA = (rgbA >> 16) & 0xff;
          int greenA = (rgbA >> 8) & 0xff;
          int blueA = (rgbA) & 0xff;
          int redB = (rgbB >> 16) & 0xff;
          int greenB = (rgbB >> 8) & 0xff;
          int blueB = (rgbB) & 0xff;
          difference += Math.abs(redA - redB);
          difference += Math.abs(greenA - greenB);
          difference += Math.abs(blueA - blueB);
        }
      }

      double total_pixels = widthImgA * heightImgA * 3;

      double avg_different_pixels = difference /
          total_pixels;

      double percentage = (avg_different_pixels /
          255) * 100;

      return (percentage <= differencePercentage);
    }
  }

  /**
   * Converts a file to an image
   *
   * @param sourceFile - The file which we want to convert. Supports all file formats
   * @param imageFile  - The name of the newly created image (PNG)
   * @throws IOException the io exception
   */
  public void convertFileToImage(String sourceFile, String imageFile) throws IOException {
    BufferedImage image;
    try (DataInputStream dis = new DataInputStream(new FileInputStream(sourceFile))) {
      int size = ((int) Math.sqrt(dis.available())) + 2;
      image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
      for (int y = 0; y < size; y++) {
        for (int x = 0; x < size; x++) {
          int red = dis.read();
          int green = dis.read();
          int blue = dis.read();
          int rgb = (0xFF << 24) | ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF);
          image.setRGB(x, y, rgb);
        }
      }
    }
    ImageIO.write(image, "png", new File(imageFile));
  }
}
