package org.xtext.emn.selenium.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.safari.SafariDriver;
import org.xtext.emn.selenium.ISeleniumService;
import org.xtext.emn.selenium.interpreter.handlers.InterpreterAction;

public class SeleniumService implements ISeleniumService {

	//Singleton pattern
	private SeleniumService(){

	}
	private static class SeleniumServiceHandler {
		private final static ISeleniumService instance = new SeleniumService();
	}
	public static ISeleniumService getInstance() {
		return SeleniumServiceHandler.instance;
	}

	private WebDriver driver;



	private void log(String msg) {
		System.out.println("--" + driver.getClass().toString() + " --> " + msg);
	}
	private void fail(String msg) {
		System.err.println("--" + driver.getClass().toString() + " --> " + msg);
	}


	/* TESTED */
	@Override
	public void setDriver(String browserName) {
		retrieveProperties();
		log("driver set to " + browserName);
		if("firefox".equalsIgnoreCase(browserName)) {
			try {
				driver = new FirefoxDriver();
			}catch(Exception e) {
				String binaryPath = System.getProperty("emn.selenium.config.firefox");
				System.out.println("binpath: " +binaryPath);
				System.setProperty("webdriver.firefox.driver", binaryPath);
				driver = new FirefoxDriver(new FirefoxBinary(new File(binaryPath)), new FirefoxProfile());
			}
		} else if("chrome".equalsIgnoreCase(browserName)) {
			try {
				driver = new ChromeDriver();
			}catch(Exception e) {
				String binaryPath = System.getProperty("emn.selenium.config.chrome");
				System.out.println("binpath: " +binaryPath);
				System.setProperty("webdriver.chrome.driver", binaryPath);
				driver = new ChromeDriver();
			}
		} else if("safari".equalsIgnoreCase(browserName)) {
			try {
				driver = new SafariDriver();
			}catch(Exception e) {
				String binaryPath = System.getProperty("emn.selenium.config.safari");
				System.out.println("binpath: " +binaryPath);
				System.setProperty("webdriver.safari.driver", binaryPath);
				driver = new SafariDriver();
			}
		}else if("ie".equalsIgnoreCase(browserName)) {
			try {
				driver = new EdgeDriver();
			}catch(Exception e) {
				String binaryPath = System.getProperty("emn.selenium.config.ie");
				System.out.println(	"binpath: " +binaryPath);
				System.setProperty("webdriver.edge.driver", binaryPath);
				driver = new EdgeDriver();
				System.out.println(driver);
			}
		} else 
			throw new RuntimeException("unknown browser : " + browserName);
	}

	/* TESTED */
	private void retrieveProperties() {
		Properties p = new Properties();
		System.out.println("Parsing property file from: " + System.getProperty("emn.selenium.config"));
		try {
			p.load(new FileInputStream(System.getProperty("emn.selenium.config")));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		for(Object key : p.keySet()) {
			System.setProperty((String)key, (String)p.get(key));
		}
	}

	/* TESTED */
	@Override
	public void closeDriver() {
		log("closing driver...");
		driver.close();
	}

	/* TESTED */
	@Override
	public void gotoLink(WebElement link) {
		log("gotoLink(Welem: "+link.getText() + " to "+link.getAttribute("href")+")");
		try {
			link.click();
		}catch (Exception e) {
			driver.get(link.getAttribute("href"));
		}
	}


	/* TESTED */
	@Override
	public void gotoLink(String link) {
		log("gotoLink("+link+")");
		driver.get(link);
	}

	/* TESTED */
	@Override
	public void fillInput(WebElement e, String str) {
		e.sendKeys(str);
	}


	/* TESTED */
	@Override
	public void clickButton(WebElement e) {
		e.click();
	}


	/* TESTED */
	@Override
	public boolean isChecked(WebElement e) {
		log("isChecked: " +e.isSelected());
		return e.isSelected();
	}

	/* TESTED */
	@Override
	public boolean isEnabled(WebElement e) {
		log("isEnabled: " +e.isEnabled());
		return e.isEnabled();
	}

	/* TESTED */
	@Override
	public boolean exists(WebElement e) {
		return e!=null;
	}

	/* TESTED */
	@Override
	public boolean contains(WebElement e, String s) {
		if(e == null) return false;
		if(e.getAttribute("value") != null)
			return s.contains(e.getAttribute("value"));
		if(e.getText() != null)
			return s.contains(e.getText());
		if(e.getAttribute("innerHTML") != null)
			return s.contains(e.getAttribute("innerHTML"));
		return false;
	}

	/* TESTED */
	@Override
	public boolean equals(WebElement e, String s) {
		if(e == null) return false;
		return s.equalsIgnoreCase(e.getAttribute("value")) || s.equalsIgnoreCase(e.getText()) || s.equalsIgnoreCase(e.getAttribute("innerHTML"));
	}






	/* TESTED */ 
	@Override
	public WebElement getButton(String identifier) {
		log("trying to find button: '" + identifier + "'");
		WebElement e = complexFind(identifier);
		if(e == null) 
			e = complexFind("input", "value", identifier);

		if(e != null)
			if(e.getTagName().equals("input") || e.getTagName().equals("button"))
				return e;

		fail("ERROR: element '"+identifier+"' not found on this page.");
		return null;
	}

	/* TESTED */
	@Override
	public WebElement getLink(String identifier) {
		log("trying to find link: '" + identifier + "'");
		WebElement e = complexFind(identifier);

		try {
			if(e == null) 
				e = complexFind("area", "href", identifier);
			if(e == null)
				e = xpathFindA(identifier);

			if(e != null)
				return e;

			fail("ERROR: element '"+identifier+"' not found on this page.");
			return null;
		}catch(NoSuchElementException ex) {
			fail(ex.getMessage());
			return null;
		}
	}

	/* TESTED */
	@Override
	public WebElement getCheckbox(String identifier) {
		log("trying to find checkbox: '" + identifier + "'");
		WebElement e = complexFind(identifier);
		if(e != null)
			if(e.getTagName().equals("input"))
				return e;
		fail("ERROR: element '"+identifier+"' not found on this page.");
		return null;
	}

	
	@Override
	public WebElement getText(String identifier) {
		log("trying to find text: '" + identifier + "'");
		WebElement e = complexFind(identifier);
		if(e != null) {
			return e;
		}
		fail("ERROR: element '"+identifier+"' not found on this page.");
		return null;
	}


	/* TESTED */
	@Override
	public WebElement getInput(String identifier) {
		log("trying to find input: '" + identifier + "'");
		WebElement e = complexFind(identifier);
		if(e != null)
			if(e.getTagName().equals("input"))
				return e;
		fail("ERROR: element '"+identifier+"' not found on this page.");
		return null;
	}

	/* TESTED */
	public void tickCheckbox(WebElement e) {
		e.click();
	}


	/* TESTED */
	private WebElement complexFind(String identifier) {
		WebElement elem = null;
		try {
			elem = driver.findElement(By.id(identifier));
			return elem;
		} catch (NoSuchElementException e) {
			try {
				elem = driver.findElement(By.name(identifier));
				return elem;
			}catch (NoSuchElementException ex) {
				try {
					elem = driver.findElement(By.cssSelector(identifier));
					return elem;
				}catch (NoSuchElementException exe) {
					try {
						elem = driver.findElement(By.linkText(identifier));
						return elem;
					}catch (NoSuchElementException exep) {
						try {
							elem = driver.findElement(By.xpath(identifier));
							return elem;
						} catch (NoSuchElementException exepr) {
							return null;
						}
					} 
				}
			}
		} 
	}

	/* TESTED */
	private WebElement xpathFindA(String identifier) {
		return driver.findElement(
				By.xpath("//a[*[contains(text(), \""+identifier+"\")] or contains(text(), \""+identifier+"\") or contains(@title, \""+identifier+"\") or contains(@href, \""+identifier+"\")]"));
	}

	/* TESTED */
	private WebElement complexFind(String tagName, String attributeName, String value) {
		List<WebElement> elems = driver.findElements(By.tagName(tagName));
		WebElement result = null;
		for(WebElement e : elems) {
			//log("finding... '" +cleanShittyChars(e.getAttribute(attributeName))+ "' with '" + value.trim() +"'");
			if(cleanShittyChars(e.getAttribute(attributeName).trim()).equalsIgnoreCase(cleanShittyChars(value.trim())))  {
				result = e;
				break;
			}
		}
		return result;
	}

	/* TESTED */
	private String cleanShittyChars(String str) {
		StringBuffer res = new StringBuffer();
		for(int i=0; i < str.length(); ++i) {
			if((byte)str.charAt(i) >= 0) 
				//System.out.print(" "+(byte)str.charAt(i));
				res.append(str.charAt(i));
		}
		//System.out.println("");
		return res.toString();
	}

	@Override
	public List<WebElement> getButtons(String identifier) {
		List<WebElement> elems;
		List<WebElement> list = new ArrayList<WebElement>();
		try {
			elems = driver.findElements(By.name(identifier));
			elems.addAll(driver.findElements(By.cssSelector(identifier)));
			elems.addAll(driver.findElements(By.partialLinkText(identifier)));
			elems.addAll(driver.findElements(By.xpath(identifier)));
			list = elems.stream()
					.filter(e -> (e.getTagName() == "button" || e.getTagName() == "input"))
					.collect(Collectors.toList());
		}catch(Exception e) {
			fail("ERROR: no buttons matching '"+identifier+"' found on this page.");
		}

		return list;
	}


	@Override
	public List<WebElement> getLinks(String identifier) {
		List<WebElement> elems;
		List<WebElement> list = new ArrayList<WebElement>();
		try {
			elems = driver.findElements(By.name(identifier));
			elems.addAll(driver.findElements(By.cssSelector(identifier)));
			elems.addAll(driver.findElements(By.partialLinkText(identifier)));
			elems.addAll(driver.findElements(By.xpath(identifier)));
			list = elems.stream()
					.filter(e -> (e.getTagName() == "a" || e.getTagName() == "area"))
					.collect(Collectors.toList());
		}catch(Exception e) {
			fail("ERROR: no links matching '"+identifier+"' found on this page.");
		}

		return list;
	}


	@Override
	public List<WebElement> getCheckboxes(String identifier) {
		List<WebElement> elems;
		List<WebElement> list = new ArrayList<WebElement>();
		try {
			elems = driver.findElements(By.name(identifier));
			elems.addAll(driver.findElements(By.cssSelector(identifier)));
			elems.addAll(driver.findElements(By.partialLinkText(identifier)));
			elems.addAll(driver.findElements(By.xpath(identifier)));
			list = elems.stream()
					.filter(e -> (e.getTagName() == "input"))
					.collect(Collectors.toList());
		}catch(Exception e) {
			fail("ERROR: no checkboxes matching '"+identifier+"' found on this page.");
		}

		return list;
	}


	@Override
	public List<WebElement> getInputs(String identifier) {
		List<WebElement> elems;
		List<WebElement> list = new ArrayList<WebElement>();
		try {
			elems = driver.findElements(By.name(identifier));
			elems.addAll(driver.findElements(By.cssSelector(identifier)));
			elems.addAll(driver.findElements(By.partialLinkText(identifier)));
			elems.addAll(driver.findElements(By.xpath(identifier)));
			list = elems.stream()
					.filter(e -> (e.getTagName() == "input"))
					.collect(Collectors.toList());
		}catch(Exception e) {
			fail("ERROR: no inputs matching '"+identifier+"' found on this page.");
		}

		return list;
	}

	@Override
	public List<WebElement> getTexts(String identifier) {
		List<WebElement> elems = new ArrayList<WebElement>();
		try {
			elems = driver.findElements(By.name(identifier));
			elems.addAll(driver.findElements(By.cssSelector(identifier)));
			elems.addAll(driver.findElements(By.partialLinkText(identifier)));
			elems.addAll(driver.findElements(By.xpath(identifier)));
			
		}catch(Exception e) {
			fail("ERROR: no texts matching '"+identifier+"' found on this page.");
		}
		return elems;
	}




}
