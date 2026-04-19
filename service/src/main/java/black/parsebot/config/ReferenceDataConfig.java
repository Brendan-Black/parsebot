package black.parsebot.config;

import java.util.Properties;

public final class ReferenceDataConfig {

  private final Properties props;

  ReferenceDataConfig(Properties props) {
    this.props = props;
  }

  public String getCustomerCsvPath() {
    return props.getProperty(ConfigKey.CSV_CUSTOMERS, "customers.csv");
  }

  public String getProductCsvPath() {
    return props.getProperty(ConfigKey.CSV_PRODUCTS, "products.csv");
  }

  public String getPriceMatrixCsvPath() {
    return props.getProperty(ConfigKey.CSV_PRICEMATRIX, "");
  }

  public String getCustomRulesDir() {
    return props.getProperty(ConfigKey.CUSTOM_RULES_DIR, "custom_rules");
  }

  public String getCustomProductListsDir() {
    return props.getProperty(ConfigKey.CUSTOM_PRODUCTLISTS_DIR, "custom_productlists");
  }
}
