package black.parsebot.parser;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriceMatrixTest {

	private static final double EPS = 1e-9;

	@Test
	void transformMultiplier() {
		PriceMatrix pm = PriceMatrix.load("""
				customer_number,product_id,type,value
				CUST1,PROD1,multiplier,1.5
				""");
		assertEquals(15.0, pm.transform("CUST1", "PROD1", 10.0).getAsDouble(), EPS);
	}

	@Test
	void transformFixed() {
		PriceMatrix pm = PriceMatrix.load("""
				customer_number,product_id,type,value
				CUST1,PROD1,fixed,42.50
				""");
		assertEquals(42.50, pm.transform("CUST1", "PROD1", 999.0).getAsDouble(), EPS);
	}

	@Test
	void transformPercentageDiscount() {
		PriceMatrix pm = PriceMatrix.load("""
				customer_number,product_id,type,value
				CUST1,PROD1,percentage_discount,10
				""");
		assertEquals(90.0, pm.transform("CUST1", "PROD1", 100.0).getAsDouble(), EPS);
	}

	@Test
	void transformPercentageMarkup() {
		PriceMatrix pm = PriceMatrix.load("""
				customer_number,product_id,type,value
				CUST1,PROD1,percentage_markup,25
				""");
		assertEquals(125.0, pm.transform("CUST1", "PROD1", 100.0).getAsDouble(), EPS);
	}

	@Test
	void noMatchReturnsEmpty() {
		PriceMatrix pm = PriceMatrix.load("""
				customer_number,product_id,type,value
				CUST1,PROD1,fixed,10
				""");
		assertTrue(pm.transform("OTHER", "PROD1", 5.0).isEmpty());
		assertTrue(pm.transform("CUST1", "OTHER", 5.0).isEmpty());
	}

	@Test
	void emptyMatrixMatchesNothing() {
		PriceMatrix pm = PriceMatrix.load("customer_number,product_id,type,value\n");
		assertTrue(pm.transform("C", "P", 1.0).isEmpty());
	}

	@Test
	void wildcardCustomer() {
		PriceMatrix pm = PriceMatrix.load("""
				customer_number,product_id,type,value
				*,PROD1,fixed,7
				""");
		assertEquals(7.0, pm.transform("ANY", "PROD1", 0.0).getAsDouble(), EPS);
		assertEquals(7.0, pm.transform("OTHER", "PROD1", 0.0).getAsDouble(), EPS);
	}

	@Test
	void wildcardProduct() {
		PriceMatrix pm = PriceMatrix.load("""
				customer_number,product_id,type,value
				CUST1,*,multiplier,2
				""");
		assertEquals(20.0, pm.transform("CUST1", "ANY", 10.0).getAsDouble(), EPS);
	}

	@Test
	void wildcardBoth() {
		PriceMatrix pm = PriceMatrix.load("""
				customer_number,product_id,type,value
				*,*,multiplier,1.1
				""");
		assertEquals(11.0, pm.transform("X", "Y", 10.0).getAsDouble(), EPS);
	}

	@Test
	void specificityPrefersExactCustomerAndProduct() {
		PriceMatrix pm = PriceMatrix.load("""
				customer_number,product_id,type,value
				*,*,multiplier,1.1
				CUST1,*,fixed,50
				*,PROD1,fixed,60
				CUST1,PROD1,fixed,99
				""");
		assertEquals(99.0, pm.transform("CUST1", "PROD1", 10.0).getAsDouble(), EPS);
	}

	@Test
	void specificityExactCustomerBeatsExactProduct() {
		PriceMatrix pm = PriceMatrix.load("""
				customer_number,product_id,type,value
				*,PROD1,fixed,60
				CUST1,*,fixed,50
				""");
		assertEquals(50.0, pm.transform("CUST1", "PROD1", 10.0).getAsDouble(), EPS);
	}

	@Test
	void specificityExactProductBeatsAllWildcard() {
		PriceMatrix pm = PriceMatrix.load("""
				customer_number,product_id,type,value
				*,*,fixed,1
				*,PROD1,fixed,60
				""");
		assertEquals(60.0, pm.transform("OTHER", "PROD1", 10.0).getAsDouble(), EPS);
	}

	@Test
	void matchIsCaseInsensitive() {
		PriceMatrix pm = PriceMatrix.load("""
				customer_number,product_id,type,value
				cust1,prod1,fixed,10
				""");
		assertEquals(10.0, pm.transform("CUST1", "PROD1", 0.0).getAsDouble(), EPS);
		assertEquals(10.0, pm.transform("Cust1", "Prod1", 0.0).getAsDouble(), EPS);
	}

	@Test
	void invalidRuleTypeIsSkipped() {
		PriceMatrix pm = PriceMatrix.load("""
				customer_number,product_id,type,value
				CUST1,PROD1,bogus,10
				CUST2,PROD2,fixed,20
				""");
		assertTrue(pm.transform("CUST1", "PROD1", 0.0).isEmpty());
		assertEquals(20.0, pm.transform("CUST2", "PROD2", 0.0).getAsDouble(), EPS);
	}

	@Test
	void malformedRowIsSkipped() {
		PriceMatrix pm = PriceMatrix.load("""
				customer_number,product_id,type,value
				CUST1,PROD1
				CUST2,PROD2,fixed,20
				""");
		assertEquals(20.0, pm.transform("CUST2", "PROD2", 0.0).getAsDouble(), EPS);
	}

	@Test
	void commentAndBlankLinesSkipped() {
		PriceMatrix pm = PriceMatrix.load("""
				customer_number,product_id,type,value
				# a comment

				CUST1,PROD1,fixed,5
				""");
		assertEquals(5.0, pm.transform("CUST1", "PROD1", 0.0).getAsDouble(), EPS);
	}

	@Test
	void nonNumericValueIsSkipped() {
		PriceMatrix pm = PriceMatrix.load("""
				customer_number,product_id,type,value
				CUST1,PROD1,fixed,not-a-number
				CUST2,PROD2,fixed,7
				""");
		assertTrue(pm.transform("CUST1", "PROD1", 0.0).isEmpty());
		assertEquals(7.0, pm.transform("CUST2", "PROD2", 0.0).getAsDouble(), EPS);
	}

	@Test
	void applyToResponseRewritesUnitPrices() {
		PriceMatrix pm = PriceMatrix.load("""
				customer_number,product_id,type,value
				CUST1,*,multiplier,2
				""");
		String input = """
				{
				  "content": [
				    {
				      "type": "tool_use",
				      "name": "match",
				      "input": {
				        "customer_number": "CUST1",
				        "items": [
				          {"product_id": "P1", "unit_price": 10.0},
				          {"product_id": "P2", "unit_price": 7.5}
				        ]
				      }
				    }
				  ]
				}
				""";
		String out = pm.applyToResponse(input);
		JsonArray items = JsonParser.parseString(out).getAsJsonObject()
				.getAsJsonArray("content").get(0).getAsJsonObject()
				.getAsJsonObject("input").getAsJsonArray("items");
		assertEquals(20.0, items.get(0).getAsJsonObject().get("unit_price").getAsDouble(), EPS);
		assertEquals(15.0, items.get(1).getAsJsonObject().get("unit_price").getAsDouble(), EPS);
	}

	@Test
	void applyToResponseLeavesNonMatchingItemsUnchanged() {
		PriceMatrix pm = PriceMatrix.load("""
				customer_number,product_id,type,value
				CUST1,P1,fixed,99
				""");
		String input = """
				{
				  "content": [
				    {
				      "type": "tool_use",
				      "name": "match",
				      "input": {
				        "customer_number": "CUST1",
				        "items": [
				          {"product_id": "P1", "unit_price": 1.0},
				          {"product_id": "P2", "unit_price": 2.0}
				        ]
				      }
				    }
				  ]
				}
				""";
		JsonArray items = JsonParser.parseString(pm.applyToResponse(input)).getAsJsonObject()
				.getAsJsonArray("content").get(0).getAsJsonObject()
				.getAsJsonObject("input").getAsJsonArray("items");
		assertEquals(99.0, items.get(0).getAsJsonObject().get("unit_price").getAsDouble(), EPS);
		assertEquals(2.0, items.get(1).getAsJsonObject().get("unit_price").getAsDouble(), EPS);
	}

	@Test
	void applyToResponseRejectUnchanged() {
		PriceMatrix pm = PriceMatrix.load("""
				customer_number,product_id,type,value
				*,*,multiplier,2
				""");
		String input = """
				{"content":[{"type":"tool_use","name":"reject","input":{"reason":"x"}}]}
				""";
		// reject is untouched — same string returned
		assertEquals(input, pm.applyToResponse(input));
	}

	@Test
	void applyToResponseNoMatchReturnsUnchangedString() {
		PriceMatrix pm = PriceMatrix.load("""
				customer_number,product_id,type,value
				OTHER,*,fixed,99
				""");
		String input = """
				{"content":[{"type":"tool_use","name":"match","input":{"customer_number":"CUST1","items":[{"product_id":"P1","unit_price":1.0}]}}]}
				""";
		assertEquals(input, pm.applyToResponse(input));
	}

	@Test
	void applyToResponseMalformedJsonReturnsUnchanged() {
		PriceMatrix pm = PriceMatrix.load("customer_number,product_id,type,value\n*,*,fixed,1\n");
		String garbage = "not-json";
		assertEquals(garbage, pm.applyToResponse(garbage));
	}

	@Test
	void applyToResponseMissingItemUnitPriceTreatedAsZero() {
		PriceMatrix pm = PriceMatrix.load("""
				customer_number,product_id,type,value
				*,*,multiplier,5
				""");
		String input = """
				{"content":[{"type":"tool_use","name":"match","input":{"customer_number":"C","items":[{"product_id":"P"}]}}]}
				""";
		JsonObject item = JsonParser.parseString(pm.applyToResponse(input)).getAsJsonObject()
				.getAsJsonArray("content").get(0).getAsJsonObject()
				.getAsJsonObject("input").getAsJsonArray("items").get(0).getAsJsonObject();
		// 0 * 5 = 0 — rule matched, price is written
		assertTrue(item.has("unit_price"));
		assertEquals(0.0, item.get("unit_price").getAsDouble(), EPS);
	}

	@Test
	void transformReturnsOptionalEmptyWhenNoRulesAtAll() {
		PriceMatrix pm = PriceMatrix.load("");
		OptionalDouble result = pm.transform("C", "P", 1.0);
		assertFalse(result.isPresent());
	}

	@Test
	void crlfLineEndingsSupported() {
		PriceMatrix pm = PriceMatrix.load("customer_number,product_id,type,value\r\nCUST1,PROD1,fixed,5\r\n");
		assertEquals(5.0, pm.transform("CUST1", "PROD1", 0.0).getAsDouble(), EPS);
	}
}
