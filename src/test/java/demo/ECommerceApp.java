package demo;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class ECommerceApp {

	String url = "http://automationpractice.com/index.php";
	WebDriver driver;
	WebDriverWait wait;
	Actions action;
	String orderNumber;

	/** 1. Launch the application **/
	@BeforeTest
	public void setup() {

		String driverPath = System.getProperty("user.dir") + "/src/test/resources/drivers/chromedriver.exe";

		// Set the chrome driver path
		System.setProperty("webdriver.chrome.driver", driverPath);

		// Set chrome options
		ChromeOptions options = new ChromeOptions();
		options.setPageLoadStrategy(PageLoadStrategy.NORMAL);
		options.setScriptTimeout(Duration.ofSeconds(10));
		options.setPageLoadTimeout(Duration.ofSeconds(20));
		//options.setHeadless(true);
		
		// Create instance of chrome driver
		driver = new ChromeDriver(options);
		
		action = new Actions(driver);

		wait = new WebDriverWait(driver, Duration.ofSeconds(3000));

		driver.get(url);
		driver.manage().window().maximize();
		System.out.println("\n\nApplication successfully launched");
	}

	@Test(priority = 1)
	public void SearchAddSummerDress() {
		System.out.println(
				"\n\n1. Search for the any summer dress by applying search criteria in catalog and add to cart ");

		// Search for "Chiffon" & Enter
		String strSearch = "Chiffon";
		SearchProduct(strSearch);

		String addToCartXpath = "//span[text()='Add to cart']";

		String product = AddProductsToCart(strSearch, addToCartXpath);

		System.out.println(product + "--> Successfully added to the cart.");
	}

	@Test(priority = 2)
	public void AddProductsToBeCompared() throws Exception {
		System.out.println("\n2. Compare Faded Short Sleeve T-shirts and Printed Dress using Add to compare option, add the higher price into cart and remove the another one\n");

		// Search for "Faded Short Sleeve T-shirts" & add to compare
		String strSearch = "Faded Short Sleeve T-shirts";
		SearchProduct(strSearch);
		String addToCompareXpath = "//a[contains(text(),'Compare')]";
		String product1 = AddProductsToCompare(strSearch, addToCompareXpath);

		// Search for "Printed Chiffon Dress" & add to compare
		strSearch = "Printed Chiffon Dress";
		SearchProduct(strSearch);
		String product2 = AddProductsToCompare(strSearch, addToCompareXpath);

		System.out.println("\n" + product1 + " & " + product2 + "--> Successfully added to be compared.");

		String comparelocator = "//button[contains(@class,'bt_compare')]/span[contains(text(),'Compare')]";
		wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.xpath(comparelocator)));
		
		// Click on Compare Button
		WebElement btnCompare = driver.findElement(By.xpath(comparelocator));
		((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", btnCompare);
		wait.until(ExpectedConditions.elementToBeClickable(By.xpath(comparelocator)));
		btnCompare.click();

	}

	@Test(dependsOnMethods = "AddProductsToBeCompared")
	public void ComparePricesAddToCart() {

		String productsXpath = "//td[contains(@class,'ajax_block_product')]";
		String pricesXpath = "/div[@class='prices-container']/span[contains(@class,'product-price') and not(contains(@class,'old-price'))]";

		wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.xpath(productsXpath + pricesXpath)));
		List<WebElement> prices = driver.findElements(By.xpath(productsXpath + pricesXpath));

		// Use Java Streams to find the highest price
		Optional<Double> price = prices.stream().map(x -> Double.parseDouble(x.getText().replace("$", "")))
				.sorted(Collections.reverseOrder()).findFirst();
		String highestPrice = "$" + price.get();

		String addToCartXpath = "./parent::div/following-sibling::div[@class='comparison_product_infos']//a[@title='Add to cart']";
		String removeXpath = "./parent::div/preceding-sibling::div[@class='remove']//a[@title='Remove']";
		String productNameXpath = "./parent::div/preceding-sibling::h5";
		for (WebElement product : prices) {
			String strProductPrice = product.getText();

			WebElement productNameElement = product.findElement(By.xpath(productNameXpath));
			String productName = productNameElement.getText();

			if (strProductPrice.equals(highestPrice)) // If this is the product with highest price - Add to cart
			{
				// Add to Cart
				WebElement btnAddToCart = product.findElement(By.xpath(addToCartXpath));
				btnAddToCart.click();

				// Close the confirmation panel that opens up
				String panelXpath = "//div[@id='layer_cart']//div[@class='clearfix']";
				wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(panelXpath)));
				WebElement btnClose = driver.findElement(By.xpath(panelXpath + "//span[@class='cross']"));
				btnClose.click();
				System.out.println("\nProduct " + productName + " with higher price " + strProductPrice	+ " ------> moved to the cart.");

			} else {

				// Delete from the Compare list
				wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//a[@title='Remove']")));
				WebElement btnRemove = product.findElement(By.xpath(removeXpath));
				((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", btnRemove);
				btnRemove.click();
				System.out.println("Product " + productName + " with lower price " + strProductPrice + " ----> removed from the compare list.");
			}
		}
	}

	@Test(dependsOnMethods = "ComparePricesAddToCart")
	public void Checkout() {
		System.out.println("\n3.Checkout -> register -> update address -> Select shipping mode -> use pay by check option -> Save order reference id -> go to order history -> find out status, total, tax amount -> Logout");
		WebElement shoppingCart = driver.findElement(By.xpath("//div[@class='shopping_cart']/a"));
		shoppingCart.click();

		wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.xpath("//span[text()='Proceed to checkout']")));
		WebElement checkOut = driver.findElement(By.xpath("//span[text()='Proceed to checkout']"));
		((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", checkOut);
		checkOut.click();
	}

	@Test(dependsOnMethods = "Checkout")
	public void UpdatePersonalDetails() {
		WebElement email = driver.findElement(By.id("email_create"));
		email.sendKeys(GenerateRandomEmail());
		driver.findElement(By.id("SubmitCreate")).click();

		wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.id("id_gender2")));
		wait.until(ExpectedConditions.elementToBeClickable(By.id("id_gender2")));
		
		driver.findElement(By.id("id_gender2")).click();
		driver.findElement(By.id("customer_firstname")).sendKeys("Naveena");
		driver.findElement(By.id("customer_lastname")).sendKeys("Pai");
		driver.findElement(By.id("passwd")).sendKeys("RandomPassword");

		Select dateDropdowm = new Select(driver.findElement(By.id("days")));
		dateDropdowm.selectByValue("1");

		dateDropdowm = new Select(driver.findElement(By.id("months")));
		dateDropdowm.selectByValue("1");

		dateDropdowm = new Select(driver.findElement(By.id("years")));
		dateDropdowm.selectByValue("1990");
		System.out.println("\nPersonal Details updated Successfully");
	}

	@Test(dependsOnMethods = "UpdatePersonalDetails")
	public void UpdateAddressDetails() {
		driver.findElement(By.id("firstname")).sendKeys("Naveena");
		driver.findElement(By.id("lastname")).sendKeys("Pai");
		driver.findElement(By.id("address1")).sendKeys("1232 Hughes Avenue");

		Select stateDropdowm = new Select(driver.findElement(By.id("id_state")));
		stateDropdowm.selectByVisibleText("New Jersey");
		driver.findElement(By.id("city")).sendKeys("Plainsboro");
		driver.findElement(By.id("postcode")).sendKeys("43134");
		driver.findElement(By.id("phone_mobile")).sendKeys("4313414565");

		driver.findElement(By.id("submitAccount")).click();
		System.out.println("\nAddress Details updated Successfully");
	}

	@Test(dependsOnMethods = "UpdateAddressDetails")
	public void UpdateShippingDetails() {
		wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.name("processAddress")));
		WebElement checkOut = driver.findElement(By.name("processAddress"));
		((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", checkOut);
		checkOut.click();

		// check the checkbox for "I agree" and click proceed to checkout
		driver.findElement(By.id("cgv")).click();
		driver.findElement(By.name("processCarrier")).click();

	}

	@Test(dependsOnMethods = "UpdateShippingDetails")
	public void UpdatePaymentDetails() {

		// select pay by cheque & confirm
		driver.findElement(By.className("cheque")).click();
		driver.findElement(By.xpath("//button/span[text()='I confirm my order']")).click();
		// Grab the order number
		WebElement orderNumberText = driver.findElement(By.xpath("//div[contains(@class,'order-confirmation')]"));

		orderNumber = GetOrderNumber(orderNumberText.getText());
		System.out.println("\nOrder is successfully placed ---> Order Confirmation Number :  " + orderNumber);

	}

	@Test(dependsOnMethods = "UpdatePaymentDetails")
	public void ViewOrderHistory() {

		// move to order histiry page
		driver.findElement(By.xpath("//a[@title='My orders']")).click();

		System.out.println("\nOrder Details for order with confirmation number: ---> " + orderNumber);
		
		// click on orderNumber for details
		String orderNumberColXpath = "//td[contains(@class,'footable-first-column')]//a[contains(text(),'" + orderNumber + "')]";
		
		String totalPriceXpath = "/parent::td/following-sibling::td[@class='history_price']";
		String totalPrice = driver.findElement(By.xpath(orderNumberColXpath + totalPriceXpath)).getText();
		System.out.println("\nTotal Price: " + totalPrice);

		// Click on order number to find tax amount & status
		driver.findElement(By.xpath(orderNumberColXpath)).click();
		
		String statusXpath="//table[contains(@class,'detail_step_by_step ')]//span";
		wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.xpath(statusXpath)));
		String status = driver.findElement(By.xpath(statusXpath)).getText();
		System.out.println("\nStatus: " + status);
		
		String tax = driver.findElement(By.xpath(
				"//div[@id='order-detail-content']//td/strong[text()='Items (tax excl.)']/parent::td/following-sibling::td/span[@class='price']"))
				.getText();
		System.out.println("\nTax: " + tax);

	}

	@Test(dependsOnMethods = "ViewOrderHistory")
	public void LogOut() {
		driver.findElement(By.className("logout")).click();
		System.out.println("\nLogged out successfully !!!!\n\n");
	}

	@AfterTest()
	public void teardown() {
		driver.quit();
	}

	/****** Private Methods *****/
	private void SearchProduct(String strSearch) {
		WebElement txtSearch = driver.findElement(By.id("search_query_top"));
		txtSearch.clear();
		txtSearch.sendKeys(Keys.chord(strSearch, Keys.ENTER));
		System.out.println("Searched for ----> " + strSearch);
	}

	private String AddProductsToCart(String strSearch, String xPath) {

		// Add to Cart
		String productXpath = "//ul[contains(@class,'product_list')]/li//h5";
		String searchXPath = "/a[contains(@title,'" + strSearch + "')]";

		WebElement dress = driver.findElement(By.xpath(productXpath + searchXPath));
		((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", dress);
		String product = dress.getText();
		
		// Mouse over to open the ajax panel with add to cart button
		action.moveToElement(dress).build().perform();

		String addToCartPanelXPath = "/parent::h5/following-sibling::div[@class='button-container']";
		wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.xpath(productXpath + searchXPath + addToCartPanelXPath)));

		WebElement addToCart = driver.findElement(By.xpath(productXpath + searchXPath + addToCartPanelXPath + xPath));
		((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", addToCart);
		addToCart.click();

		// Close the confirmation panel that opens up
		String panelXpath = "//div[@id='layer_cart']//div[@class='clearfix']";
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(panelXpath)));
		WebElement btnClose = driver.findElement(By.xpath(panelXpath + "//span[@class='cross']"));
		((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", btnClose);
		btnClose.click();

		return product;
	}

	private String AddProductsToCompare(String strSearch, String xPath) {

		// Add to Compare
		String productXpath = "//ul[contains(@class,'product_list')]/li//h5";
		String searchXPath = "/a[contains(@title,'" + strSearch + "')]";

		WebElement dress = driver.findElement(By.xpath(productXpath + searchXPath));
		((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", dress);
		String product = dress.getText();
		
		// Mouse over to open the ajax panel with add to cart button
		action.moveToElement(dress).build().perform();

		String addToComparePanelXPath = "/parent::h5/parent::div/following-sibling::div[contains(@class,'functional-buttons')]";
		wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.xpath(productXpath + searchXPath + addToComparePanelXPath)));

		WebElement addToCompare = driver
				.findElement(By.xpath(productXpath + searchXPath + addToComparePanelXPath + xPath));
		((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", addToCompare);
		addToCompare.click();

		return product;
	}

	private static String GenerateRandomEmail() {
		String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
		StringBuilder builder = new StringBuilder();
		Random rnd = new Random();
		while (builder.length() < 10) { // length of the random string.
			int index = (int) (rnd.nextFloat() * chars.length());
			builder.append(chars.charAt(index));
		}
		String email = builder.toString()+"@gmail.com";
		return email;

	}

	// Method to order number from text
	private String GetOrderNumber(String text) {
		String[] messages = text.split("\r?\n|\r"); // split based on new line
		String orderConfirmationMsg = messages[4];
		String orderNumber = orderConfirmationMsg.substring(orderConfirmationMsg.lastIndexOf(' '),
				orderConfirmationMsg.lastIndexOf('.'));
		return orderNumber.trim();
	}

}