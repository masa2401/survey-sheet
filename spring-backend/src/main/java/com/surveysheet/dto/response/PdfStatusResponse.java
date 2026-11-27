package com.surveysheet.dto.response;

public record PdfStatusResponse(String status, String downloadUrl) {
  public static PdfStatusResponse generating() {
    return new PdfStatusResponse("generating", null);
  }

  public static PdfStatusResponse ready(String downloadUrl) {
    return new PdfStatusResponse("ready", downloadUrl);
  }
}
