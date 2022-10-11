/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.helpers;

import java.awt.Image;
import java.util.ArrayList;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.contentstream.PDFStreamEngine;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

/**
 * The type PDFImage Extractor.
 */
public class PDFImageExtractor extends PDFStreamEngine {

  private List<Image> listImg = new ArrayList<>();

  public List getAllImages() {
    return listImg;
  }

  /**
   * @param operator The operation to perform.
   * @param operands The list of arguments.
   * @throws IOException If there is an error processing the operation.
   */
  @Override
  protected void processOperator(Operator operator, List<COSBase> operands) throws IOException {
    String operation = operator.getName();
    if ("Do".equals(operation))  //NOI18N
    {
      COSName objectName = (COSName) operands.get(0);
      PDXObject xobject = getResources().getXObject(objectName);
      if (xobject instanceof PDImageXObject) {
        PDImageXObject image = (PDImageXObject) xobject;
        BufferedImage bImage = image.getImage();
        listImg.add(bImage);
      }
    } else {
      super.processOperator(operator, operands);
    }
  }

}

