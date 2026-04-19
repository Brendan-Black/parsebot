package black.parsebot.admin.source;

import black.parsebot.admin.api.ReferenceDataApi.ReferenceDataResponse;

public interface ReferenceDataSource {

  ReferenceDataResponse load();
}
