package black.parsebot.admin.demo;

import black.parsebot.admin.api.ReferenceDataApi.ReferenceDataResponse;
import black.parsebot.admin.api.ReferenceDataApi.ReferenceFile;
import black.parsebot.admin.source.ReferenceDataSource;

public final class DemoReferenceDataSource implements ReferenceDataSource {

  @Override
  public ReferenceDataResponse load() {
    return new ReferenceDataResponse(
        new ReferenceFile(DemoData.CUSTOMERS_PATH, DemoData.CUSTOMERS_CSV, null),
        new ReferenceFile(DemoData.PRODUCTS_PATH, DemoData.PRODUCTS_CSV, null),
        new ReferenceFile(DemoData.PRICE_MATRIX_PATH, DemoData.PRICE_MATRIX_CSV, null));
  }
}
