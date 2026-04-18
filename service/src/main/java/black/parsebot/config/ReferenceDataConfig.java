package black.parsebot.config;

import java.util.Properties;

public final class ReferenceDataConfig {

	private final Properties props;

	ReferenceDataConfig(Properties props) {
		this.props = props;
	}

	public String getCustomerCsvPath() {
		return props.getProperty("csv.customers", "customers.csv");
	}

	public String getProductCsvPath() {
		return props.getProperty("csv.products", "products.csv");
	}

	public String getPriceMatrixCsvPath() {
		return props.getProperty("csv.pricematrix", "");
	}

	public String getCustomRulesDir() {
		return props.getProperty("custom.rules.dir", "custom_rules");
	}

	public String getCustomProductListsDir() {
		return props.getProperty("custom.productlists.dir", "custom_productlists");
	}
}
