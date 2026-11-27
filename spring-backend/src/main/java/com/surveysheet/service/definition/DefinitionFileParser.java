package com.surveysheet.service.definition;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.TokenStreamContext;
import tools.jackson.core.TokenStreamLocation;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** 定義ファイルのバイト列を、JSON として読む（BOM の除去・UTF-8 の確認・キーの重複の確認を含む） */
final class DefinitionFileParser {

  private static final byte[] BOM = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };

  // Spring Boot 全体の JsonMapper は変えず、定義ファイル専用に作る
  private final JsonMapper mapper = JsonMapper.builder()
      .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
      .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
      .build();

  static byte[] stripBom(byte[] bytes) {
    if (bytes.length >= BOM.length && Arrays.equals(bytes, 0, BOM.length, BOM, 0, BOM.length)) {
      return Arrays.copyOfRange(bytes, BOM.length, bytes.length);
    }
    return bytes;
  }

  /** 読めなかったときは、errors に足して null を返す */
  JsonNode parse(String file, byte[] bytes, List<DefinitionError> errors) {
    byte[] content = stripBom(bytes);
    String text = decodeUtf8(file, content, errors);
    if (text == null) {
      return null;
    }
    JsonNode node;
    try {
      node = mapper.readTree(text);
    } catch (JacksonException e) {
      errors.add(toError(file, e));
      return null;
    }
    if (node == null || node.isMissingNode()) {
      errors.add(DefinitionError.at(file, "", "ファイルが空です。"));
      return null;
    }
    return node;
  }

  private static String decodeUtf8(String file, byte[] content, List<DefinitionError> errors) {
    CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT);
    ByteBuffer in = ByteBuffer.wrap(content);
    CharBuffer out = CharBuffer.allocate(content.length + 1);
    CoderResult result = decoder.decode(in, out, true);
    if (!result.isError()) {
      result = decoder.flush(out);
    }
    if (result.isError()) {
      // 正しく読めた所までの文字から、行と列を数える
      String before = new String(content, 0, in.position(), StandardCharsets.UTF_8);
      int lineStart = before.lastIndexOf('\n') + 1;
      int line = (int) before.chars().filter(c -> c == '\n').count() + 1;
      int column = before.length() - lineStart + 1;
      errors.add(DefinitionError.at(file, lineAndColumn(line, column),
          "UTF-8 として読めない文字があります。ファイルを UTF-8 で保存し直してください。"));
      return null;
    }
    return out.flip().toString();
  }

  private static DefinitionError toError(String file, JacksonException e) {
    TokenStreamLocation location = e.getLocation();
    String place = location == null ? "" : lineAndColumn(location.getLineNr(), location.getColumnNr());
    String path = e.processor() instanceof JsonParser parser ? pathOf(parser.streamReadContext()) : "";
    if (!path.isEmpty()) {
      place = place.isEmpty() ? path : place + "（" + path + "）";
    }
    String original = e.getOriginalMessage();
    if (original != null && original.startsWith("Duplicate Object property")) {
      return DefinitionError.at(file, place, "同じキーが重複して書かれています。");
    }
    return DefinitionError.at(file, place,
        "JSON として読み込めません。カンマの過不足や、括弧・引用符の閉じ忘れがないか確かめてください。");
  }

  private static String pathOf(TokenStreamContext context) {
    List<Object> parts = new ArrayList<>();
    for (TokenStreamContext c = context; c != null && !c.inRoot(); c = c.getParent()) {
      if (c.inArray() && c.hasCurrentIndex()) {
        parts.addFirst(c.getCurrentIndex());
      } else if (c.inObject() && c.hasCurrentName()) {
        parts.addFirst(c.currentName());
      }
    }
    return new KeyPath(parts).toString();
  }

  private static String lineAndColumn(int line, int column) {
    return line + "行目 " + column + "列目";
  }
}
