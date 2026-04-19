package black.parsebot.admin.api;

import java.util.List;

import com.sun.net.httpserver.HttpExchange;

import black.parsebot.admin.config.ConfigSchema;
import black.parsebot.admin.server.JsonHandler;
import black.parsebot.admin.service.ServiceManager;

public final class ConfigApi {

  public record SchemaResponse(
      List<ConfigSchema.Section> sections,
      List<ConfigSchema.Carrier> smsCarriers
  ) {}

  public static final class SchemaHandler extends JsonHandler {
    @Override
    protected Object handleJson(HttpExchange exchange) {
      requireMethod(exchange, "GET");
      return new SchemaResponse(ConfigSchema.SECTIONS, ConfigSchema.SMS_CARRIERS);
    }
  }

  public static final class ConfigHandler extends JsonHandler {
    private final ServiceManager serviceManager;

    public ConfigHandler(ServiceManager serviceManager) {
      this.serviceManager = serviceManager;
    }

    @Override
    protected Object handleJson(HttpExchange exchange) {
      requireMethod(exchange, "GET");
      return serviceManager.currentConfig();
    }
  }

  private ConfigApi() {}
}
