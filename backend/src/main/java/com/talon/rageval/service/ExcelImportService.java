package com.talon.rageval.service;

import com.talon.rageval.dto.ImportDtos.ImportPreviewResponse;
import com.talon.rageval.dto.ImportDtos.ImportPreviewRow;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class ExcelImportService {

  private static final Duration TOKEN_TTL = Duration.ofMinutes(30);

  private record PreviewToken(Instant createdAt, List<ImportPreviewRow> rows) {}

  private final Map<String, PreviewToken> tokenStore = new ConcurrentHashMap<>();

  public ImportPreviewResponse preview(InputStream in) throws Exception {
    cleanupExpired();

    List<ImportPreviewRow> rows = parse(in);
    int valid = 0;
    int err = 0;
    for (ImportPreviewRow r : rows) {
      if (r.error() == null) {
        valid++;
      } else {
        err++;
      }
    }
    String token = UUID.randomUUID().toString();
    tokenStore.put(token, new PreviewToken(Instant.now(), rows));
    return new ImportPreviewResponse(token, valid, err, rows);
  }

  public List<ImportPreviewRow> getPreviewRowsOrThrow(String token) {
    cleanupExpired();
    PreviewToken pt = tokenStore.get(token);
    if (pt == null) {
      throw new IllegalArgumentException("invalid or expired token");
    }
    return pt.rows();
  }

  public void consumeToken(String token) {
    tokenStore.remove(token);
  }

  private void cleanupExpired() {
    Instant now = Instant.now();
    tokenStore.entrySet().removeIf(e -> Duration.between(e.getValue().createdAt(), now).compareTo(TOKEN_TTL) > 0);
  }

  private List<ImportPreviewRow> parse(InputStream in) throws Exception {
    try (Workbook wb = new XSSFWorkbook(in)) {
      Sheet sheet = wb.getNumberOfSheets() > 0 ? wb.getSheetAt(0) : null;
      if (sheet == null) {
        return List.of(new ImportPreviewRow(0, null, null, "no sheet found"));
      }

      Row header = sheet.getRow(sheet.getFirstRowNum());
      if (header == null) {
        return List.of(new ImportPreviewRow(0, null, null, "header row not found"));
      }

      Map<String, Integer> colIndex = buildHeaderIndex(header);
      Integer qCol = firstPresent(colIndex, "question", "问题");
      Integer aCol = firstPresent(colIndex, "referenceanswer", "reference_answer", "参考答案");
      if (qCol == null) {
        return List.of(new ImportPreviewRow(0, null, null, "missing header: question/问题"));
      }

      List<ImportPreviewRow> out = new ArrayList<>();
      int last = sheet.getLastRowNum();
      for (int i = header.getRowNum() + 1; i <= last; i++) {
        Row r = sheet.getRow(i);
        if (r == null) {
          continue;
        }
        String q = getCellString(r.getCell(qCol));
        String a = aCol == null ? null : getCellString(r.getCell(aCol));

        String error = null;
        if (q == null || q.isBlank()) {
          // skip fully empty rows
          if ((a == null || a.isBlank())) {
            continue;
          }
          error = "question is blank";
        }

        out.add(new ImportPreviewRow(i + 1, q, a, error));
      }
      if (out.isEmpty()) {
        out.add(new ImportPreviewRow(header.getRowNum() + 2, null, null, "no data rows"));
      }
      return out;
    }
  }

  private static Map<String, Integer> buildHeaderIndex(Row header) {
    Map<String, Integer> idx = new HashMap<>();
    for (Cell cell : header) {
      String raw = getCellString(cell);
      if (raw == null) {
        continue;
      }
      String key = normalizeHeader(raw);
      idx.putIfAbsent(key, cell.getColumnIndex());
    }
    return idx;
  }

  private static Integer firstPresent(Map<String, Integer> idx, String... candidates) {
    for (String c : candidates) {
      Integer v = idx.get(normalizeHeader(c));
      if (v != null) return v;
    }
    return null;
  }

  private static String normalizeHeader(String s) {
    return s == null ? "" : s.trim().toLowerCase(Locale.ROOT).replace(" ", "").replace("-", "").replace("_", "");
  }

  private static String getCellString(Cell cell) {
    if (cell == null) return null;
    if (cell.getCellType() == CellType.STRING) {
      return cell.getStringCellValue();
    }
    if (cell.getCellType() == CellType.NUMERIC) {
      double d = cell.getNumericCellValue();
      if (Math.floor(d) == d) return Long.toString((long) d);
      return Double.toString(d);
    }
    if (cell.getCellType() == CellType.BOOLEAN) {
      return Boolean.toString(cell.getBooleanCellValue());
    }
    if (cell.getCellType() == CellType.FORMULA) {
      try {
        return cell.getStringCellValue();
      } catch (Exception ignore) {
        try {
          return Double.toString(cell.getNumericCellValue());
        } catch (Exception ignore2) {
          return null;
        }
      }
    }
    return null;
  }
}

