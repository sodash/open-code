package com.goodloop.data;

import org.junit.Test;

import com.winterwell.gson.Gson;

/**
 * @tested {@link Money}
 * @author daniel
 *
 */
public class MoneyTest {
	
	@Test
	public void testInitBugSept2020_blankValue() {
		{
			String json = "{'currency':'GBP', 'value':'', 'value100p':0}".replace('\'', '"');
			Money money = new Gson().fromJson(json, Money.class);
			money.init();
			System.out.println(money);
		}
		{
			Money money = new Money(KCurrency.GBP, "");
			money.init();
		}
	}

	@Test
	public void testPlusMoney() {
		{
			Money two = Money.pound(2);
			com.goodloop.data.Money three = com.goodloop.data.Money.pound(3);
			Money five = two.plus(three);
			assert five.getValue().doubleValue() == 5 : five;
		}
		{
			Money two = Money.pound(2);
			Money three = Money.pound(3);
			Money five = two.plus(three);
			assert five.getValue().doubleValue() == 5 : five;
		}
		{
			Money two = Money.pound(20);
			Money three = Money.pound(0.5);
			Money five = two.plus(three);
			assert five.getValue().doubleValue() == 20.5 : five;
		}
	}

	@Test
	public void testMinusMoney() {
		Money three = Money.pound(3);
		com.goodloop.data.Money two = com.goodloop.data.Money.pound(2);
		Money one = three.minus(two);
		assert one.getValue().doubleValue() == 1 : one;
	}
	

}
