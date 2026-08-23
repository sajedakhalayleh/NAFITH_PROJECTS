package nafith;

import io.github.bonigarcia.wdm.WebDriverManager;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.List;


public class NafithAutomation {

    // =========================================================
    // CONFIG
    // =========================================================

    public static final String BASE_URL =
            "https://khb.nafith.net";

    public static final String LOGIN_URL =
            BASE_URL + "/auth/login?redirectTo=/auth/login";

    public static final String USERNAME =
            "nafithadmin";

    public static final String PASSWORD =
            "Lz7F27@q";

    public static final int DEFAULT_TIMEOUT_SECONDS = 15;

    public static final int WAIT_AFTER_CLICK = 2000;


    // =========================================================
    // DRIVER
    // =========================================================

    protected WebDriver driver;

    protected WebDriverWait wait;


    // =========================================================
    // SETUP
    // =========================================================

    @BeforeSuite(alwaysRun = true)
    public void setUp() {

        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();

        options.addArguments("--start-maximized");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");

        if (System.getenv("GITHUB_ACTIONS") != null) {
            options.addArguments("--headless=new");
        }

        driver = new ChromeDriver(options);

        wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS)
        );

        login();

        if (System.getenv("GITHUB_ACTIONS") == null) {
            maximizeWindow();
        }
    }


    // =========================================================
    // TEARDOWN
    // =========================================================

    @AfterSuite(alwaysRun = true)
    public void tearDown() {

        if (driver != null) {
            driver.quit();
        }
    }


    // =========================================================
    // LOGIN
    // =========================================================

    protected void login() {

        driver.get(LOGIN_URL);

        WebElement userInput =
                wait.until(
                        ExpectedConditions.presenceOfElementLocated(
                                By.cssSelector(
                                        "input[name='userName']"
                                )
                        )
                );

        WebElement passInput =
                wait.until(
                        ExpectedConditions.presenceOfElementLocated(
                                By.cssSelector(
                                        "input[name='password']"
                                )
                        )
                );

        userInput.clear();
        userInput.sendKeys(USERNAME);

        passInput.clear();
        passInput.sendKeys(PASSWORD);

        WebElement loginButton =
                wait.until(
                        ExpectedConditions.elementToBeClickable(
                                By.cssSelector(
                                        "button[type='submit']"
                                )
                        )
                );

        loginButton.click();

        sleep(WAIT_AFTER_CLICK);

        wait.until(
                ExpectedConditions.urlContains("/app/")
        );

        Assert.assertTrue(
                driver.getCurrentUrl().contains("/app/"),
                "فشل تسجيل الدخول"
        );
    }


    // =========================================================
    // MAXIMIZE
    // =========================================================

    protected void maximizeWindow() {

        driver.manage()
                .window()
                .maximize();

        sleep(WAIT_AFTER_CLICK);
    }


    // =========================================================
    // DASHBOARD + EXPORT
    // =========================================================

    protected void openDashboardAndExport() {

        driver.get(BASE_URL + "/app/dashboard");

        sleep(WAIT_AFTER_CLICK);

        wait.until(
                ExpectedConditions.urlContains("/app/dashboard")
        );

        WebElement exportButton =
                wait.until(
                        ExpectedConditions.elementToBeClickable(
                                By.xpath(
                                        "//button[contains(normalize-space(.),'تصدير إلى Excel')]"
                                )
                        )
                );

        clickElement(exportButton);

        sleep(WAIT_AFTER_CLICK);
    }


    // =========================================================
    // NAVIGATION
    // =========================================================

    protected void navigateViaMenu(
            String parentMenuText,
            String childMenuText,
            String expectedUrlPart) {

        // =====================================================
        // 1. إذا في Parent Menu
        // =====================================================

        if (parentMenuText != null
                && !parentMenuText.isEmpty()) {

            WebElement parent =
                    findParentMenu(parentMenuText);

            if (parent == null) {

                throw new RuntimeException(
                        "لم يتم العثور على Parent Menu: "
                                + parentMenuText
                );
            }

            // -------------------------------------------------
            // نشوف إذا الـ Parent مفتوح
            // -------------------------------------------------

            String state =
                    parent.getAttribute("data-state");

            // -------------------------------------------------
            // إذا مسكر افتحه
            // -------------------------------------------------

            if (!"open".equals(state)) {

                clickElement(parent);

                sleep(WAIT_AFTER_CLICK);
            }

            // -------------------------------------------------
            // إذا الرابط الداخلي مش ظاهر
            // -------------------------------------------------

            List<WebElement> childLinks =
                    driver.findElements(
                            By.cssSelector(
                                    "a[href='" +
                                            expectedUrlPart +
                                            "']"
                            )
                    );

            boolean childVisible = false;

            for (WebElement link : childLinks) {

                try {

                    if (link.isDisplayed()) {

                        childVisible = true;

                        break;
                    }

                } catch (Exception ignored) {
                }
            }

            if (!childVisible) {

                clickElement(parent);

                sleep(WAIT_AFTER_CLICK);
            }
        }

        // =====================================================
        // 2. دور على الرابط بالـ URL
        // =====================================================

        WebElement child =
                wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                By.cssSelector(
                                        "a[href='" +
                                                expectedUrlPart +
                                                "']"
                                )
                        )
                );

        // =====================================================
        // 3. اضغط الرابط
        // =====================================================

        clickElement(child);

        sleep(WAIT_AFTER_CLICK);

        // =====================================================
        // 4. تأكد من فتح الصفحة
        // =====================================================

        wait.until(
                ExpectedConditions.urlContains(
                        expectedUrlPart
                )
        );

        Assert.assertTrue(
                driver.getCurrentUrl()
                        .contains(expectedUrlPart),

                "لم يتم الانتقال للشاشة المتوقعة: "
                        + childMenuText
        );

        sleep(WAIT_AFTER_CLICK);
    }


    // =========================================================
    // FIND PARENT MENU
    // =========================================================

    protected WebElement findParentMenu(String parentMenuText) {

        // -----------------------------------------------------
        // Locator خاص بالخطط حسب الـ HTML الفعلي
        // -----------------------------------------------------

        if ("الخطط".equals(parentMenuText)) {

            try {

                By plansLocator =
                        By.xpath(
                                "//button[@data-sidebar='menu-button' " +
                                        "and @data-state='open' " +
                                        "and .//span[normalize-space(.)='الخطط']]"
                        );

                WebElement plansMenu =
                        wait.until(
                                ExpectedConditions.visibilityOfElementLocated(
                                        plansLocator
                                )
                        );

                System.out.println(
                        "FOUND PLANS MENU: ["
                                + plansMenu.getText()
                                + "]"
                );

                return plansMenu;

            } catch (Exception e) {

                System.out.println(
                        "FAILED TO FIND PLANS MENU: "
                                + e.getMessage()
                );
            }
        }

        // -----------------------------------------------------
        // انتظر لحد ما يكون الـ Parent موجود بالـ DOM
        // -----------------------------------------------------

        try {

            wait.until(
                    ExpectedConditions.presenceOfElementLocated(
                            By.xpath(
                                    "//button[" +
                                            ".//span[normalize-space(text())='" +
                                            parentMenuText +
                                            "']" +
                                            "]"
                            )
                    )
            );

        } catch (Exception ignored) {
        }

        // -----------------------------------------------------
        // الـ locator الأصلي لباقي الـ Parent Menus
        // -----------------------------------------------------

        List<WebElement> buttons =
                driver.findElements(
                        By.xpath(
                                "//button[" +
                                        ".//span[normalize-space(text())='" +
                                        parentMenuText +
                                        "']" +
                                        "]"
                        )
                );

        // -----------------------------------------------------
        // DEBUG: Parent Menu Buttons
        // -----------------------------------------------------

        System.out.println(
                "===== DEBUG PARENT MENU: "
                        + parentMenuText
                        + " ====="
        );

        for (WebElement button : buttons) {

            try {

                System.out.println(
                        "BUTTON TEXT: ["
                                + button.getText()
                                + "]"
                );

                System.out.println(
                        "BUTTON DISPLAYED: ["
                                + button.isDisplayed()
                                + "]"
                );

            } catch (Exception e) {

                System.out.println(
                        "Could not read button"
                );
            }
        }

        System.out.println(
                "===== END DEBUG ====="
        );

        // -----------------------------------------------------
        // إذا لقينا Button ظاهر
        // -----------------------------------------------------

        for (WebElement button : buttons) {

            try {

                if (button.isDisplayed()) {

                    return button;
                }

            } catch (Exception ignored) {
            }
        }

        // -----------------------------------------------------
        // Fallback
        // -----------------------------------------------------

        List<WebElement> elements =
                driver.findElements(
                        By.xpath(
                                "//*[normalize-space(text())='" +
                                        parentMenuText +
                                        "']"
                        )
                );

        // -----------------------------------------------------
        // DEBUG: Text Elements
        // -----------------------------------------------------

        System.out.println(
                "===== DEBUG TEXT ELEMENTS: "
                        + parentMenuText
                        + " ====="
        );

        for (WebElement element : elements) {

            try {

                System.out.println(
                        "ELEMENT TAG: ["
                                + element.getTagName()
                                + "] TEXT: ["
                                + element.getText()
                                + "] DISPLAYED: ["
                                + element.isDisplayed()
                                + "]"
                );

            } catch (Exception e) {

                System.out.println(
                        "Could not read element"
                );
            }
        }

        System.out.println(
                "===== END TEXT ELEMENTS ====="
        );

        // -----------------------------------------------------
        // البحث عن أقرب Parent Button
        // -----------------------------------------------------

        for (WebElement element : elements) {

            try {

                if (!element.isDisplayed()) {
                    continue;
                }

                List<WebElement> parentButtons =
                        element.findElements(
                                By.xpath(
                                        "./ancestor::button[1]"
                                )
                        );

                if (!parentButtons.isEmpty()) {

                    return parentButtons.get(0);
                }

            } catch (Exception ignored) {
            }
        }

        // -----------------------------------------------------
        // لم يتم العثور على Parent Menu
        // -----------------------------------------------------

        return null;
    }


    // =========================================================
    // SAFE CLICK
    // =========================================================

    protected void clickElement(
            WebElement element) {

        try {

            wait.until(
                    ExpectedConditions.elementToBeClickable(
                            element
                    )
            );

            element.click();

        } catch (Exception e) {

            ((JavascriptExecutor) driver)
                    .executeScript(
                            "arguments[0].click();",
                            element
                    );
        }
    }


    // =========================================================
    // FIRST RECORD DETAILS
    // =========================================================

    protected String openFirstRecordDetailsAndScroll() {

        List<WebElement> records =
                driver.findElements(
                        By.cssSelector(
                                "table tbody tr:first-child " +
                                        "a[href*='/view/']"
                        )
                );

        if (records.isEmpty()) {
            return null;
        }

        WebElement firstRecord =
                records.get(0);

        String href =
                firstRecord.getAttribute("href");

        clickElement(firstRecord);

        sleep(WAIT_AFTER_CLICK);

        wait.until(
                ExpectedConditions.urlContains("/view/")
        );

        JavascriptExecutor js =
                (JavascriptExecutor) driver;

        for (int i = 0; i < 5; i++) {

            js.executeScript(
                    "window.scrollBy(0, 600);"
            );

            sleep(300);

            long height =
                    (long) js.executeScript(
                            "return document.body.scrollHeight"
                    );

            long scrollY =
                    (long) js.executeScript(
                            "return window.scrollY"
                    );

            if (scrollY + 900 >= height) {
                break;
            }
        }

        Assert.assertTrue(
                driver.getCurrentUrl()
                        .contains("/view/"),

                "لم يتم فتح صفحة تفاصيل الريكورد"
        );

        return href;
    }


    // =========================================================
    // ACTIVITY LOG
    // =========================================================

    protected boolean openActivityLogIfAvailable() {

        ((JavascriptExecutor) driver)
                .executeScript(
                        "window.scrollTo(0, 0);"
                );

        sleep(500);

        List<WebElement> activityLinks =
                driver.findElements(
                        By.xpath(
                                "//a[contains(normalize-space(.)," +
                                        "'سجل الحركات')]"
                        )
                );

        if (activityLinks.isEmpty()) {
            return false;
        }

        WebElement activityLink =
                activityLinks.get(0);

        if (!activityLink.isDisplayed()) {
            return false;
        }

        clickElement(activityLink);

        sleep(WAIT_AFTER_CLICK);

        wait.until(
                ExpectedConditions.urlContains(
                        "/activity-log/"
                )
        );

        return true;
    }


    // =========================================================
    // DOWNLOAD
    // =========================================================

    protected void clickDownloadButton() {

        List<WebElement> buttons =
                driver.findElements(
                        By.xpath(
                                "//button[" +
                                        ".//*[local-name()='title' " +
                                        "and contains(text()," +
                                        "'Download icon')]" +
                                        "]"
                        )
                );

        if (buttons.isEmpty()) {
            return;
        }

        WebElement downloadButton =
                buttons.get(0);

        clickElement(downloadButton);

        sleep(WAIT_AFTER_CLICK);
    }


    // =========================================================
    // RUN SCREEN
    // =========================================================

    protected void runScreenScenario(
            String parentMenuText,
            String childMenuText,
            String screenUrlPart,
            boolean hasData,
            boolean openActivityLog,
            boolean openDetails) {

        // =====================================================
        // Navigate
        // =====================================================

        navigateViaMenu(
                parentMenuText,
                childMenuText,
                screenUrlPart
        );

        // =====================================================
        // Details
        // =====================================================

        if (hasData && openDetails) {

            String detailsUrl =
                    openFirstRecordDetailsAndScroll();

            if (detailsUrl != null
                    && openActivityLog) {

                openActivityLogIfAvailable();
            }

            // =================================================
            // الرجوع للقائمة
            // =================================================

            driver.navigate().to(
                    BASE_URL +
                            screenUrlPart
            );

            sleep(WAIT_AFTER_CLICK);

            wait.until(
                    ExpectedConditions.urlContains(
                            screenUrlPart
                    )
            );

            sleep(WAIT_AFTER_CLICK);
        }

        // =====================================================
        // Download
        // =====================================================

        clickDownloadButton();
    }


    // =========================================================
    // VIEW ONLY SCREEN
    // =========================================================

    protected void runViewOnlyScenario(
            String parentMenuText,
            String childMenuText,
            String screenUrlPart) {

        // =====================================================
        // 1. Navigate from menu
        // =====================================================

        navigateViaMenu(
                parentMenuText,
                childMenuText,
                screenUrlPart
        );

        // =====================================================
        // 2. Open first record using View
        // =====================================================

        String detailsUrl =
                openFirstRecordDetailsAndScroll();

        // =====================================================
        // 3. Return to the list screen
        // =====================================================

        if (detailsUrl != null) {

            driver.navigate().to(
                    BASE_URL +
                            screenUrlPart
            );

            sleep(WAIT_AFTER_CLICK);

            wait.until(
                    ExpectedConditions.urlContains(
                            screenUrlPart
                    )
            );

            sleep(WAIT_AFTER_CLICK);
        }
    }


    // =========================================================
    // RUN ALL SCREENS
    // =========================================================

    @Test(
            description =
                    "Run all Nafith screens"
    )
    public void runAllScreens() {

        // =====================================================
        // 0. Dashboard
        // =====================================================

        openDashboardAndExport();

        // =====================================================
        // 1. Fleet
        // =====================================================

        runScreenScenario(
                "التسجيل",
                "الأسطول",
                "/app/fleets",
                true,
                true,
                true
        );

        // =====================================================
        // 2. Users
        // =====================================================

        runScreenScenario(
                "التسجيل",
                "المستخدمين",
                "/app/users",
                true,
                true,
                true
        );

        // =====================================================
        // 3. Tags
        // =====================================================

        runScreenScenario(
                "التسجيل",
                "الملصقات",
                "/app/tagging",
                true,
                true,
                true
        );

        // =====================================================
        // 4. Cargo Release Permits
        // =====================================================

        runScreenScenario(
                null,
                "أذونات التحميل",
                "/app/cargo-release",
                true,
                true,
                true
        );

        // =====================================================
        // 5. Permits
        // =====================================================

        runScreenScenario(
                null,
                "التصاريح",
                "/app/cargo-permits",
                true,
                true,
                true
        );

        // =====================================================
        // 6. Plans
        // =====================================================

        runScreenScenario(
                "الخطط",
                "الخطط",
                "/app/plans-managements",
                true,
                false,
                true
        );

        // =====================================================
        // 6.1 Scheduling Release Plans
        // =====================================================

        runViewOnlyScenario(
                "الخطط",
                "خطط فك الجدولة",
                "/app/scheduling-release-plans"
        );

        // =====================================================
        // 6.2 Waiting Release Plans
        // =====================================================

        runViewOnlyScenario(
                "الخطط",
                "خطط فك الانتظار",
                "/app/waiting-release-plans"
        );

        // =====================================================
        // 7. Financial Transactions
        // =====================================================

        runScreenScenario(
                null,
                "الحركات المالية",
                "/app/financial-transactions",
                true,
                false,
                true
        );

        // =====================================================
        // 8. Gate Transactions
        // =====================================================

        runScreenScenario(
                "البوابات",
                "المعاملات",
                "/app/transactions",
                true,
                true,
                true
        );

        // =====================================================
        // 8.1 Live Gates
        // =====================================================
    }


    // =========================================================
    // SLEEP
    // =========================================================

    protected void sleep(
            long milliseconds) {

        try {

            Thread.sleep(milliseconds);

        } catch (InterruptedException e) {

            Thread.currentThread()
                    .interrupt();
        }
    }
}