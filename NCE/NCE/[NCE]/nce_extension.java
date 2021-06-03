
package myProject;

import static java.lang.System.exit;

import java.awt.RenderingHints.Key;
import java.awt.Robot;
import java.awt.event.KeyEvent;
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
import org.openqa.selenium.support.ui.Select;

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
import org.openqa.selenium.support.ui.WebDriverWait;


public class nce_extension {	
	protected static String thread;
    protected static String query;
    protected static String rsQuery;
    protected static List<String> dataList = new ArrayList<String>();	
    protected static int n;
    protected static WebDriver driver;
    protected static String id;
	protected static String requestIdStr;
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
	
	static String errorDesc = null;
	
    public static void main(String[] args) throws Exception { 
    	try {getURL();} catch (Throwable e) {System.exit(0);}  	
    	try {run(args[0]);} catch (Throwable e) {System.exit(0);}
    }
   
    @SuppressWarnings("null")
	public static void run(String... args) throws Throwable {
        if (args[0].length() > 0 ) { 
        	login();
        	search_menu().click();Thread.sleep(500);
        	request_submenu().click();Thread.sleep(500);
        	
        	thread = args[0].toString().trim();	
        	connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/"+"PPMC", "postgres", "admin");
			count = connection.createStatement();
			query="SELECT COUNT(*) FROM public.nce_extension WHERE key_status IN ('PENDING','ONGOING') AND parallel_key='"+thread+"';";
			rsQuery="SELECT * FROM public.nce_extension WHERE key_status IN ('PENDING','ONGOING') AND parallel_key='"+thread+"' ORDER BY id;";
	        countRs = count.executeQuery(query);
	        	countRs.next();	
	            rowCount = countRs.getInt(1);
	            start = Instant.now();
	            System.out.println("");
	            System.out.println("[THREAD "+ thread +"]: PROCESSING...");
	            System.out.println("TIME START: "+start);
	            System.out.println("TOTAL 'Extension' DEMAND(S) ["+rowCount+"]");
	            if (rowCount==0) {driver.quit();System.exit(0);}
	            for (int x = 0; x < rowCount; x++) {
	    	        try {
	    	            if (connection != null) {
	   	                stmt = connection.createStatement();
	   	                update = connection.prepareStatement("UPDATE nce_extension SET key_status = ?, request_id = ?, status = ?, duration = ?, position_id = ? WHERE parallel_key=? AND id = ?;");
	   	                rs = stmt.executeQuery(rsQuery);	
	   			                while (rs.next()) {
	   			                	startRec= Instant.now();
	   			                	System.out.println("");
	   			                	error="";
	   			                	requestIdStr=rs.getString("request_id".trim()); 			                   	 
				                	id=rs.getString("id".trim());
				                	fteDateStr=rs.getString("fte".trim());
				                	searchRequestId(requestIdStr.trim());
				                	System.out.println("RECORD ["+id+"] - REQUEST ID ["+requestIdStr+"] >> PROCESSING");
				                	
				                	//CHECK IF I HAVE ACCES TO THE ACCOUNT
				                	if(!accessError()) {
				                		//STATUS WAIT
				                		statusElemWait();currentStatus = statusWait();
				                		System.out.println("RECORD ["+id+"] - REQUEST ID ["+requestIdStr+"] >> " + currentStatus);

						                	dataList.clear();
						                	for (int count=1; count <= 42;count++) {
						                		dataList.add(rs.getString(count));
						                	}
						                	
					                	      	statusElemWait();
					                	      	//STATUS WAIT
							                	currentStatus = statusWait();
							                	// STATUS: PLM APPROVED OR STAFFING APPROVED THEN CLICK REDEFINED BUTTON
							                	System.out.println("REQUEST STATUS: "+currentStatus);
							                	if (currentStatus.trim().contains("Staffing Approved") || currentStatus.trim().contains("PLM Approved")) {
							                		reworkOnPLM();
													if (reworkOnPLM()) {
														reworkPLM().click();	
														System.out.println("RECORD ["+id+"] - REQUEST ID ["+requestIdStr+"] >> REWORK ON PLM");
													}else {
														System.out.println("RECORD ["+id+"] - REQUEST ID ["+requestIdStr+"] >> ISSUE ON CFREDENTIAL");
														error="CREDENTIAL";break;
													}
													
												}
							                	//MAIN METHOD IF STATUS IS IN PLANNING OR POSITION TO SP MOVE TO MAIN METHOD
							                	statusElemWait();
					                	      	//STATUS WAIT
							                	currentStatus = statusWait();
							                	System.out.println("REQUEST STATUS: "+currentStatus);
							                	if(currentStatus.trim().contains("In Planning") || currentStatus.trim().contains("Position Created in SP")) {
							                		populate_projectDetails(requestIdStr, fteDateStr, dataList);
							                	}
							                	//STATUS WAIT
							                	statusElemWait();currentStatus = statusWait();
							                	Thread.sleep(100);
							                	//CHECK IF STATUS AFTER MAIN METHOD | CANCELLED
							                	if(currentStatus.trim().contains("Cancelled")) {
							                		System.out.println("RECORD ["+id+"] - REQUEST ID ["+requestIdStr+"] >> " + currentStatus);
							                		  error="[Error] Cancelled";
							                	}
							                	//CHECK IF STATUS AFTER MAIN METHOD | CLOSED
							                	if( currentStatus.trim().contains("Closed")) {
							                		System.out.println("RECORD ["+id+"] - REQUEST ID ["+requestIdStr+"] >> " + currentStatus);
							                		  error="[Error] Closed";
							                	}
							                	//CHECK IF STATUS AFTER MAIN METHOD | Position Created in SP
							                	if(currentStatus.trim().contains("Position Created in SP")) {
							                		System.out.println("RECORD ["+id+"] - REQUEST ID ["+requestIdStr+"] >> " + currentStatus);
							                		  error="[Error] Issue In Credentials/ Not PM in the Account";
							                	}
							                	//CHECK IF STATUS AFTER MAIN METHOD | In Planning
							                	if(currentStatus.trim().contains("In Planning")) {
							                		System.out.println("RECORD ["+id+"] - REQUEST ID ["+requestIdStr+"] >> " + currentStatus);
							                		completePLM().click();
							                	}
				                	
						                	
						                	error();
						                	
						                	if (!error.isEmpty()) {               		
												System.out.println("RECORD ["+id+"] - PROJECT ID ["+requestIdStr+"] >> SKIPPED");
						                	}else {
												  statusElemWait();currentStatus = statusWait();
												  Thread.sleep(500);
												  
												  //CHECK IF IN PLANNING
												  if (currentStatus.trim().contains("In Planning")){
												  System.out.println("RECORD ["+id+"] - REQUEST ID  ["+requestIdStr+"] >> COMPLETE PLM NOT COMPLETE | Project FTE ISSUE");  
												  error="[Error] Project FTE ISSUE ";
												  }
												  
												  //STATUS: READY FOR APPROVAL (With Data issue encountered)
												  statusElemWait();currentStatus = statusWait();
												  Thread.sleep(500);
												  if(currentStatus.trim().contains("Ready for Approval")) {
													  System.out.println("RECORD ["+id+"] - REQUEST ID ["+requestIdStr+"] >> " + currentStatus);
													  String HeaderTxt = driver.findElement(By.xpath("//*[@id=\"DRIVEN_CH_41\"]")).getText();
													  System.out.println("No Release for Approval Error: "+HeaderTxt.isEmpty());
													  if(HeaderTxt.isEmpty()) {
															releaseForAppvl().click();
													  } else {
														  releaseForAppvl().click();
														  error="[Error]" + HeaderTxt;
													  }
												  }
												  
												  statusElemWait();currentStatus = statusWait();
												  Thread.sleep(500);
												  if(currentStatus.trim().contains("Ready for Approval")) {
													  System.out.println("RECORD ["+id+"] - REQUEST ID ["+requestIdStr+"] >> " + currentStatus);
													  String HeaderTxt = driver.findElement(By.xpath("//*[@id=\"DRIVEN_CH_41\"]")).getText();
													  System.out.println("No Release for Approval Error: "+HeaderTxt.isEmpty());
													  if(HeaderTxt.isEmpty()) {
															releaseForAppvl().click();
													  } else {
														  releaseForAppvl().click();
														  error="[Error]" + HeaderTxt;
													  }
												  }
												  
							                	  //STATUS: ES APPROVAL
//							                	  do {statusElemWait();currentStatus = statusWait();
//						                		  System.out.println("RECORD ["+id+"] - REQUEST ID ["+requestIdStr+"] >> " + currentStatus);
//						                		  if(approveBtnES() && currentStatus.trim().contains("Pending ES Approval")) {
//						                			  approveES().click();
//						                		  } else {
//						                			  error="[Error] Approval Button Not Activated"; 
//						                		  }
//							                	} while (currentStatus.trim().contains("Pending ES Approval"));	

						                		  //STATUS: PENDING ADL APPROVAL 
												  statusElemWait();currentStatus = statusWait();
												  Thread.sleep(500);
												  if(currentStatus.trim().contains("Pending ADL Approval")) {
													  System.out.println("RECORD ["+id+"] - REQUEST ID ["+requestIdStr+"] >>  APPROVAL RELEASED");
							                		  System.out.println("RECORD ["+id+"] - REQUEST ID ["+requestIdStr+"] >> " + currentStatus);
							                		  if(approveBtn() && currentStatus.trim().contains("Pending ADL Approval")) {
							                			  error="";
								                		  approveADL().click();
							                		  } else {
							                			  error="[Error] Pending ADL Approval Approval Button Not Activated"; 
							                		  }
												  }
								                	// STATUS: PENDING AE APPROVAL
							                	  statusElemWait();currentStatus = statusWait();
							                	  Thread.sleep(500);
							                	  if(currentStatus.trim().contains("Pending AE Approval")) {
							                		  System.out.println("RECORD ["+id+"] - REQUEST ID ["+requestIdStr+"] >>  APPROVAL RELEASED");
							                		  System.out.println("RECORD ["+id+"] - REQUEST ID ["+requestIdStr+"] >> " + currentStatus);
							                		  if(approveAEBtn() && currentStatus.trim().contains("Pending AE Approval")) {
							                			  error="";
							                			  approveAE().click();
							                		  } else {
							                			  error="[Error] Pending AE Approval Approval Button Not Activated"; 
							                		  }
							                	  }							         		                	  
								                	
								                	//STATUS: PENDING DEMAND APPROVAL
							                	  statusElemWait();currentStatus = statusWait();
							                	  Thread.sleep(500);
							                	  if(currentStatus.trim().contains("Pending Dmd Planner Approval")) {
							                		  System.out.println("RECORD ["+id+"] - REQUEST ID ["+requestIdStr+"] >> " + currentStatus);
							                		  if(approveBtnDmdPlanner() && currentStatus.trim().contains("Pending Dmd Planner Approval")) {
							                			  error="";
							                			  approveADLDmdPlanner().click();
							                		  } else {
							                			  error="[Error] Pending Dmd Planner Approval Approval Button Not Activated"; 
							                		  }
							                	  }
								                	//STATUS: PLM APPROVAL
							                	  statusElemWait();currentStatus = statusWait();
							                	  Thread.sleep(500);
							                	  if(currentStatus.trim().contains("PLM Approved")) {
							                		  System.out.println("RECORD ["+id+"] - REQUEST ID ["+requestIdStr+"] >> " + currentStatus);
							                		  String HeaderTxt = driver.findElement(By.xpath("//*[@id=\"DRIVEN_CH_41\"]")).getText();
													  System.out.println("No PLM Approved Error: "+HeaderTxt.isEmpty());
													  if(HeaderTxt.isEmpty()) {
														  error="";
														  moveToSp().click();
													  } else {
														  error="[Error]" + HeaderTxt;
													  }
							                	  }
								                	//STATUS: STAFFING APPROVAL
							                	  statusElemWait();currentStatus = statusWait();
							                	  Thread.sleep(500);
							                	  if(currentStatus.trim().contains("Staffing Approved")) {
							                		  System.out.println("RECORD ["+id+"] - REQUEST ID ["+requestIdStr+"] >> " + currentStatus);
							                		  String HeaderTxt = driver.findElement(By.xpath("//*[@id=\"DRIVEN_CH_41\"]")).getText();
													  System.out.println("No Staffing Approved Error: "+HeaderTxt.isEmpty());
													  if(HeaderTxt.isEmpty()) {
														  error="";
														  moveToSp().click();
													  } else {
														  error="[Error]" + HeaderTxt;
													  }
							                	  }
								                	
								                  //STATUS: POSITION CREATED IN SP
							                	  statusElemWait();currentStatus = statusWait();
							                	  Thread.sleep(500);
							                	  if(currentStatus.trim().contains("Position Created in SP")) {
													  error=currentStatus; 
												  }
					                	  }
				                	} else {
				                		//CHECK IF I HAVE ACCES TO THE ACCOUNT
				                		System.out.println("Access Error: "+accessError());
				                		error="[Error] Access Error";
				                	}
				                	  
				                	  ongoingUpate();
				                	  update.executeUpdate();	
									  System.out.println("RECORD ["+id+"] - REQUEST ID  ["+requestIdStr+"] >> SUCCESSFUL");
									  
				                	search_menu().click(); Thread.sleep(500);
				                    request_submenu().click();Thread.sleep(500);									 	
				                    startRec=null;endRec=null;
				                	alertHandlermenu();
									
	   			                }
	    	               break;
	    	            } else {
	    	            	System.out.println("No connection.");
	    	            }
	    	        } catch (Exception e) {
	    	            e.printStackTrace();
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
   
	
	public static void populate_projectDetails(String reqIDVal, String fteDateVal, List<String> dataArryVal) throws Throwable {
		for (int x = 0; x < 10; x++) {
		try {
					WebDriverWait wait = new WebDriverWait(driver, 5);
			//Check ProjId with value?
			if (!reqIDVal.isEmpty()) {
//					invalidataHandler();
					statusElemWait();currentStatus = statusWait(); 
					if (currentStatus.trim().contains("Position Created in SP")) {
						reDefineCheck();
						if (reDefineCheck()) {
							reDefine().click();	
							System.out.println("RECORD ["+id+"] - REQUEST ID ["+requestIdStr+"] >> REDEFINED");
						}else {
							System.out.println("RECORD ["+id+"] - REQUEST ID ["+requestIdStr+"] >> ISSUE ON CFREDENTIAL");
							error="CREDENTIAL";break;
						}
						
					}
					
					statusElemWait();currentStatus = statusWait();

					Thread.sleep(100);
                	  if (currentStatus.trim().contains("Cancelled")){
                		  System.out.println("RECORD ["+id+"] - REQUEST ID ["+requestIdStr+"] >> " + currentStatus);
                		  error="[Error] Cancelled Request ID";
                		  break;
                	  }
                	  if (currentStatus.trim().contains("Closed")){
                		  System.out.println("RECORD ["+id+"] - REQUEST ID ["+requestIdStr+"] >> " + currentStatus);
                		  error="[Error] Closed";
                		  break;
                	  }
                	  if (currentStatus.trim().contains("Ready for Approval")){
                		  System.out.println("RECORD ["+id+"] - REQUEST ID ["+requestIdStr+"] >> " + currentStatus);
                		  break;
                	  }
                	  
				statusElemWait();currentStatus = statusWait(); 
		    	if (currentStatus.trim().contains("In Planning")){

						//Check FTE with value?
						if (!fteDateVal.isEmpty()) {

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
								if (!array_dataLine[dataCount].isEmpty()) {
									By fieldPath = By.id("49025_COL_"+dataCount);
									WebElement field = wait.until(ExpectedConditions.presenceOfElementLocated(fieldPath));
									wait.until(ExpectedConditions.elementToBeClickable(field)).clear();
									field.sendKeys(array_dataLine[dataCount]);
									field.sendKeys(Keys.TAB);
									alertHandler();	
									if (!error.isEmpty()) {
										System.out.println("[ERROR]:"+error);
										break;
										}
								}
							}
						 }
					   }
						//Populate fields
						for (int ctr = 1; ctr <= 29	; ctr++) {
							 String ctrStr=Integer.toString(ctr);
							
						if (!dataList.get(ctr+12).isEmpty()) {
							 try (InputStream input = new FileInputStream("src/main/resources/properties/elements.properties")) {
						            Properties prop = new Properties();
						            prop.load(input);
						            
						            System.out.println(ctr+"|"+prop.getProperty(ctrStr)+"|"+dataList.get(ctr+12));
									
						            if (ctr==3) {
						            	System.out.println("Country");
										Select DropDown = new Select(driver.findElement(By.id("REQD.P.COUNTRY")));

										DropDown.selectByIndex(0);
										DropDown.selectByVisibleText(dataList.get(ctr+12));
									} else if(ctr==13) {
						            	System.out.println("Reason Position Needed");
										Select DropDown = new Select(driver.findElement(By.id("REQD.P.WFM_REASON_POSITION_NEEDED")));

										DropDown.selectByIndex(0);
										DropDown.selectByVisibleText(dataList.get(ctr+12));
									} else if(ctr==12) {
						            	System.out.println("Bill Type");
										Select DropDown = new Select(driver.findElement(By.id("REQD.P.WFM_BILL_TYPE")));

										DropDown.selectByIndex(0);
										DropDown.selectByVisibleText(dataList.get(ctr+12));
									} else if(ctr==11) {
						            	System.out.println("Resource Type");
										Select DropDown = new Select(driver.findElement(By.id("REQD.P.WFM_RESOURCE_TYPE")));

										DropDown.selectByIndex(0);
										DropDown.selectByVisibleText(dataList.get(ctr+12));
									} else if(ctr==30) {
						            	System.out.println("Position Work Location");
										Select DropDown = new Select(driver.findElement(By.id("REQD.P.WFM_LOC_ALIGN_TO_WPG")));

										DropDown.selectByIndex(0);
										DropDown.selectByVisibleText(dataList.get(ctr+12));
									} else if(ctr==29) {
						            	System.out.println("Location Conts Constrained");
						            	if(dataList.get(ctr+12).toLowerCase().contains("yes")) {
						            		WebElement element = driver.findElement(By.id("REQD.P.WFM_LOCTAION_CONTRACTUALLY_Y"));
						            		((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
											 element.click();
											 System.out.println(element.isSelected());
											 if(!element.isSelected()) {
												 element.click();
											 }
						            	}
						            	if(dataList.get(ctr+12).toLowerCase().contains("no")) {
						            		WebElement element = driver.findElement(By.id("REQD.P.WFM_LOCTAION_CONTRACTUALLY_N"));
											 element.click();
											 System.out.println(element.isSelected());
											 if(!element.isSelected()) {
												 element.click();
											 }
						            	}
						            	
									} else {
										By fieldPath = By.id(prop.getProperty(ctrStr));
										wait.until(ExpectedConditions.presenceOfElementLocated(fieldPath));
										wait.until(ExpectedConditions.elementToBeClickable(fieldPath));
										WebElement field = wait.until(ExpectedConditions.presenceOfElementLocated(fieldPath));
										field.clear();
										
										if (ctr==8) {
											field.clear();
										}
										
										if(ctr==18 || ctr==19) {
											Thread.sleep(200);
											field.clear();
											field.sendKeys(dataList.get(ctr+12).trim());
											field.clear();
											field.sendKeys(dataList.get(ctr+12).trim());
											field.sendKeys(Keys.TAB);
										} else {
											Thread.sleep(200);
											field.sendKeys(dataList.get(ctr+12).trim());
											field.sendKeys(Keys.TAB);
										}
										
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
											if(ctr==7) {
												error="";
												System.out.println("Using Default Value for Requested Resource");
												field.sendKeys("");

												field.sendKeys(Keys.TAB);
											} else if (ctr==25) {
												error="";
												System.out.println("Using Default Value for Primary Skill");
												field.sendKeys("ITIL - General");

												field.sendKeys(Keys.TAB);
											}else if (ctr==27) {
												error="";
												System.out.println("Using Default Value for Secondary Skill");
												field.sendKeys("Tools - General Delivery - Other");

												field.sendKeys(Keys.TAB);
											}else {
												System.out.println("[Data Handler ERROR ]:"+error);
												break;
											}
										}

										
										
									}
						            
									alertHandler();	if (!error.isEmpty()) {
										System.out.println("[ERROR]:"+error);
										break;}
									
									invalidataHandler(dataList.get(ctr+12));if (!error.isEmpty()) {
										System.out.println("[ERROR]:"+error);
										break;}
									
						        } catch (Exception e) {}      
						     }
						   }
				break;
					
		}
		    	}
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
	    	password.sendKeys("Stereorama14!");
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
			Thread.sleep(500);
			WebDriverWait wait = new WebDriverWait(driver, 30);
			By elemPath = By.xpath("//a[contains(@href,'#MENU_SEARCH')]");
			WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
			wait.until(ExpectedConditions.elementToBeClickable(elem));
			WebElement element = driver.findElement(By.xpath("//a[contains(@href,'#MENU_SEARCH')]"));
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
			WebDriverWait wait = new WebDriverWait(driver, 30);
			By elemPath = By.xpath("//a[contains(@class,'yuimenuitemlabel')]");
			WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
			wait.until(ExpectedConditions.elementToBeClickable(elem));
			WebElement element = driver.findElement(By.xpath("//a[contains(text(),'Requests')]"));
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

	public static WebElement submit() {
		for (int x = 0; x < 20; x++) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, 5);
			By elemPath = By.id("submit");
			WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
			wait.until(ExpectedConditions.elementToBeClickable(elem));
			return elem;
		} catch (Exception e) {
			driver.navigate().refresh();
			System.out.println("[WAITING] SUBMIT BUTTON");
		}
	  }
		return null;
	}
	
	private static void ongoingUpate() {
		for (int x = 0; x < 20; x++) {
		try {
			endRec = Instant.now();timeElapsedRec = Duration.between(startRec, endRec);
			 if (!error.isEmpty()) {
				 update.setString(1, error);
				 update.setString(3, error.replaceAll("\\s",""));
				}else {
				update.setString(1, "DONE");
				update.setString(3, currentStatus);
			 }

			 update.setString(2, requestIdStr);
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
	
	public static WebElement reqID() {
		for (int x = 0; x < 20; x++) {
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
			//Default Value for Primary Skill and Secondary skill
			WebElement searchTextBoxPrimarySkill= driver.findElement(By.id("REQD.P.PRIMARY_SKILLAC_TF"));
			WebElement searchTextBoxSecondarySkill= driver.findElement(By.id("REQD.P.SECONDARY_SKILLAC_TF"));
			
			// retrieving html attribute value using getAttribute() method
			String typeValue=searchTextBoxPrimarySkill.getAttribute("value");
			String typeValueSecondary=searchTextBoxSecondarySkill.getAttribute("value");
			System.out.println("Value of type attribute: "+typeValue);
			
			if(typeValue.isEmpty())
			{
				System.out.println("Using Default Value for Primary Skill");
				searchTextBoxPrimarySkill.sendKeys("ITIL - General");

				searchTextBoxPrimarySkill.sendKeys(Keys.TAB);
				
				System.out.println("Using Default Value for Secondary Skill");
				searchTextBoxSecondarySkill.sendKeys("Tools - General Delivery - Other");

				searchTextBoxSecondarySkill.sendKeys(Keys.TAB);
			}
			
			if(typeValueSecondary.isEmpty())
			{
				
				System.out.println("Using Default Value for Secondary Skill");
				searchTextBoxSecondarySkill.sendKeys("Tools - General Delivery - Other");

				searchTextBoxSecondarySkill.sendKeys(Keys.TAB);
			}
			
			//Click Complete PLM
			By elemPath = By.xpath("//a//div[contains(text(), 'Complete PLM')]");
			WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
			wait.until(ExpectedConditions.elementToBeClickable(elem));
			WebElement element = driver.findElement(By.xpath("//a//div[contains(text(), 'Complete PLM')]"));
			System.out.println("RECORD ["+id+"] - REQUEST ID ["+requestIdStr+"] >> [COMPLETE PLM]");
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
			System.out.println("RECORD ["+id+"] - PROJECT ID ["+requestIdStr+"] >> [RELEASE FOR APPROVAL]");
			return element;
		} catch (Exception e) {
			driver.navigate().refresh();
			System.out.println("[WAITING] RELEASE FOR APPROVAL BUTTON");
		}
		}
		return null;
	}
	
	public static WebElement approveADL() {
		for (int x = 0; x < 20; x++) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, 10);
			By elemPath = By.xpath("//*[@id=\"DB1_0\"]");
			WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
			wait.until(ExpectedConditions.elementToBeClickable(elem));
			WebElement element = driver.findElement(By.xpath("//*[@id=\"DB1_0\"]"));
			System.out.println("RECORD ["+id+"] - PROJECT ID ["+requestIdStr+"] >> [Approved ADL]");
			return element;
		} catch (Exception e) {
			driver.navigate().refresh();
			System.out.println("[WAITING] Approval BUTTON");
		}
		}
		return null;
	}
	
	   public static boolean approveAEBtn() {
			for (int x = 0; x < 5; x++) {
			try {
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
	
	 public static WebElement approveAE() {
	  		for (int x = 0; x < 20; x++) {
	  		try {
	  			WebDriverWait wait = new WebDriverWait(driver, 10);
	  			By elemPath = By.xpath("//*[@id=\"DB0_0\"]");
	  			WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
	  			wait.until(ExpectedConditions.elementToBeClickable(elem));
	  			WebElement element = driver.findElement(By.xpath("//*[@id=\"DB0_0\"]"));
	  			System.out.println("RECORD ["+id+"] - PROJECT ID ["+requestIdStr+"] >> [Approved ADL]");
	  			return element;
	  		} catch (Exception e) {
	  			driver.navigate().refresh();
	  			System.out.println("[WAITING] Approval BUTTON");
	  		}
	  		}
	  		return null;
	  	}
	
	public static WebElement approveADLDmdPlanner() {
		for (int x = 0; x < 20; x++) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, 10);
			By elemPath = By.xpath("//*[@id=\"DB0_0\"]");
			WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
			wait.until(ExpectedConditions.elementToBeClickable(elem));
			WebElement element = driver.findElement(By.xpath("//*[@id=\"DB0_0\"]"));
			System.out.println("RECORD ["+id+"] - PROJECT ID ["+requestIdStr+"] >> [Approved DMD PLANNER]");
			return element;
		} catch (Exception e) {
			driver.navigate().refresh();
			System.out.println("[WAITING] Approval BUTTON");
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
			WebElement element = driver.findElement(By.xpath("//*[@id=\"DB1_1\"]"));
			System.out.println("RECORD ["+id+"] - PROJECT ID ["+requestIdStr+"] >> [Approved EARLY STAFFING]");
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
			By elemPath = By.id("DB1_1");
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
	
	
	public static WebElement moveToSp() {
		for (int x = 0; x < 20; x++) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, 10);
			By elemPath = By.xpath("//*[@id=\"DB0_0\"]");
			WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
			wait.until(ExpectedConditions.elementToBeClickable(elem));
			WebElement element = driver.findElement(By.xpath("//*[@id=\"DB0_0\"]"));
			System.out.println("RECORD ["+id+"] - PROJECT ID ["+requestIdStr+"] >> [Move to SP]");
			return element;
		} catch (Exception e) {
			driver.navigate().refresh();
			System.out.println("[WAITING] Move yo SP BUTTON");
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
				WebDriverWait wait = new WebDriverWait(driver, 60);
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
			WebDriverWait wait = new WebDriverWait(driver, 60
					);
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
			for (int x = 0; x < 3; x++) {
			try {
				WebDriverWait wait = new WebDriverWait(driver, 1);
				Alert alert = wait.until(ExpectedConditions.alertIsPresent());
				String alertmessage = alert.getText();
				alert.accept();
				System.out.println("[ALERT MESSAGE]:"+alertmessage);
			    error=alertmessage;
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
					 }
				 }
			} catch (Exception e) {
			}
		 }
		
		public static WebElement reDefine() {
			for (int x = 0; x < 20; x++) {
			try {
				WebDriverWait wait = new WebDriverWait(driver, 10);
				By elemPath = By.xpath("//div[contains(text(), 'ReDefine Demand')]");
				WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
				wait.until(ExpectedConditions.elementToBeClickable(elem));
				WebElement element = driver.findElement(By.xpath("//div[contains(text(), 'ReDefine Demand')]"));
				return element;
			} catch (Exception e) {
				driver.navigate().refresh();
				System.out.println("[WAITING] REDEFINE BUTTON");
			}
			}
			return null;
		}
		
		public static WebElement reworkPLM() {
			for (int x = 0; x < 20; x++) {
			try {
				WebDriverWait wait = new WebDriverWait(driver, 10);
				By elemPath = By.xpath("//*[@id=\"DB0_1\"]");
				WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
				wait.until(ExpectedConditions.elementToBeClickable(elem));
				WebElement element = driver.findElement(By.xpath("//*[@id=\"DB0_1\"]"));
				return element;
			} catch (Exception e) {
				driver.navigate().refresh();
				System.out.println("[WAITING] REWORK ON PLM BUTTON");
			}
			}
			return null;
		}
	
	public static boolean reDefineCheck() {
		for (int x = 0; x < 2; x++) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, 2);
			By elemPath = By.xpath("//div[contains(text(), 'ReDefine Demand')]");
			WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
			if (elem.isDisplayed()) {
				return true;
			}else{
				return false;
			}
		} catch (Exception e) {
		}
		}
		return false;
	}
	
	public static boolean reworkOnPLM() {
		for (int x = 0; x < 2; x++) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, 2);
			By elemPath = By.xpath("//*[@id=\"DB0_1\"]");
			WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
			if (elem.isDisplayed()) {
				return true;
			}else{
				return false;
			}
		} catch (Exception e) {
		}
		}
		return false;
	}
	
	private static void searchRequestId(String text) {
		for (int x = 0; x < 20; x++) {
			try {
				request_id().clear();
				request_id().sendKeys(text);
				request_id().sendKeys(Keys.ENTER);
				break;
			} catch (Exception e) {
				driver.navigate().refresh();
				System.err.println("[WAITING] REQUEST ID SEND");
			}
		}
	}
	
	public static WebElement request_id() {
		for (int x = 0; x < 20; x++) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, 10);
			By elemPath = By.xpath("//input[contains(@name,'REQUEST_ID')]");
			WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
			wait.until(ExpectedConditions.elementToBeClickable(elem));
			WebElement element = driver.findElement(By.xpath("//input[contains(@name,'REQUEST_ID')]"));
			return element;
		} catch (Exception e) {
			driver.navigate().refresh();
			System.out.println("[WAITING] REQUEST ID");
			search_menu().click();
        	request_submenu().click();
		}
		}
		return null;
	}
	
	public static boolean error() {
		for (int x = 0; x < 20; x++) {
		try {
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
				return false;
			}
		} catch (Exception e) {
		}
		}
		return false;
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
	
	public static boolean approveBtn() {
		for (int x = 0; x < 20; x++) {
		try {
			Thread.sleep(100);
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
	public static boolean accessError() {
		for (int x = 0; x < 20; x++) {
		try {
			Thread.sleep(100);
			WebDriverWait wait = new WebDriverWait(driver, 5);
			Boolean isPresent = driver.findElements(By.xpath("//*[@id=\"page-min-width-div\"]/div[5]/div/table/tbody/tr[3]/td[3]/pre")).size() > 0;
			System.out.println("Access Error Page: "+isPresent);
			if (isPresent) {
				 error="You do not have access";
				return true;
			}else{
				return false;
			}
		} catch (Exception e) {
		}
		}
		
		return false;
	}
	 public static void editFTE_deleteExisiting() throws Throwable {
	 		
	 		errorDesc="Deleting ftes";
	 			try { // mod

	 				// page.value("BT_EDIT_P_1").click();
	 				WebDriverWait wait = new WebDriverWait(driver, 20);
	 				By editbtn = By.id("BT_EDIT_P_1");
	 				By delbtn = By.id("BT_DEL_ROW_P_1");
	 				By viewbtn = By.id("BT_VIEW_P_1");
	 				WebElement editbtnstl = wait.until(ExpectedConditions.presenceOfElementLocated(editbtn));
	 				Thread.sleep(2000);
	 				wait.until(ExpectedConditions.elementToBeClickable(editbtnstl)).click();
	 				WebElement dltbtnstl = wait.until(ExpectedConditions.presenceOfElementLocated(delbtn));
	 				WebElement vwbtnstl = wait.until(ExpectedConditions.presenceOfElementLocated(viewbtn));								
	 					boolean bol = true;
	 					
	 					do {
	 						Thread.sleep(3000);
	 						wait.until(ExpectedConditions.elementToBeClickable(dltbtnstl)).click();
	 						Robot robot = new Robot();
	 						robot.keyPress(KeyEvent.VK_ENTER);
	 						robot.keyRelease(KeyEvent.VK_ENTER);
	 						WebDriverWait wait5 = new WebDriverWait(driver, 10);
	 						Alert waitalrt = wait5.until(ExpectedConditions.alertIsPresent());
	 						robot.delay(200);
	 						waitalrt.accept();
	 						Thread.sleep(2000);
	 						wait.until(ExpectedConditions.elementToBeClickable(vwbtnstl)).click();
	 						Thread.sleep(2000);
	 						wait.until(ExpectedConditions.elementToBeClickable(editbtnstl)).click();
//	 						final String DEL_BTN_CLASS = page.value("BT_DEL_ROW_P_1").getAttribute("class");
	 					    

	 						final String DEL_BTN_CLASS = wait.until(ExpectedConditions.elementToBeClickable(dltbtnstl)).getAttribute("class");


	 						if(DEL_BTN_CLASS.contentEquals("tc-btn-enabled")) {
	 							bol = true;
	 							
	 						}
	 						else {
	 							bol = false;
	 							System.out.println("FTEs deleted. Proceed to update.");
	 							Thread.sleep(2000);
	 							wait.until(ExpectedConditions.elementToBeClickable(vwbtnstl)).click();
	 						}
	 												
	 					}while(bol == true);
	 					

	 			} catch (Exception e) {
	 				errorDesc = "deleting existing fte error. retry again";
	 				System.out.println(e);
//	 			error_writeNotSubmmitedinExcel();

	 			}

	 		}
}

