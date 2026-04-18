package black.parsebot.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CustomOverrideResolverTest {

  @Test
  void exactEmailMatch(@TempDir Path tmp) throws IOException {
    Path rulesDir = Files.createDirectory(tmp.resolve("rules"));
    Path listsDir = Files.createDirectory(tmp.resolve("lists"));
    Files.writeString(rulesDir.resolve("bob@acme.com"), "exact rules");

    CustomOverrideResolver r = new CustomOverrideResolver(rulesDir.toString(), listsDir.toString());
    assertEquals("exact rules", r.resolveRules("bob@acme.com"));
  }

  @Test
  @DisabledOnOs(value = OS.WINDOWS, disabledReason = "'*' is not a legal Windows filename character")
  void exactEmailMatchBeatsWildcard(@TempDir Path tmp) throws IOException {
    Path rulesDir = Files.createDirectory(tmp.resolve("rules"));
    Path listsDir = Files.createDirectory(tmp.resolve("lists"));
    Files.writeString(rulesDir.resolve("bob@acme.com"), "exact rules");
    Files.writeString(rulesDir.resolve("*@acme.com"), "wildcard rules");

    CustomOverrideResolver r = new CustomOverrideResolver(rulesDir.toString(), listsDir.toString());
    assertEquals("exact rules", r.resolveRules("bob@acme.com"));
  }

  @Test
  @DisabledOnOs(value = OS.WINDOWS, disabledReason = "'*' is not a legal Windows filename character")
  void domainWildcardMatchWhenNoExact(@TempDir Path tmp) throws IOException {
    Path rulesDir = Files.createDirectory(tmp.resolve("rules"));
    Path listsDir = Files.createDirectory(tmp.resolve("lists"));
    Files.writeString(rulesDir.resolve("*@acme.com"), "wildcard rules");

    CustomOverrideResolver r = new CustomOverrideResolver(rulesDir.toString(), listsDir.toString());
    assertEquals("wildcard rules", r.resolveRules("alice@acme.com"));
  }

  @Test
  void noMatchReturnsNull(@TempDir Path tmp) throws IOException {
    Path rulesDir = Files.createDirectory(tmp.resolve("rules"));
    Path listsDir = Files.createDirectory(tmp.resolve("lists"));
    Files.writeString(rulesDir.resolve("someone-else@other.com"), "x");

    CustomOverrideResolver r = new CustomOverrideResolver(rulesDir.toString(), listsDir.toString());
    assertNull(r.resolveRules("alice@acme.com"));
  }

  @Test
  void missingDirReturnsNull(@TempDir Path tmp) {
    CustomOverrideResolver r = new CustomOverrideResolver(
        tmp.resolve("missing-rules").toString(),
        tmp.resolve("missing-lists").toString());
    assertNull(r.resolveRules("bob@acme.com"));
    assertNull(r.resolveProductList("bob@acme.com"));
  }

  @Test
  void nullOrBlankSenderReturnsNull(@TempDir Path tmp) throws IOException {
    Path rulesDir = Files.createDirectory(tmp.resolve("rules"));
    Path listsDir = Files.createDirectory(tmp.resolve("lists"));
    CustomOverrideResolver r = new CustomOverrideResolver(rulesDir.toString(), listsDir.toString());
    assertNull(r.resolveRules(null));
    assertNull(r.resolveRules(""));
    assertNull(r.resolveRules("   "));
    assertNull(r.resolveProductList(null));
  }

  @Test
  void senderWithoutAtSymbolDoesNotCrash(@TempDir Path tmp) throws IOException {
    Path rulesDir = Files.createDirectory(tmp.resolve("rules"));
    Path listsDir = Files.createDirectory(tmp.resolve("lists"));
    CustomOverrideResolver r = new CustomOverrideResolver(rulesDir.toString(), listsDir.toString());
    assertNull(r.resolveRules("not-an-email"));
  }

  @Test
  void productListResolvesIndependentlyFromRules(@TempDir Path tmp) throws IOException {
    Path rulesDir = Files.createDirectory(tmp.resolve("rules"));
    Path listsDir = Files.createDirectory(tmp.resolve("lists"));
    Files.writeString(rulesDir.resolve("bob@acme.com"), "R");
    Files.writeString(listsDir.resolve("bob@acme.com"), "L");

    CustomOverrideResolver r = new CustomOverrideResolver(rulesDir.toString(), listsDir.toString());
    assertEquals("R", r.resolveRules("bob@acme.com"));
    assertEquals("L", r.resolveProductList("bob@acme.com"));
  }

  @Test
  void productListReturnsNullWhenOnlyRulesExist(@TempDir Path tmp) throws IOException {
    Path rulesDir = Files.createDirectory(tmp.resolve("rules"));
    Path listsDir = Files.createDirectory(tmp.resolve("lists"));
    Files.writeString(rulesDir.resolve("bob@acme.com"), "R");

    CustomOverrideResolver r = new CustomOverrideResolver(rulesDir.toString(), listsDir.toString());
    assertNull(r.resolveProductList("bob@acme.com"));
  }
}
