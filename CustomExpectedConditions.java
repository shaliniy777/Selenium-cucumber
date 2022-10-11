/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.helpers;

import java.util.List;

import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.Select;

/**
 * The type Custom expected conditions.
 */
public class CustomExpectedConditions {
    private static final String ELEMENTS_COUNT_LITERAL = "Elements count ";
    private static final String DIFFERENT_THAN_LITERAL = " to be different than ";
    private static final String WITHIN_LIST_LITERAL = " within list ";

    private CustomExpectedConditions() {
    }

    /**
     * Wait for elements count change expected condition.
     *
     * @param elements the elements
     * @param oldCount the old count
     * @return the expected condition or return null when the element count doesn't change
     */
    public static ExpectedCondition<List<WebElement>> waitForElementsCountChange(final List<WebElement> elements,
                                                                                 final Integer oldCount) {
        return new ExpectedCondition<List<WebElement>>() {
            public List<WebElement> apply(WebDriver driver) {
                if (elements.size() != oldCount) {
                    return elements;
                } else {
                    return null;
                }
            }

            public String toString() {
                return ELEMENTS_COUNT_LITERAL + elements + DIFFERENT_THAN_LITERAL + oldCount;
            }
        };
    }

    /**
     * Wait for elements count at least expected condition.
     *
     * @param elements      the elements
     * @param expectedCount the expected count
     * @return the expected condition or null in case of WebDriver exception or less than expected element size
     */
    public static ExpectedCondition<List<WebElement>> waitForElementsCountAtLeast(final List<WebElement> elements,
                                                                                  final int expectedCount) {
        return new ExpectedCondition<List<WebElement>>() {
            public List<WebElement> apply(WebDriver driver) {
                try {
                    if (elements.size() >= expectedCount) {
                        return elements;
                    } else {
                        return null;
                    }
                } catch (WebDriverException ex) {
                    return null;
                }
            }

            public String toString() {
                return ELEMENTS_COUNT_LITERAL + elements.size() + " to be greater than or equal to " + expectedCount;
            }
        };
    }

    /**
     * Wait for elements count equals expected condition.
     *
     * @param elements      the elements
     * @param expectedCount the expected count
     * @return the expected condition or null in case of WebDriver exception or if expected and actual element size differs
     */
    public static ExpectedCondition<List<WebElement>> waitForElementsCountEquals(final List<WebElement> elements,
                                                                                 final int expectedCount) {
        return new ExpectedCondition<List<WebElement>>() {
            public List<WebElement> apply(WebDriver driver) {
                try {
                    if (elements.size() == expectedCount) {
                        return elements;
                    } else {
                        return null;
                    }
                } catch (WebDriverException ex) {
                    return null;
                }
            }

            public String toString() {
                return ELEMENTS_COUNT_LITERAL + elements.size() + " to be equal to " + expectedCount;
            }
        };
    }

    /**
     * Visibility of all elements expected condition.
     *
     * @param elements the elements
     * @return the expected condition or null in case of element is not  displayed or StaleElementReferenceException
     */
    public static ExpectedCondition<List<WebElement>> visibilityOfAllElements(
            final List<WebElement> elements) {
        return new ExpectedCondition<List<WebElement>>() {
            @Override
            public List<WebElement> apply(WebDriver driver) {
                for (WebElement element : elements) {
                    try {
                        if (!element.isDisplayed()) {
                            return null;
                        }
                    } catch (StaleElementReferenceException e) {
                        return null;
                    }
                }
                return elements.size() > 0 ? elements : null;
            }

            @Override
            public String toString() {
                return "visibility of all " + elements;
            }
        };
    }

    /**
     * Wait for window to close expected condition.
     *
     * @param windowHandle the window handle
     * @return the expected condition
     */
    public static ExpectedCondition<Boolean> waitForWindowToClose(
            final String windowHandle) {
        return new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                return !driver.getWindowHandles().contains(windowHandle);
            }

            @Override
            public String toString() {
                return "Closing of window with handle id " + windowHandle;
            }
        };
    }

    /**
     * Element with text visible expected condition.
     *
     * @param elements    the elements
     * @param elementText the element text
     * @param partial     the partial
     * @return the expected condition
     */
    public static ExpectedCondition<WebElement> elementWithTextVisible(final List<WebElement> elements,
                                                                       final String elementText, boolean partial) {
        return new ExpectedCondition<WebElement>() {
            @Override
            public WebElement apply(WebDriver driver) {
                for (WebElement element : elements) {
                    try {
                        if ((!partial && element.getText().trim().equals(elementText) || (partial
                                && element.getText().trim().contains(elementText)))
                                && element.isDisplayed()) {
                            return element;
                        }
                    } catch (StaleElementReferenceException e) {
                        return null;
                    }
                }
                return null;
            }

            @Override
            public String toString() {
                if (partial) {
                    return "Visibility of element with partial text " + elementText + WITHIN_LIST_LITERAL + elements;
                } else {
                    return "Visibility of element with text " + elementText + WITHIN_LIST_LITERAL + elements;
                }

            }
        };
    }

    /**
     * Element with attribute value expected condition.
     *
     * @param elements       the elements
     * @param attributeName  the attribute name
     * @param attributeValue the attribute value
     * @return the expected condition
     */
    public static ExpectedCondition<WebElement> elementWithAttributeValue(final List<WebElement> elements,
                                                                          final String attributeName, final String attributeValue) {
        return new ExpectedCondition<WebElement>() {
            @Override
            public WebElement apply(WebDriver driver) {
                for (WebElement element : elements) {
                    try {
                        if (element.getAttribute(attributeName).trim().equals(attributeValue)) {
                            return element;
                        }
                    } catch (StaleElementReferenceException e) {
                        return null;
                    }
                }
                return null;
            }

            @Override
            public String toString() {
                return "Element with attribute " + attributeName + " value equal to " + attributeValue + WITHIN_LIST_LITERAL
                        + elements;
            }
        };
    }

    /**
     * Wait for drop down value expected condition.
     *
     * @param dropdown      the dropdown
     * @param expectedValue the expected value
     * @param equal         the equal
     * @return the expected condition
     */
    public static ExpectedCondition<Boolean> waitForDropDownValue(
            final Select dropdown, final String expectedValue, final Boolean equal) {
        return new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                String dropdownValue = dropdown.getFirstSelectedOption().getText();
                if (equal.equals(dropdownValue.equals(expectedValue))) {
                    return Boolean.TRUE;
                } else {
                    return Boolean.FALSE;
                }
            }

            @Override
            public String toString() {
                return "Value of dropdown " + dropdown + " " + (equal ? " equals to " : " not equals to ") + expectedValue;
            }
        };
    }

    /**
     * Wait for text field value expected condition.
     *
     * @param element       the element
     * @param expectedValue the expected value
     * @param equal         the equal
     * @return the expected condition
     */
    public static ExpectedCondition<Boolean> waitForTextFieldValue(
            final WebElement element, final String expectedValue, final Boolean equal) {
        return new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                String textFieldValue = element.getAttribute("value");
                if (equal.equals(textFieldValue.equals(expectedValue))) {
                    return Boolean.TRUE;
                } else {
                    return Boolean.FALSE;
                }
            }

            @Override
            public String toString() {
                return "Value of text field " + element + " " + (equal ? " equals to " : " not equals to ") + expectedValue;
            }
        };
    }

    /**
     * Wait for element content change expected condition.
     *
     * @param element    the element
     * @param oldContent the old content
     * @return the expected condition
     */
    public static ExpectedCondition<WebElement> waitForElementContentChange(final WebElement element,
                                                                            final String oldContent) {
        return new ExpectedCondition<WebElement>() {
            public WebElement apply(WebDriver driver) {
                try {
                    if (!element.getText().trim().equals(oldContent.trim())) {
                        return element;
                    } else {
                        return null;
                    }
                } catch (StaleElementReferenceException ex) {
                    return null;
                }
            }

            public String toString() {
                return "Elements content " + element + DIFFERENT_THAN_LITERAL + oldContent;
            }
        };
    }

    /**
     * Wait for element attribute change expected condition.
     *
     * @param element       the element
     * @param attributeName the attribute name
     * @param oldContent    the old content
     * @return the expected condition
     */
    public static ExpectedCondition<WebElement> waitForElementAttributeChange(final WebElement element,
                                                                              final String attributeName, final String oldContent) {
        return new ExpectedCondition<WebElement>() {
            public WebElement apply(WebDriver driver) {
                try {
                    if (!element.getAttribute(attributeName).trim().equals(oldContent.trim())) {
                        return element;
                    } else {
                        return null;
                    }
                } catch (StaleElementReferenceException ex) {
                    return null;
                }
            }

            public String toString() {
                return "Attribute " + attributeName + " of element " + element + DIFFERENT_THAN_LITERAL + oldContent;
            }
        };
    }

    /**
     * Wait for window with title expected condition.
     *
     * @param windowTitle the window title
     * @return the expected condition
     */
    public static ExpectedCondition<String> waitForWindowWithTitle(final String windowTitle) {
        return new ExpectedCondition<String>() {
            @Override
            public String apply(WebDriver driver) {
                for (String windowHandle : driver.getWindowHandles()) {
                    driver.switchTo().window(windowHandle);
                    if (driver.getTitle().equals(windowTitle)) {
                        return windowHandle;
                    }
                }
                return null;
            }

            @Override
            public String toString() {
                return "Waiting of window with handle id " + windowTitle;
            }
        };
    }

    /**
     * Wait for elements' count to stop changing over time.
     * This means it is highly possible that loading is completed.
     *
     * @param elements a list of web elements
     * @return the expected condition
     */
    public static ExpectedCondition<Boolean> waitForElementsStableCount(final List<WebElement> elements) {
        return new ExpectedCondition<Boolean>() {
            private int count = -1;

            @Override
            public Boolean apply(WebDriver driver) {
                int previousCount = count;
                count = elements.size();
                return previousCount == count;
            }

            @Override
            public String toString() {
                return "stop changing elements' count over time. Possible solution is to reconfigure time intervals."; //NOI18N
            }
        };
    }
}
