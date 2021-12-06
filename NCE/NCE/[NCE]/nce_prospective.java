/*Author: Reynald Perez
 * Co-author:James Dionisio
 * Last Update: Nov 16,2020
 * CREATE NCE AUTOMATION
 * */
package myProject;

import static java.lang.System.exit;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;


public class nce_prospective {	
	protected static String thread;
    protected static String query;
    protected static String rsQuery;
    protected static List<String> dataList = new ArrayList<String>();	
    protected static int n;
    protected static WebDriver driver;
    protected static String id;
	protected static String projIDStr;
	protected static String positionID;
	protected static String fteDateStr;
	protected static String reasonStr;
	protected static String parallelKey;
	protected static String currentStatus;
	protected static Connection connection;
	protected static Statement stmt;
	protected static PreparedStatement update;
	protected static PreparedStatement errorUpdate;
	protected static Statement count;
	protected static ResultSet rs;
	protected static ResultSet countRs;
	protected static int rowCount;
	protected static Instant start =null;
	protected static Instant end =null;
	protected static Instant startRec =null;
	protected static Instant endRec =null;
	protected static Duration timeElapsed=null;
	protected static Duration timeElapsedRec=null;
	protected static String warning;
	protected static String error;
	protected static String reqID;
	
    public static void main(String[] args) throws Exception { 
    	try {getURL();} catch (Throwable e) {System.exit(0);}  	
    	try {run(args[0]);} catch (Throwable e) {System.exit(0);}
    }
   
    @SuppressWarnings("null")
	public static void run(String... args) throws Throwable {
        if (args[0].length() > 0 ) { 
        	login();
        	search_menu().click();
        	request_submenu().click();
        	submenu_wfm().click();
        	
        	thread = args[0].toString().trim();	
        	connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/"+"PPMC", "postgres", "admin");
			count = connection.createStatement();
			query="SELECT COUNT(*) FROM public.nce_prospect WHERE key_status IN ('PENDING','ONGOING') AND parallel_key='"+thread+"';";
			rsQuery="SELECT * FROM public.nce_prospect WHERE key_status IN ('PENDING','ONGOING') AND parallel_key='"+thread+"' ORDER BY id;";
	        countRs = count.executeQuery(query);
	        	countRs.next();	
	            rowCount = countRs.getInt(1);
	            start = Instant.now();
	            System.out.println("");
	            System.out.println("[THREAD "+ thread +"]: PROCESSING...");
	            System.out.println("TIME START: "+start);
	            System.out.println("TOTAL 'PROSPECTIVE' DEMAND(S) ["+rowCount+"]");
	            if (rowCount==0) {driver.quit();System.exit(0);}
	            for (int x = 0; x < rowCount; x++) {
	    	        try {
	    	            if (connection != null) {
	    	             
	    	            startRec= Instant.now();
	   	                stmt = connection.createStatement();
	   	             update = connection.prepareStatement("UPDATE nce_prospect SET key_status = ?, request_id = ?, status = ?, duration = ?, position_id = ? WHERE parallel_key=? AND id = ?;");
	   	                rs = stmt.executeQuery(rsQuery);	
	   			                while (rs.next()) {	
	   			                	System.out.println("");
	   			                	error="";
	   			                	projIDStr=rs.getString("project_id".trim()); 			                   	 
				                	id=rs.getString("id".trim());
				                	fteDateStr=rs.getString("fte".trim());
				                	System.out.println("RECORD ["+id+"] - PROJECT ID ["+projIDStr+"] >> PROCESSING");
				                	
				                	dataList.clear();
				                	for (int count=1; count <= 47;count++) {
				                		dataList.add(rs.getString(count));
				                	}
				                	
				                	//[MAIN]//
				                	populate_projectDetails(projIDStr, fteDateStr, dataList);
				                	
				                	if (!error.isEmpty()) {
				                		   ongoingUpate();
						                	update.executeUpdate();
						                	System.out.println();
										System.out.println("RECORD ["+id+"] - PROJECT ID ["+projIDStr+"] >> [SKIPPED]");
				                	}else {
				                		//click submit
				                		submit();
				                						                		
				                			//catch location constraints issue
				                			Thread.sleep(300);
				                			locCons(dataList);
				                			error="";
				                			submit();
				                			error();
				                			
										alertHandler();
				                		
				                		//PROCEED TO CLICK REQUEST ID AND APPROVALS
				                		if(error.isEmpty()) {
						                	reqID().click();
						                	completePLM().click();
						    					ongoingUpate();
						    					update.executeUpdate();	
						       					projUnsold().click();
						       					Thread.sleep(100);
						    					indicatorES(dataList).click();	

						                	releaseForAppvl().click();
						                		ongoingUpate();
						                		update.executeUpdate();
						                	statusElemWait();currentStatus = statusWait();
						                	
						                	
						                	int ctr = 0;
						                	
						                	//Check which Approval instance will be next to Ready for Approval
						                	do {
						                	  // STATUS: PENDING ADL APPROVAL         
						                	  Thread.sleep(500);
						                	  if(currentStatus.trim().contains("Pending ADL Approval")) {
						                		  System.out.println("RECORD ["+id+"] - PROJECT ID ["+projIDStr+"] >>  APPROVAL RELEASED");
						                		  System.out.println("RECORD ["+id+"] - PROJECT ID ["+projIDStr+"] >> " + currentStatus);
						                		  if(approveBtn() && currentStatus.trim().contains("Pending ADL Approval")) {
						                		  
						                		   	approveADL().click();
						                				
						                		  } else {
						                			  error="[Error] Approval Button Not Activated on ADL Approval"; 
						                			  ongoingUpateWithReqId();
						                		  }
						                	  }
						                	  // STATUS: PENDING AE APPROVAL
						                	  statusElemWait();currentStatus = statusWait();
						                	  reqID = getReqIDt().getAttribute("innerText");
						                	  if(currentStatus.trim().contains("Pending AE Approval")) {
						                		  System.out.println("RECORD ["+id+"] - REQUEST ID ["+reqID+"] >>  APPROVAL RELEASED");
						                		  if(currentStatus.trim().contains("Pending AE Approval")) {
						                			  
						                			  approveAE().click();
						                										                			  
						                		  } else {
						                			  error="[Error] Approval Button Not Activated on AE Approval"; 
						                			  ongoingUpateWithReqId();
						                		  }
						                	  }
							                } while (currentStatus.trim().contains("Pending ADL Approval")||currentStatus.trim().contains("Pending AE Approval"));
						                	  
						                	  // STATUS: PENDING DMD PLANNER APPROVAL
//						                	  ctr=0;
//						                	  do {statusElemWait();currentStatus = statusWait();
//						                		  System.out.println("RECORD ["+id+"] - REQUEST ID ["+reqID+"] >> " + currentStatus);
//						                		  if(approveBtnDmdPlanner() && currentStatus.trim().contains("Pending Dmd Planner Approval")) {
//						                			  
//						                			  String HeaderTxt = driver.findElement(By.xpath("//*[@id=\"DB0_0\"]")).getText();
//						                			  String expectedHeading = "Cancel";
//						                				if(expectedHeading.equalsIgnoreCase(HeaderTxt)) {
//						                					 System.out.println("==Refresh Page==");
//						                					 driver.navigate().refresh();
//						                				}else {
//						                					approveADLDmdPlanner().click();
//						                				}
//						                			  
//						                			  
//						                		  } else {
//						                			  error="[Error] Approval Button Not Activated on DMD Approval"; 
//						                			  ongoingUpateWithReqId();
//						                		  }
//						                		  if(ctr == 10) {
//						                			  break;
//						                		  }
//						                		  ctr++;
//							                	} while (currentStatus.trim().contains("Pending Dmd Planner Approval"));
				                	  			                	  
						                	  
						                	  //STATUS: ES APPROVAL
//						                	  do {statusElemWait();currentStatus = statusWait();
//					                		  System.out.println("RECORD ["+id+"] - REQUEST ID ["+projIDStr+"] >> " + currentStatus);
//					                		  if(approveBtnES() && currentStatus.trim().contains("Pending ES Approval")) {
//					                		String HeaderTxt = driver.findElement(By.xpath("//*[@id=\"DB0_0\"]")).getText();
//					                		String expectedHeading = "Cancel";
//			                				if(expectedHeading.equalsIgnoreCase(HeaderTxt)) {
//			                					 System.out.println("==Refresh Page==");
//			                					 driver.navigate().refresh();
//			                				}else {
//					                			  approveES().click();
//			                				}
//			                				} else {
//					                			  error="[Error] Approval Button Not Activated on ES Approval"; 
//					                			  ongoingUpateWithReqId();
//					                		  }
//						                	} while (currentStatus.trim().contains("Pending ES Approval"));
			                	  
						                	  
							                  // STATUS: PLM APPROVED
						                	  ctr=0;
							                  do {statusElemWait();currentStatus = statusWait();
							                  Thread.sleep(500);
						                		  if(currentStatus.trim().contains("PLM Approved")) {
						                			  System.out.println("RECORD ["+id+"] - REQUEST ID ["+reqID+"] >> " + currentStatus);
						                			  String HeaderTxt = driver.findElement(By.xpath("//*[@id=\"DRIVEN_CH_41\"]")).getText();
													  System.out.println("No PLM Approved Error: "+HeaderTxt.isEmpty());
						                			  if(HeaderTxt.isEmpty()) {
														  error="";
														  moveToSp().click();
													  } else {
														  error="[Error]" + HeaderTxt;
													  }
							                	  }
							                	  ctr++;
							                      	if(ctr==5) {
							                      		break;
							                      	}  
							                	} while (currentStatus.trim().contains("PLM Approved"));
							                	
							                	
							                  // STATUS: STAFFING APPROVED
							                  ctr=0;
							                  do {statusElemWait();currentStatus = statusWait();
							                  	Thread.sleep(500);
						                		  System.out.println("RECORD ["+id+"] - REQUEST ID ["+reqID+"] >> " + currentStatus);
						                		  if(currentStatus.trim().contains("Staffing Approved")) {
						                			  System.out.println("RECORD ["+id+"] - REQUEST ID ["+reqID+"] >> " + currentStatus);
						                			  String HeaderTxt = driver.findElement(By.xpath("//*[@id=\"DRIVEN_CH_41\"]")).getText();
													  System.out.println("No PLM Approved Error: "+HeaderTxt.isEmpty());
						                			  if(HeaderTxt.isEmpty()) {
														  error="";
														  moveToSp().click();
													  } else {
														  error="[Error]" + HeaderTxt;
													  }
							                	  }
							                	  ctr++;
							                      	if(ctr==5) {
							                      		break;
							                      	} 
							                	} while (currentStatus.trim().contains("Staffing Approved"));

						                	  
						                	  statusElemWait();currentStatus = statusWait();
							                  Thread.sleep(500);
							                  // STATUS: STAFFING APPROVED
							                  if (currentStatus.trim().contains("Position Created in SP")) {
												  error="DONE"; 
											  }
							                  if (currentStatus.trim().contains("Pending Org Lead Approval")) {
												  error="DONE"; 
											  }

							                  	Thread.sleep(500);
							                  	ongoingUpateWithReqId();
							                  	reqID = getReqIDt().getAttribute("innerText");
							                  	
							                	update.executeUpdate();
							                	System.out.println();
												System.out.println("RECORD ["+id+"] - PROJECT ID ["+projIDStr+"] - REQUEST ID ["+reqID+"] >> [SUCCESSFUL]");
				                		} else {
				                			Thread.sleep(500);
				                			ongoingUpate();
						                	update.executeUpdate();
						                	reqID = getReqIDt().getAttribute("innerText");
						                	System.out.println();
						                	System.out.println("RECORD ["+id+"] - PROJECT ID ["+projIDStr+"] - REQUEST ID ["+reqID+"] >> [SKIPPED]");
						                	ongoingUpateWithReqId();
				                		}
				                		

				                	}
				                	
				                	Thread.sleep(1500);
				                	search_menu().click();
				                	Thread.sleep(500);
				                	request_submenu().click();
				                	Thread.sleep(500);
				                	submenu_wfm().click();
				                	Thread.sleep(500);
				                	alertHandlermenu();
									
	   			                }
	    	               break;
	    	            } else {
	    	            	System.out.println("No connection.");
	    	            }
	    	        } catch (Exception e) {
	    	        	System.out.println("You're here");
	    	        	ongoingUpate();
	                	update.executeUpdate();
	                	ongoingUpateWithReqId();
	                	System.out.println();
	                	System.out.println("RECORD ["+id+"] - PROJECT ID ["+projIDStr+"] >> [SKIPPED]");
	                		
	    	        }
	    	  		}
	    			 count.close();
	                 countRs.close();
	                 connection.close();
	                 driver.close();
	             	 end = Instant.now();
	             	System.out.println("");
	             	System.out.println("TIME ENDED: "+end);
		         	timeElapsed = Duration.between(start, end);
		         	System.out.println("TIME ELAPSED: "+ timeElapsed.toMinutes()+" MINUTES");         
	    		
        }else{
            System.out.println("[ERROR] INVALID INPUT");
        }
        exit(0);
    }
    
	public static WebElement approveADLDmdPlanner() {
		for (int x = 0; x < 20; x++) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, 10);
			By elemPath = By.xpath("//*[@id=\"DB0_0\"]");
			WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
			wait.until(ExpectedConditions.elementToBeClickable(elem));
			WebElement element = driver.findElement(By.xpath("//*[@id=\"DB0_0\"]"));
			System.out.println("RECORD ["+id+"] - PROJECT ID ["+projIDStr+"] >> [Approved DMD PLANNER]");
			return element;
		} catch (Exception e) {
			driver.navigate().refresh();
			System.out.println("[WAITING] Approval BUTTON");
		}
		}
		return null;
	}
    
	public static boolean approveBtnDmdPlanner() {
		for (int x = 0; x < 20; x++) {
		try {
			Thread.sleep(100);
			WebDriverWait wait = new WebDriverWait(driver, 5);
			By elemPath = By.id("DB0_0");
			WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
			System.out.println("Approval Button Activated: "+ elem.isDisplayed());
			if (elem.isDisplayed()) {
				return true;
			}else{

				error="Approval button not active";
				return false;
			}
		} catch (Exception e) {
		}
		}
		return false;
	}
    
	public static boolean error() {
		for (int x = 0; x < 20; x++) {
		try {
			
			if(driver.findElements(By.id("errorInformation")).size()>0)		 
			{
//			   System.out.println("The element present");
				Thread.sleep(100);
				WebDriverWait wait = new WebDriverWait(driver, 5);
				String HeaderTxt = driver.findElement(By.xpath("//*[@id=\"emptyFieldsLink\"]")).getText();
				By elemPath = By.id("errorInformation");
				WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
				
				if (elem.isDisplayed()) {
					System.out.println("[Error]"+HeaderTxt);
					error="[Error]"+HeaderTxt;
					return true;
				}else{
					error="";
					return false;
				}
			}else
			{
//			   System.out.println("this element is missing");
			   error="";
			   return false;
			}

		} catch (Exception e) {
			error="";
			return false;
		}
		}
		return false;
	}
    
    public static boolean approveBtn() {
		for (int x = 0; x < 5; x++) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, 5);
			By elemPath = By.id("DB1_0");
			WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
			System.out.println("Approval Button Activated: "+ elem.isDisplayed());
			if (elem.isDisplayed()) {
				return true;
			}else{

				error="Approval button not active";
				return false;
			}
		} catch (Exception e) {
		}
		}
		return false;
	}
    
//    public static boolean approveAEBtn() {
//		for (int x = 0; x < 5; x++) {
//		try {
//			WebDriverWait wait = new WebDriverWait(driver, 5);
//			By elemPath = By.id("DB01_0");
//			WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
//			System.out.println("Approval Button Activated: "+ elem.isDisplayed());
//			if (elem.isDisplayed()) {
//				return true;
//			}else{
//
//				error="Approval button not active";
//				return false;
//			}
//		} catch (Exception e) {
//		}
//		}
//		return false;
//	}
    
    public static WebElement approveADL() {
		for (int x = 0; x < 20; x++) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, 15);
			By elemPath = By.xpath("//a//div[contains(text(), 'Approve')]");
			WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
			wait.until(ExpectedConditions.elementToBeClickable(elem));
			WebElement element = driver.findElement(By.xpath("//a//div[contains(text(), 'Approve')]"));
			System.out.println("RECORD ["+id+"] - REQUEST ID ["+reqID+"] >> [Approved ADL]");
			return element;
		} catch (Exception e) {
			driver.navigate().refresh();
			System.out.println("[WAITING] Approval BUTTON");
			statusElemWait();currentStatus = statusWait();
			if(!currentStatus.trim().contains("Pending ADL Approval")) {
				break;
			}
		}
		}
		return null;
	}
    

    public static WebElement approveAE() {
  		for (int x = 0; x < 20; x++) {
  		try {
  			WebDriverWait wait = new WebDriverWait(driver, 10);
  			By elemPath = By.xpath("//a//div[contains(text(), 'Approve')]");
  			WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
  			wait.until(ExpectedConditions.elementToBeClickable(elem));
  			WebElement element = driver.findElement(By.xpath("//a//div[contains(text(), 'Approve')]"));
  			System.out.println("RECORD ["+id+"] - PROJECT ID ["+projIDStr+"] >> [Approved AE]");
  			return element;
  		} catch (Exception e) {
  			driver.navigate().refresh();
  			System.out.println("[WAITING] Approval BUTTON");
  			statusElemWait();currentStatus = statusWait();
			if(!currentStatus.trim().contains("Pending AE Approval")) {
				break;
			}
  		}
  		}
  		return null;
  	}
    
    public static WebElement moveToSp() {
		for (int x = 0; x < 20; x++) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, 10);
			By elemPath = By.xpath("//a//div[contains(text(), 'Move to SP')]");
			WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
			wait.until(ExpectedConditions.elementToBeClickable(elem));
			WebElement element = driver.findElement(By.xpath("//a//div[contains(text(), 'Move to SP')]"));
			System.out.println("RECORD ["+id+"] - PROJECT ID ["+projIDStr+"] >> [Move to SP]");
			return element;
		} catch (Exception e) {
			driver.navigate().refresh();
			System.out.println("[WAITING] Move to SP BUTTON");
		}
		}
		return null;
	}
    
    public static WebElement projUnsold() {
    	for (int x= 0; x< 20; x++) {
    	try {
    	WebDriverWait wait = new WebDriverWait(driver, 10);
    	By elemPath = By.id("REQD.P.WFM_PROJECT_SOLD_N");
    	WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
    	wait.until(ExpectedConditions.elementToBeClickable(elem));
    	WebElement element = driver.findElement(By.id("REQD.P.WFM_PROJECT_SOLD_N"));
    	System.out.println("RECORD ["+id+"] - PROJECT ID ["+projIDStr+"] >> [Project Sold >> No]");
    	return element;
    	}catch (Exception e) {
    	driver.navigate().refresh();
    	System.out.println("[WAITING] Ready for Approval");
    	}
    	}
    	return null;
    	}
    
    public static WebElement indicatorES(List<String> dataArryVal) {
    	for (int x= 0; x< 20; x++) {
    		try {
    		if(dataList.get(44).toLowerCase().contains("no")) {
            	//Early Staffing Indicator to No
            	WebDriverWait wait = new WebDriverWait(driver, 10);
            	By elemPath = By.id("REQD.P.WFM_EARLY_STAFF_FLAG_N");
            	WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
            	wait.until(ExpectedConditions.elementToBeClickable(elem));
            	WebElement element = driver.findElement(By.id("REQD.P.WFM_EARLY_STAFF_FLAG_N"));
            	System.out.println("RECORD ["+id+"] - PROJECT ID ["+projIDStr+"] >> [Early Staffing >> No]");
            	return element;
            	}
        	else {
            	//Early Staffing Indicator to Yes
            	WebDriverWait wait = new WebDriverWait(driver, 10);
            	By elemPath = By.id("REQD.P.WFM_EARLY_STAFF_FLAG_Y");
            	WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
            	wait.until(ExpectedConditions.elementToBeClickable(elem));
            	WebElement element = driver.findElement(By.id("REQD.P.WFM_EARLY_STAFF_FLAG_Y"));
            	System.out.println("RECORD ["+id+"] - PROJECT ID ["+projIDStr+"] >> [Early Staffing >> Yes]");
            	return element;
            	}
            	}catch (Exception e) {
            	driver.navigate().refresh();
            	System.out.println("[WAITING] Ready for Approval");
            	}
            	}
            	return null;
            	}
    
   
	public static WebElement approveES() {
		for (int x = 0; x < 20; x++) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, 10);
			By elemPath = By.xpath("//*[@id=\"DB0_0\"]");
			WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
			wait.until(ExpectedConditions.elementToBeClickable(elem));
			WebElement element = driver.findElement(By.xpath("//*[@id=\"DB0_0\"]"));
			System.out.println("RECORD ["+id+"] - PROJECT ID ["+projIDStr+"] >> [Approved EARLY STAFFING]");
			return element;
		} catch (Exception e) {
			driver.navigate().refresh();
			System.out.println("[WAITING] Approval BUTTON");
		}
		}
		return null;
	}
    
	public static boolean approveBtnES() {
		for (int x = 0; x < 20; x++) {
		try {
			Thread.sleep(100);
			WebDriverWait wait = new WebDriverWait(driver, 5);
			By elemPath = By.id("DB0_0");
			WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
			System.out.println("Approval Button Activated: "+ elem.isDisplayed());
			if (elem.isDisplayed()) {
				return true;
			}else{

				error="Approval button not active";
				return false;
			}
		} catch (Exception e) {
		}
		}
		return false;
	}
//	populate_projectDetails FUNCTION POPULATES PPMC FIELDS WITH TEMPLATES DATA
	public static void populate_projectDetails(String projIDVal, String fteDateVal, List<String> dataArryVal) throws Throwable {
		for (int x = 0; x < 10; x++) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, 5);
			projID().clear();
			projID().sendKeys(projIDVal);
			projID().sendKeys(Keys.TAB);projID().sendKeys(Keys.TAB);	
			invalidataHandler(projIDVal);
			if (!error.isEmpty()) {break;}
					
			forecastEdit().click();
			
			StringTokenizer tokenizedData = new StringTokenizer(fteDateVal, ",");
			int forecastLine = tokenizedData.countTokens(); 
			String array_dataForecast[] = new String[forecastLine];
			
			for (int line = 0; line < forecastLine; line++) {
				array_dataForecast[line] = tokenizedData.nextToken(); 
				String ftevalues = array_dataForecast[line];
				StringTokenizer dataPerLine = new StringTokenizer(ftevalues, "#");
				int dataLine = dataPerLine.countTokens();
				String array_dataLine[] = new String[dataLine];
				
				By addButton = By.id("BT_ADD_ROW_P_1");
				WebElement fteaddPath = wait.until(ExpectedConditions.presenceOfElementLocated(addButton));
				wait.until(ExpectedConditions.elementToBeClickable(fteaddPath)).click();
				By enddateCol = By.xpath("//span[contains(text(), 'FTE End Date')]");
				WebElement enddateElem = wait.until(ExpectedConditions.presenceOfElementLocated(enddateCol));
				wait.until(ExpectedConditions.elementToBeClickable(enddateElem)).click();

				
				for (int dataCount = 0; dataCount < dataLine; dataCount++) { 
					array_dataLine[dataCount] = dataPerLine.nextToken();
					By fieldPath = By.id("49025_COL_"+dataCount);
					WebElement field = wait.until(ExpectedConditions.presenceOfElementLocated(fieldPath));
					wait.until(ExpectedConditions.elementToBeClickable(field)).clear();
					field.sendKeys(array_dataLine[dataCount]);
					field.sendKeys(Keys.TAB);
					Thread.sleep(1000);
					alertHandler();
					if (!error.isEmpty()) {
						System.out.println("[ERROR]:"+error);
						break;
					}
				}
			}
			Thread.sleep(100);
			
			
			
			//Populate create fields
			for (int ctr = 1; ctr <= 34; ctr++) {
				 String ctrStr=Integer.toString(ctr);
				 System.out.print("."); 
		         System.out.flush();
				 try (InputStream input = new FileInputStream("src/main/resources/properties/elements.properties")) {
			            Properties prop = new Properties();
			            prop.load(input);
			            System.out.println(ctr+"|"+prop.getProperty(ctrStr)+"|"+dataList.get(ctr+12));
			            
						if (ctr==26){
							if(dataList.get(ctr+12).toLowerCase().contains("no")) {			            	
				            	By elemPath = By.id("REQD.P.WFM_LOCTAION_CONTRACTUALLY_N");
				            	WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
				            	wait.until(ExpectedConditions.elementToBeClickable(elem));
			            		WebElement element = driver.findElement(By.id("REQD.P.WFM_LOCTAION_CONTRACTUALLY_N"));
								 element.click();
								 System.out.println(element.isSelected());
								 if(!element.isSelected()){
									 element.click();
									 System.out.println(element.isSelected());
								 }
			            	}else 
			            	{
								By elemPath = By.id("REQD.P.WFM_LOCTAION_CONTRACTUALLY_Y");
				            	WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
				            	wait.until(ExpectedConditions.elementToBeClickable(elem));
			            		WebElement element = driver.findElement(By.id("REQD.P.WFM_LOCTAION_CONTRACTUALLY_Y"));
								 element.click();
								 System.out.println(element.isSelected());
								 if(!element.isSelected()){
									 element.click();
									 System.out.println(element.isSelected());
								 }
			            	}
						}
						
						if (ctr==33) {
			            	System.out.println("Client Interview");
							Select DropDown = new Select(driver.findElement(By.id("REQD.P.CLIENT_INTRW")));

							DropDown.selectByIndex(0);
							DropDown.selectByVisibleText(dataList.get(ctr+12));
						}
						
												
				            By fieldPath = By.id(prop.getProperty(ctrStr));
							wait.until(ExpectedConditions.presenceOfElementLocated(fieldPath));
							wait.until(ExpectedConditions.elementToBeClickable(fieldPath));
							WebElement field = wait.until(ExpectedConditions.presenceOfElementLocated(fieldPath));
							Thread.sleep(1000);
							if (ctr==8) {
								field.clear();
							}
			
							field.sendKeys(dataList.get(ctr+12).trim());

							field.sendKeys(Keys.TAB);
						//ALERT POP UP MESSAGE Apply Changes
						alertHandler();
						if (!error.isEmpty()) {
							System.out.println("[ERROR]:"+error);
							break;
						}
						Thread.sleep(1000);
						//PPMC ERROR HANDLER
						invalidataHandler(dataList.get(ctr+12));
						Thread.sleep(1000);
						//Using Default Values if data Fails
						if (!error.isEmpty()) {
							if(ctr==5) {
								error="";
								System.out.println("Using Default Value for Requested Resource");
								field.sendKeys("");

								field.sendKeys(Keys.TAB);
							}else if (ctr==23) {
								error="";
								System.out.println("Using Default Value for Primary Skill");
								field.sendKeys("DXC-ITIL GENERAL");

								field.sendKeys(Keys.TAB);
							}
							else if (ctr==25) {
								error="";
								System.out.println("Using Default Value for Secondary Skill");
								field.sendKeys("DXC-MICROSOFT OFFICE SUITE");

								field.sendKeys(Keys.TAB);
							}
							else {
								System.out.println("[Data Handler ERROR ]:"+error);
								break;
							}
						}
						
			        } catch (Exception e) {
			        	
			        }
			          
			}
				
			break;
			
		} catch (Exception e) {
		}
		
	  }
		
	}

	//STEP 1
	public static void getURL() {
		for (int x = 0; x < 5; x++) {
			try {
				System.setProperty("webdriver.chrome.driver", "chromedriver.exe");
				DesiredCapabilities capabilities;	    
				capabilities = DesiredCapabilities.chrome();
				ChromeOptions options = new ChromeOptions(); 
//						options.addArguments("--headless");	    
			    	    options.addArguments("--disable-extensions");   
			    	    options.addArguments("--disable-gpu");   
			    	    options.addArguments("--no-sandbox");   
			    	    options.addArguments("--window-size=1600,900");   
				capabilities.setCapability(ChromeOptions.CAPABILITY, options);
				options.merge(capabilities);
				driver = new ChromeDriver(options);
		    	driver.get("https://ppmcpro.itcs.houston.dxccorp.net/");
		    	driver.manage().timeouts().implicitlyWait(15, TimeUnit.SECONDS);
		    	driver.manage().window().maximize();
				break;
			} catch (Exception e) {
				driver.navigate().refresh();
				System.out.println("[ERROR] INTERUPTION OCCURED");
			}	
		}
	}

	
	//STEP 2
	public static void login() {
		for (int x = 0; x < 20; x++) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, 30);
			wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[contains(@name,'username')]")));
            WebElement username = driver.findElement(By.name("username"));
            WebElement password = driver.findElement(By.name("password"));
            WebElement loginBtn = driver.findElement(By.id("okta-signin-submit"));
        	username.sendKeys("ernest.nebre");
	    	password.sendKeys("!15Stereorama");

	    	loginBtn.click();
			break;
		} catch (Exception e) {
			loginWait();
		}
		}
	}

	//STEP 3
    public static WebElement search_menu() {
		for (int x = 0; x < 20; x++) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, 5);
			By elemPath = By.xpath("//a[contains(@href,'#MENU_CREATE')]");
			WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
			wait.until(ExpectedConditions.elementToBeClickable(elem));
			WebElement element = driver.findElement(By.xpath("//a[contains(@href,'#MENU_CREATE')]"));
			return element;
		} catch (Exception e) {
			driver.navigate().refresh();
			System.out.println("[WAITING] SEARCH CREATE");
		}
	}
		return null;
	}
  
    //STEP 4
	public static WebElement request_submenu() {
		for (int x = 0; x < 5; x++) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, 5);
			By elemPath = By.xpath("//a[contains(@href,'#CREATE_REQUEST')]");
			WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
			wait.until(ExpectedConditions.elementToBeClickable(elem));
			WebElement element = driver.findElement(By.xpath("//a[contains(@href,'#CREATE_REQUEST')]"));
			return element;
		} catch (Exception e) {
			driver.navigate().refresh();
			System.out.println("[WAITING] REQUEST SUB-MENU");
		}
	  }
		return null;
	}
	
	public static WebElement submenu_wfm() {
		for (int x = 0; x < 5; x++) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, 5);
			By elemPath = By.id("CREATE_REQUEST:@WFM Position Level Management");
			WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
			wait.until(ExpectedConditions.elementToBeClickable(elem));
			return elem;
		} catch (Exception e) {
			driver.navigate().refresh();
			System.out.println("[WAITING] WFM Position Level Management sub-menu");
		}
	  }
		return null;
	}

	public static void submit() {
		for (int x = 0; x < 2; x++) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, 10);
			By elemPath = By.id("submit");
			WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
			wait.until(ExpectedConditions.elementToBeClickable(elem));
			elem.click();
		} catch (Exception e) {
//			driver.navigate().refresh();
			System.out.println("[WAITING] SUBMIT BUTTON");
		}
	  }
	}
	
	private static void ongoingUpate() {
		for (int x = 0; x < 20; x++) {
		try {
			Thread.sleep(2000);
			endRec = Instant.now();timeElapsedRec = Duration.between(startRec, endRec);
			 if (!error.isEmpty()) {
						 update.setString(1, "[ERROR] Invalid data: " + error);
						 reqID = "SKIPPED";
						 currentStatus = "[DATA ISSUE] "+error;
				}else {
				Thread.sleep(1000);
				currentStatus = statusWait(); 
				Thread.sleep(1000);
				reqID = getReqIDt().getAttribute("innerText");
				update.setString(1, "DONE");	
			 }

			 update.setString(2, reqID);
	         update.setString(3, currentStatus);
	         update.setLong(4, timeElapsedRec.toMillis());
	         if(positionId()) {
		         update.setString(5, positionID);
	         }
	         update.setString(6, thread);
	         update.setString(7, id);
	         Thread.sleep(2000);
	         break;
		} catch (Exception e) {
			System.out.println("[RETRY] UPDATE FAILED");
		}
		}		
	}
	
//	
	private static void ongoingUpateWithReqId() {
		for (int x = 0; x < 20; x++) {
		try {
			Thread.sleep(2000);
			endRec = Instant.now();timeElapsedRec = Duration.between(startRec, endRec);
			 if (!error.isEmpty()) {
				 update.setString(1, error);
				}else {
				update.setString(1, "DONE");	
			 }

			 reqID = getReqIDt().getAttribute("innerText");
			 update.setString(2, reqID);
	         update.setString(3, currentStatus);
	         update.setLong(4, timeElapsedRec.toMillis());
	         if(positionId()) {
		         update.setString(5, positionID);
	         }
	         update.setString(6, thread);
	         update.setString(7, id);
	         Thread.sleep(2000);
	         break;
		} catch (Exception e) {
			System.out.println("[RETRY] UPDATE FAILED");
		}
		}		
	}
	public static boolean positionId() {
		for (int x = 0; x < 20; x++) {
		try {
			Thread.sleep(100);
			WebDriverWait wait = new WebDriverWait(driver, 5);
			String HeaderTxt = driver.findElement(By.xpath("//*[@id=\"DRIVEN_P_7\"]")).getText();
			By elemPath = By.id("DRIVEN_P_7");
			WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
			if (elem.isDisplayed()) {
				System.out.println("[Position ID]"+HeaderTxt);
				positionID = HeaderTxt;
				return true;
			}else{
				return false;
			}
		} catch (Exception e) {
		}
		}
		return false;
	}

    public static  void locCons(List<String> dataArryVal) {
 	   try {
 		if(dataList.get(38).toLowerCase().contains("no")) {
         	//Early Staffing Indicator to No
         	WebElement element = driver.findElement(By.id("REQD.P.WFM_LOCTAION_CONTRACTUALLY_N"));
            	System.out.println("RECORD ["+id+"] - PROJECT ID ["+projIDStr+"] >> [Location Constrained >> No]");
            	element.click();
            	//Clicking Client interview as well
            	System.out.println("Client Interview");
				Select DropDown = new Select(driver.findElement(By.id("REQD.P.CLIENT_INTRW")));
				DropDown.selectByIndex(0);
				DropDown.selectByVisibleText(dataList.get(45));
				
         	}
     	else {
         	//Early Staffing Indicator to Yes
         	WebElement element = driver.findElement(By.id("REQD.P.WFM_LOCTAION_CONTRACTUALLY_Y"));
         	System.out.println("RECORD ["+id+"] - PROJECT ID ["+projIDStr+"] >> [Location Constrained >> Yes]");
         	element.click();
         	//Clicking Client interview as well
            	System.out.println("Client Interview");
				Select DropDown = new Select(driver.findElement(By.id("REQD.P.CLIENT_INTRW")));
				DropDown.selectByIndex(0);
				DropDown.selectByVisibleText(dataList.get(45));
         	            	
         	}
 	   }catch(Exception e) {
 		   System.out.println("Contractually constraint button not found/Client interview drop down not found");
 	   }
         	
         	}
    
	public static WebElement reqID() {
		for (int x = 0; x < 20; x++) {
			System.out.println("Reached");
		try {
			WebDriverWait wait = new WebDriverWait(driver, 5);
			By elemPath = By.xpath("//*[contains(@href,'RequestDetail.jsp?REQUEST_ID=')]");
			
			WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
			wait.until(ExpectedConditions.elementToBeClickable(elem));
			WebElement element = driver.findElement(By.xpath("//*[contains(@href,'RequestDetail.jsp?REQUEST_ID=')]"));
			return element;
		} catch (Exception e) {
			driver.navigate().refresh();
			System.out.println("[WAITING] REQUEST ID LINK");
		}
		}
		return null;
	}
	
	
	
	public static WebElement completePLM() {
		for (int x = 0; x < 20; x++) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, 5);
//			
//			//Default Value for Primary Skill and Secondary skill
//			WebElement searchTextBoxPrimarySkill= driver.findElement(By.id("REQD.P.PRIMARY_SKILLAC_TF"));
//			WebElement searchTextBoxSecondarySkill= driver.findElement(By.id("REQD.P.SECONDARY_SKILLAC_TF"));
//			
//			// retrieving html attribute value using getAttribute() method
//			String typeValue=searchTextBoxPrimarySkill.getAttribute("value");
//			String typeValueSecondary=searchTextBoxSecondarySkill.getAttribute("value");
//			System.out.println("Value of type attribute: "+typeValue);
//			
//			if(typeValue.isEmpty())
//			{
//				System.out.println("Using Default Value for Primary Skill");
//				searchTextBoxPrimarySkill.sendKeys("ITIL - General");
//
//				searchTextBoxPrimarySkill.sendKeys(Keys.TAB);
//				
//				System.out.println("Using Default Value for Secondary Skill");
//				searchTextBoxSecondarySkill.sendKeys("Tools - General Delivery - Other");
//
//				searchTextBoxSecondarySkill.sendKeys(Keys.TAB);
//			}
//			
//			if(typeValueSecondary.isEmpty())
//			{
//				
//				System.out.println("Using Default Value for Secondary Skill");
//				searchTextBoxSecondarySkill.sendKeys("Tools - General Delivery - Other");
//
//				searchTextBoxSecondarySkill.sendKeys(Keys.TAB);
//			}
			
			By elemPath = By.xpath("//a//div[contains(text(), 'Complete PLM')]");
			WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
			wait.until(ExpectedConditions.elementToBeClickable(elem));
			WebElement element = driver.findElement(By.xpath("//a//div[contains(text(), 'Complete PLM')]"));
			System.out.println("RECORD ["+id+"] - PROJECT ID ["+projIDStr+"] >> [COMPLETE PLM]");
			return element;
		} catch (Exception e) {
			driver.navigate().refresh();
			System.out.println("[WAITING] COMPLETE PLM BUTTON");
		}
		}
		return null;
	}
	
	public static WebElement releaseForAppvl() {
		for (int x = 0; x < 20; x++) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, 10);
			By elemPath = By.xpath("//a//div[contains(text(), 'Release for Approval')]");
			WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
			wait.until(ExpectedConditions.elementToBeClickable(elem));
			WebElement element = driver.findElement(By.xpath("//a//div[contains(text(), 'Release for Approval')]"));
			System.out.println("RECORD ["+id+"] - PROJECT ID ["+projIDStr+"] >> [RELEASE FOR APPROVAL]");
			return element;
		} catch (Exception e) {
			driver.navigate().refresh();
			System.out.println("[WAITING] RELEASE FOR APPROVAL BUTTON");
		}
		}
		return null;
	}
	
	
	public static WebElement getReqIDt() {
		for (int x = 0; x < 20; x++) { 
		try {
			WebDriverWait wait = new WebDriverWait(driver, 5);
			By elemPath = By.id("DRIVEN_REQUEST_ID");
			wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
			WebElement element = driver.findElement(By.id("DRIVEN_REQUEST_ID"));
			return element;
		} catch (Exception e) {
			driver.navigate().refresh();
			System.out.println("[WAITING] REQUEST ID");
		}
		}
		return null;
	}
	
	public static WebElement projID() {
		for (int x = 0; x < 20; x++) { 
		try {
			Thread.sleep(1000);
			WebDriverWait wait = new WebDriverWait(driver, 5);
			By elemPath = By.id("REQD.P.WFM_PROJECT_IDAC_TF");
			WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
			wait.until(ExpectedConditions.elementToBeClickable(elem));
			WebElement element = driver.findElement(By.id("REQD.P.WFM_PROJECT_IDAC_TF"));
			return element;
		} catch (Exception e) {
			System.out.println("[WAITING] PROJECT ID FIELD");
		}
		}
		return null;
	}
	
	private static String projmanagerStr() {
		for (int x = 0; x < 20; x++) {
			try {
				WebDriverWait wait = new WebDriverWait(driver, 5);
				By elemPath = By.id("DRIVEN_P_10");// project manager 
				WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
				String elemText = wait.until(ExpectedConditions.elementToBeClickable(elem)).getText().toString();
				return elemText;
				} catch (Exception e) {
					System.out.println("[WAITING] PROJECT MANAGER VALUE");
			}
		}
		return null;
	}
	
	public static WebElement forecastEdit() throws Throwable {
		for (int x = 0; x < 20; x++) { 
		try {
			WebDriverWait wait = new WebDriverWait(driver, 5);
			By elemPath = By.id("BT_EDIT_P_1");
			WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
			wait.until(ExpectedConditions.elementToBeClickable(elem));
			WebElement element = driver.findElement(By.id("BT_EDIT_P_1"));
			return element;
		} catch (Exception e) {
			System.out.println("[WAITING] EDIT BUTTON");
			focastneededExpand().click();Thread.sleep(1000);	
			expandAll();Thread.sleep(1000);	
		}
		}
		return null;
	}
	
	public static WebElement focastneededExpand() {
		for (int x = 0; x < 20; x++) { 
		try {
			WebDriverWait wait = new WebDriverWait(driver, 5);			
			By elemPath = By.id("IMAGE_EC_REQUEST_TC_REQD.P.WFM_FORECAST_NEEDED");
			WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
			wait.until(ExpectedConditions.elementToBeClickable(elem));
			WebElement element = driver.findElement(By.id("IMAGE_EC_REQUEST_TC_REQD.P.WFM_FORECAST_NEEDED"));
			return element;
		} catch (Exception e) {
			System.out.println("[WAITING] CAN NOT EXPAND");
			expandAll();
		}
		}
		return null;
	}

	
	public static void expandAll() {
		try {
			List<WebElement> collapseExpand = driver.findElements(By.xpath("//img[contains(@alt,'Expand')]"));
			if (collapseExpand.size() > 0) {
				for (WebElement elem: collapseExpand) {
					elem.click();
				}}else {};
		} catch (Exception e) {
		}
	}
	
	private static String statusWait() {
		for (int x = 0; x < 20; x++) {
			try {
				WebDriverWait wait = new WebDriverWait(driver, 30);
				By elemPath = By.xpath("//*[@id=\"requestStatus\"]/span/a/span"); 
				WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
				String elemText = wait.until(ExpectedConditions.elementToBeClickable(elem)).getText().toString();
				return elemText;
				} catch (Exception e) {
					driver.navigate().refresh();
					System.out.println("[WAITING] STATUS");
			}
		}
		return null;
	}
	
	public static void statusElemWait() {
		for (int x = 0; x < 20; x++) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, 15);
			wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[contains(@id,'requestStatus')]")));
		} catch (Exception e) {
			driver.navigate().refresh();
			System.out.println("[WAITING] STATUS ELEMENT");
		}
		}
	}
	
	public static void  loginWait() {
		for (int x = 0; x < 20; x++) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, 15);
			wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[contains(@name,'USER')]")));
		} catch (Exception e) {
			driver.navigate().refresh();
			System.out.println("[WAITING] LOGIN FIELDS");
		}
		}
	}
	
	public static void processingWait() {
		for (int x = 0; x < 20; x++) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, 30);
			By loader = By.xpath("//img[contains(@src,'Loading.gif')]");
			By waiting = By.xpath("//*[@id='page-min-width-div']/div[5]/div/form/div[3]/span[1]/img");
			wait.until(ExpectedConditions.invisibilityOfElementLocated(loader));
			wait.until(ExpectedConditions.invisibilityOfElementLocated(waiting));
			ExpectedCondition<Boolean> condition = new ExpectedCondition<Boolean>() {
				public Boolean apply(WebDriver driver) {return ((JavascriptExecutor) driver).executeScript("return document.readyState").toString().equals("complete");}
			};
			wait.until(condition);
		} catch (Exception e) {
		}
	}
	}	
		public static void alertHandler() {
			//Too many result rows were returned for SQL in Rule 90
			for (int x = 0; x < 3; x++) {
			try {
				WebDriverWait wait = new WebDriverWait(driver, 1);
				Alert alert = wait.until(ExpectedConditions.alertIsPresent());
				String alertmessage = alert.getText();
				alert.accept();
				System.out.println("[ALERT MESSAGE]:"+alertmessage);
				if(alertmessage.contains("Too many result rows were returned for SQL")) {
					System.out.println("SKIPPING ALERT MESSAGE: "+alertmessage.contains("Too many result rows were returned for SQL"));
					error = "";
				} else {
				    error=alertmessage;
				}
				return;
			} catch (Exception e) {
			}
		   }
		
	}
		
		public static void alertHandlermenu() {
			for (int x = 0; x < 3; x++) {
			try {
				WebDriverWait wait = new WebDriverWait(driver, 1);
				Alert alert = wait.until(ExpectedConditions.alertIsPresent());
				alert.accept();
				break;
			} catch (Exception e) {
			}
		   }
		
	}
	
		
	
		public static void invalidataHandler(String dataIssue) {
			try {
				Thread.sleep(3000);
				List<WebElement> iframe=driver.findElements(By.tagName("iframe"));
				 for(int i=0; i<=iframe.size(); i++){
					 if (iframe.get(i).getAttribute("id").equals("autoCompleteDialogIF")){
						 	Thread.sleep(1000);
						    iframe.get(i).sendKeys(Keys.ESCAPE);
					        Thread.sleep(1000);
					      error=dataIssue;
//					     System.out.println("RECORD ["+id+"] - PROJECT ID ["+projIDStr+"] >> [DATA ISSUE]");
//						 break;
					 }
				 }
			} catch (Exception e) {
			}
		 }
	
		
	
}

