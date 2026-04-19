package black.parsebot.parser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

/**
* A price matrix that transforms unit prices on parsed order items.
*
* <p>Loaded from a CSV with columns: {@code customer_number,product_id,type,value}
*
* <p>Supported rule types:
* <ul>
*   <li>{@code multiplier} — new price = original × value</li>
*   <li>{@code fixed} — new price = value (ignores original)</li>
*   <li>{@code percentage_discount} — new price = original × (1 − value/100)</li>
*   <li>{@code percentage_markup} — new price = original × (1 + value/100)</li>
* </ul>
*
* <p>Use {@code *} as a wildcard for customer_number or product_id.
* When multiple rules match, the most specific one wins:
* <ol>
*   <li>exact customer + exact product (specificity 3)</li>
*   <li>exact customer + wildcard product (specificity 2)</li>
*   <li>wildcard customer + exact product (specificity 1)</li>
*   <li>wildcard customer + wildcard product (specificity 0)</li>
* </ol>
*/
public class PriceMatrix {

  private static final Logger log = LoggerFactory.getLogger(PriceMatrix.class);
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

  private final List<Rule> rules;

  private PriceMatrix(List<Rule> rules) {
    this.rules = List.copyOf(rules);
  }

  public static PriceMatrix load(String csv) {
    List<Rule> rules = new ArrayList<>();
    String[] lines = csv.split("\r?\n");
    for (int i = 1; i < lines.length; i++) {
      String line = lines[i].trim();
      if (line.isEmpty() || line.startsWith("#")) continue;
      String[] parts = splitCsvLine(line);
      if (parts.length < 4) {
        log.warn("Skipping malformed price matrix row {}: '{}'", i + 1, line);
        continue;
      }
      try {
        RuleType type = RuleType.fromString(parts[2]);
        double value = Double.parseDouble(parts[3].trim());
        rules.add(new Rule(parts[0].trim(), parts[1].trim(), type, value));
      } catch (IllegalArgumentException e) {
        log.warn("Skipping invalid price matrix row {}: {}", i + 1, e.getMessage());
      }
    }
    log.info("Loaded price matrix with {} rules", rules.size());
    return new PriceMatrix(rules);
  }

  public OptionalDouble transform(String customerNumber, String productId, double originalPrice) {
    Rule best = null;
    for (Rule rule : rules) {
      if (matches(rule.customerNumber, customerNumber) && matches(rule.productId, productId)) {
        if (best == null || rule.specificity() > best.specificity()) {
          best = rule;
        }
      }
    }
    if (best == null) return OptionalDouble.empty();
    return OptionalDouble.of(best.apply(originalPrice));
  }

  public String applyToResponse(String responseJson) {
    JsonObject response;
    try {
      response = JsonParser.parseString(responseJson).getAsJsonObject();
    } catch (Exception e) {
      log.warn("Failed to parse response JSON for price matrix application: {}", e.getMessage());
      return responseJson;
    }

    JsonArray content = response.getAsJsonArray("content");
    if (content == null) return responseJson;

    boolean modified = false;

    for (JsonElement block : content) {
      if (!block.isJsonObject()) continue;
      JsonObject contentBlock = block.getAsJsonObject();
      if (!"tool_use".equals(getString(contentBlock, "type"))) continue;
      if (!"match".equals(getString(contentBlock, "name"))) continue;

      JsonObject input = contentBlock.getAsJsonObject("input");
      if (input == null) continue;

      String customerNumber = getString(input, "customer_number");
      JsonArray items = input.getAsJsonArray("items");
      if (items == null) continue;

      for (JsonElement itemEl : items) {
        if (!itemEl.isJsonObject()) continue;
        JsonObject item = itemEl.getAsJsonObject();
        String productId = getString(item, "product_id");
        double price = item.has("unit_price") ? item.get("unit_price").getAsDouble() : 0.0;

        OptionalDouble newPrice = transform(customerNumber, productId, price);
        if (newPrice.isPresent()) {
          item.addProperty("unit_price", newPrice.getAsDouble());
          modified = true;
          log.debug("Price matrix: {} product {} — {} → {}",
              customerNumber, productId, price, newPrice.getAsDouble());
        }
      }
    }

    if (!modified) return responseJson;

    log.info("Applied price matrix transformations to response");
    return GSON.toJson(response);
  }

  private static String getString(JsonObject obj, String key) {
    JsonElement el = obj.get(key);
    return (el != null && el.isJsonPrimitive()) ? el.getAsString() : null;
  }

  private static boolean matches(String pattern, String value) {
    return "*".equals(pattern) || pattern.equalsIgnoreCase(value);
  }

  private static String[] splitCsvLine(String line) {
    List<String> fields = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean inQuotes = false;
    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);
      if (c == '"') {
        if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
          current.append('"');
          i++;
        } else {
          inQuotes = !inQuotes;
        }
      } else if (c == ',' && !inQuotes) {
        fields.add(current.toString());
        current.setLength(0);
      } else {
        current.append(c);
      }
    }
    fields.add(current.toString());
    return fields.toArray(String[]::new);
  }

  enum RuleType {
    MULTIPLIER,
    FIXED,
    PERCENTAGE_DISCOUNT,
    PERCENTAGE_MARKUP;

    static RuleType fromString(String s) {
      return valueOf(s.trim().toUpperCase());
    }
  }

  private record Rule(String customerNumber, String productId, RuleType type, double value) {

    int specificity() {
      int s = 0;
      if (!"*".equals(customerNumber)) s += 2;
      if (!"*".equals(productId)) s += 1;
      return s;
    }

    double apply(double originalPrice) {
      return switch (type) {
        case MULTIPLIER -> originalPrice * value;
        case FIXED -> value;
        case PERCENTAGE_DISCOUNT -> originalPrice * (1.0 - value / 100.0);
        case PERCENTAGE_MARKUP -> originalPrice * (1.0 + value / 100.0);
      };
    }
  }
}
