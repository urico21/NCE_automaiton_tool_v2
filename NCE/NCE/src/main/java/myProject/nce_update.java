
//Apply Changes for testing
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


public class nce_update {	
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
			query="SELECT COUNT(*) FROM public.nce_update WHERE key_status IN ('PENDING','ONGOING') AND parallel_key='"+thread+"';";
			rsQuery="SELECT * FROM public.nce_update WHERE key_status IN ('PENDING','ONGOING') AND parallel_key='"+thread+"' ORDER BY id;";
	        countRs = count.executeQuery(query);
	        	countRs.next();	
	            rowCount = countRs.getInt(1);
	            start = Instant.now();
	            System.out.println("");
	            System.out.println("[THREAD "+ thread +"]: PROCESSING...");
	            System.out.println("TIME START: "+start);
	            System.out.println("TOTAL 'UPDATE' DEMAND(S) ["+rowCount+"]");
	            if (rowCount==0) {driver.quit();System.exit(0);}
	            for (int x = 0; x < rowCount; x++) {
	    	        try {
	    	            if (connection != null) {
	   	                stmt = connection.createStatement();
	   	                update = connection.prepareStatement("UPDATE nce_update SET key_status = ?, plm_id = ?, status = ?, duration = ? WHERE parallel_key=? AND id = ?;");
	   	                rs = stmt.executeQuery(rsQuery);	
	   			                while (rs.next()) {
	   			                	startRec= Instant.now();
	   			                	System.out.println("");
	   			                	error="";
	   			                	requestIdStr=rs.getString("plm_id".trim()); 			                   	 
				                	id=rs.getString("id".trim());
				                	fteDateStr=rs.getString("fte".trim());
				                	searchRequestId(requestIdStr.trim());
				                	System.out.println("RECORD ["+id+"] - REQUEST ID ["+requestIdStr+"] >> PROCESSING");
				                	
				                	//CHECK IF I HAVE ACCES TO THE ACCOUNT
				                	if(!accessError()) {
				                		//STATUS WAIT
				                		statusElemWait();currentStatus = statusWait();

						                	dataList.clear();
						                	for (int count=1; count <= 44;count++) {
						                		dataList.add(rs.getString(count));
						                	}
						                	statusElemWait();
				                	      	//STATUS WAIT
							                	currentStatus = statusWait();
							                	System.out.println("REQUEST STATUS: "+currentStatus);
							                	if (currentStatus.trim().contains("Staffing Approved") || currentStatus.trim().contains("PLM Approved")) {
							                		reworkOnPLM();
													if (reworkOnPLM()) {
														reworkPLM().click();	
														System.out.println("RECORD ["+id+"] - REQUEST ID ["+requestIdStr+"] >> REWORK ON PLM");
													}else {
														System.out.println("RECORD ["+id+"] - REQUEST ID ["+requestIdStr+"] >> PPMC TIMEDOUT");
														error="[ERROR] PPMC TIMEDOUT";
													}
													
												}
					                	      	
					                	      	
							                	//STATUS WAIT
							                	statusElemWait();currentStatus = statusWait();
							                	Thread.sleep(100);
							                	//CHECK IF STATUS AFTER MAIN METHOD | CANCELLED
							                	if(currentStatus.trim().contains("Cancelled")) {
							                		System.out.println("RECORD ["+id+"] - REQUEST ID ["+requestIdStr+"] >> " + currentStatus);
							                		  error="[Error] Cancelled";
							                	}
							                	statusElemWait();currentStatus = statusWait();
							                	//CHECK IF STATUS AFTER MAIN METHOD | CLOSED
							                	if( currentStatus.trim().contains("Closed")) {
							                		System.out.println("RECORD ["+id+"] - REQUEST ID ["+requestIdStr+"] >> " + currentStatus);
							                		  error="[Error] Closed";
							                	}			
							                	
							                	statusElemWait();currentStatus = statusWait();
							                	//Check if current status id ADL, AE or PLM Approval
							                	if(currentStatus.trim().contains("Pending ADL Approval")||currentStatus.trim().contains("Pending AE Approval")||currentStatus.trim().contains("PLM Approved")) {
							                		error="[VALIDATE] DEMAND ON APPROVAL STATE - MOVE DEMAND TO IN-PLANNING STATE THEN REPROCESS";
							                	}
							                	
							                	
							                	
							                	statusElemWait();currentStatus = statusWait();
							                	System.out.println("REQUEST STATUS: "+currentStatus);
							                	//MAIN METHOD
							                	//MAIN METHOD IF STATUS IS IN PLANNING, FOR APPROVAL OR POSITION TO SP MOVE TO MAIN METHOD
							                	if(currentStatus.trim().contains("Ready for Approval")) {
							                		populate_ReadyApproval(requestIdStr, fteDateStr, dataList);
							                	}
							                	statusElemWait();currentStatus = statusWait();
							                	if( currentStatus.trim().contains("In Planning") || currentStatus.trim().contains("Position Created in SP")) {
							                		populate_InPlanning(requestIdStr, fteDateStr, dataList);
							                	}
							                	statusElemWait();currentStatus = statusWait();
							                	//If Demand is still on SP
							                	if (currentStatus.trim().contains("Position Created in SP")) {
													  error="[VALIDATE] STATUS IS STILL: Position Created in SP - TRY TO REPROCESS IF NOT CREDENTIAL ISSUE"; 
												  }
							                	
							                	//Check if status is 'IN PLANNING' then approve
												
												  statusElemWait();currentStatus = statusWait();
												  Thread.sleep(500);
												  
												  
												    //Catch Error on first click
								                	if(currentStatus.trim().contains("In Planning")) {
								                		System.out.println("RECORD ["+id+"] - REQUEST ID ["+requestIdStr+"] >> " + currentStatus);
							              						completePLM().click();
							              						indicatorES(dataList).click();
								                	}
								                	
								                	//Amend Skills Issue
								                	if(currentStatus.trim().contains("In Planning")) {
								                		System.out.println("RECORD ["+id+"] - REQUEST ID ["+requestIdStr+"] >> " + currentStatus);
							              						completePLM().click();
							              						indicatorES(dataList).click();
								                	}
								                	
								                	//Ready for Approval
													statusElemWait();currentStatus = statusWait();
										            //Check if status is 'FOR APPROVAL' then approve
													Thread.sleep(500);
													
													if(currentStatus.trim().contains("Ready for Approval")) {
										                		System.out.println("RECORD ["+id+"] - REQUEST ID ["+reqID+"] >> " + currentStatus);
									              				releaseForAppvl().click();
										            }else {
									                			  error="[Error] Approval Button Not Activated - Release for Approval"; 
									                	  } 
										                	
										                	
													//check for other issue
								                	alertHandler();	if (!error.isEmpty()) {
														System.out.println("[ERROR]:"+error);
														}
													
													invalidataHandler();if (!error.isEmpty()) {
														System.out.println("[ERROR]:"+error);
														}
																									
							                error();
							                
							                							                
						                	if (!error.isEmpty()) {			                		
												System.out.println("RECORD ["+id+"] - PROJECT ID ["+requestIdStr+"] >> SKIPPED");
						                	}else {
						                		
						                		//APPROVAL//
						                		//Get request ID
							                	reqID = getReqIDt().getAttribute("innerText");
							                	int ctr = 0;
												  
												  ctr=0;//Reset counter
												  //Check which Approval instance after Ready for Approval
												  do {
													  //CHECK IF PENDING ADL APROVAL 
													  statusElemWait();currentStatus = statusWait();
													  Thread.sleep(500);
													  if(currentStatus.trim().contains("Pending ADL Approval")) {
													  		System.out.println("RECORD ["+id+"] - REQUEST ID ["+requestIdStr+"] >>  APPROVAL RELEASED");
													  		System.out.println("RECORD ["+id+"] - REQUEST ID ["+requestIdStr+"] >> " + currentStatus);
													  		if(currentStatus.trim().contains("Pending ADL Approval")) {
													  			approveADL().click();	
													  		} else {
													  			error="[Error] Approval Button Not Activated - ADL Approval"; 
													  		} 
													  }
													  //AE Approval 
													  statusElemWait();currentStatus = statusWait();
								                	  Thread.sleep(500);
								                	  if(currentStatus.trim().contains("Pending AE Approval")) {
								                		  System.out.println("RECORD ["+id+"] - REQUEST ID ["+requestIdStr+"] >>  APPROVAL RELEASED");
								                		  System.out.println("RECORD ["+id+"] - REQUEST ID ["+requestIdStr+"] >> " + currentStatus);
								                		  if(currentStatus.trim().contains("Pending AE Approval")) {
								                			  error="";
								                			  approveAE().click();
								                		  } else {
								                			  error="[Error] Pending AE Approval Approval Button Not Activated"; 
								                		  }
								                	  }		
								                	   
													  }while(currentStatus.trim().contains("Pending ADL Approval")||currentStatus.trim().contains("Pending AE Approval"));
							                		  								                	
												    ctr=0;//Reset counter
												    
												    //CHECK IF PENDING DMD APPROVAL
//								                	do {currentStatus = statusWait();
//								                	Thread.sleep(500);
//							                		  System.out.println("RECORD ["+id+"] - REQUEST ID ["+reqID+"] >> " + currentStatus);
//							                		  if(approveBtnDmdPlanner() && currentStatus.trim().contains("Pending Dmd Planner Approval")) {
//							                			
//							                			//CHECK IF CANCEL BUTTON I SHOWNED IN PLM APPROVED, REFRESH PAGE IF YES
//							                			  String HeaderTxt = driver.findElement(By.xpath("//*[@id=\"DB0_0\"]")).getText();
//							                			  String expectedHeading = "Cancel";
//							              					if(expectedHeading.equalsIgnoreCase(HeaderTxt)) {
//							              						 System.out.println("==Refresh Page==");
//							              						 driver.navigate().refresh();
//							              					}else {
//							              						approveADLDmdPlanner().click();
//							              					}
//						           						
//							                		  } else {
//							                			  error="[Error] Approval Button Not Activated - DMD Approval"; 
//							                		  }
//							                		  if (ctr==5) {
//								                			break;
//								                		}
//							                		  ctr++; 							                		  
//								                	} while (currentStatus.trim().contains("Pending Dmd Planner Approval"));
								                	
								                	//PLM Approval
												    statusElemWait();currentStatus = statusWait();
								                	Thread.sleep(500);
							                		  System.out.println("RECORD ["+id+"] - REQUEST ID ["+requestIdStr+"] >> " + currentStatus);
							                		  if(currentStatus.trim().contains("PLM Approved")) {
							                			  //CHECK IF CANCEL BUTTON I SHOWNED IN PLM APPROVED, REFRESH PAGE IF YES
							                			  String HeaderTxt = driver.findElement(By.xpath("//*[@id=\"DB0_0\"]")).getText();
							                			  String expectedHeading = "Cancel";
							              					if(expectedHeading.equalsIgnoreCase(HeaderTxt)) {
							              						 System.out.println("==Refresh Page==");
							              						 driver.navigate().refresh();
							              					}else {
									                			  moveToSp().click();
							              					}
							                		  } else {
							                			  error="[Error] Approval Button Not Activated - PLM Approval"; 
							                			  
							                		  }
						                	  
							                		  //Staffing Approval
							                		  statusElemWait();currentStatus = statusWait();
							                		  Thread.sleep(500);
							                		  System.out.println("RECORD ["+id+"] - REQUEST ID ["+requestIdStr+"] >> " + currentStatus);
							                		  if(currentStatus.trim().contains("Staffing Approved")) {
									                			  moveToSp().click();
							              					
							                		  } else {
							                			  error="[Error] Approval Button Not Activated - Staffing Approval"; 
								                		  								                			
							                		  }	
								                  
							                		statusElemWait();currentStatus = statusWait();
								                	Thread.sleep(500);
								                  if (currentStatus.trim().contains("Position Created in SP")) {
													  error="DONE"; 
												  }
								                  if (currentStatus.trim().contains("Pending Org Lead Approval")) {
													  error="DONE"; 
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
	    	            System.out.println("You're here");
	    	        	ongoingUpate();
	                	update.executeUpdate();
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
   
	
	
    public static void populate_InPlanning(String reqIDVal, String fteDateVal, List<String> dataArryVal) throws Throwable {
    	
    	for (int x = 0; x < 10; x++) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, 5);
			//Check ProjId with value?
			if (!reqIDVal.isEmpty()) {
					invalidataHandler();
					statusElemWait();
					currentStatus = statusWait(); 
					if (currentStatus.trim().contains("Position Created in SP")) {
						reDefineCheck();
						if (reDefineCheck()) {
							
							reDefine().click();	
							System.out.println("RECORD ["+id+"] - REQUEST ID ["+requestIdStr+"] >> REDEFINED");

						}else {
							System.out.println("RECORD ["+id+"] - REQUEST ID ["+requestIdStr+"] >> PPMC TIMEDOUT");
							error="[Error] PPMC TIMEDOUT";
							break;
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
							try {
	                           	System.out.println("RECORD ["+id+"] - REQUEST ID ["+requestIdStr+"] >> Deleting FTE RECORDS"); 
									editFTE_deleteExisiting();
								} catch (Throwable e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
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
										break;}
								}
							}
						 }
					   }
						//Populate create fields
						for (int ctr = 1; ctr <= 33	; ctr++) {
							 String ctrStr=Integer.toString(ctr);
							
						if (!dataList.get(ctr+10).isEmpty()) {
							 try (InputStream input = new FileInputStream("src/main/resources/properties/elements.properties")) {
						            Properties prop = new Properties();
						            prop.load(input);
						            
						            System.out.println(ctr+"|"+prop.getProperty(ctrStr)+"|"+dataList.get(ctr+10));
									
						            if (ctr==2) {
						            	System.out.println("Country");
										Select DropDown = new Select(driver.findElement(By.id("REQD.P.COUNTRY")));

										DropDown.selectByIndex(0);
										DropDown.selectByVisibleText(dataList.get(ctr+10));
									} else if(ctr==11) {
						            	System.out.println("Reason Position Needed");
										Select DropDown = new Select(driver.findElement(By.id("REQD.P.WFM_REASON_POSITION_NEEDED")));

										DropDown.selectByIndex(0);
										DropDown.selectByVisibleText(dataList.get(ctr+10));
									} else if(ctr==10) {
						            	System.out.println("Bill Type");
										Select DropDown = new Select(driver.findElement(By.id("REQD.P.WFM_BILL_TYPE")));

										DropDown.selectByIndex(0);
										DropDown.selectByVisibleText(dataList.get(ctr+10));
									} else if(ctr==9) {
						            	System.out.println("Resource Type");
										Select DropDown = new Select(driver.findElement(By.id("REQD.P.WFM_RESOURCE_TYPE")));

										DropDown.selectByIndex(0);
										DropDown.selectByVisibleText(dataList.get(ctr+10));
										
									} else if(ctr==21) {
						            	System.out.println("Language Aptitude");
										Select DropDown = new Select(driver.findElement(By.id("REQD.P.SPOKEN_LANG_APTITUDE")));

										DropDown.selectByIndex(0);
										DropDown.selectByVisibleText(dataList.get(ctr+10));	
									} else if(ctr==23) {
						            	System.out.println("Primary Skill Aptitude");
										Select DropDown = new Select(driver.findElement(By.id("REQD.P.PRIMARY_SKILL_APTITUDE")));

										DropDown.selectByIndex(0);
										DropDown.selectByVisibleText(dataList.get(ctr+10));
										
									} else if(ctr==25) {
						            	System.out.println("Secondary Skill Aptitude");
										Select DropDown = new Select(driver.findElement(By.id("REQD.P.SECONDARY_SKILL_APTITUDE")));

										DropDown.selectByIndex(0);
										DropDown.selectByVisibleText(dataList.get(ctr+10));	
										
									} else if(ctr==27) {
						            	System.out.println("Position Work Location");
										Select DropDown = new Select(driver.findElement(By.id("REQD.P.WFM_LOC_ALIGN_TO_WPG")));

										DropDown.selectByIndex(0);
										DropDown.selectByVisibleText(dataList.get(ctr+10));
									
									} else if(ctr==33) {
						            	System.out.println("Client Interview");
										Select DropDown = new Select(driver.findElement(By.id("REQD.P.CLIENT_INTRW")));

										DropDown.selectByIndex(0);
										DropDown.selectByVisibleText(dataList.get(ctr+10));
										
									} else if(ctr==26) {
						            	System.out.println("Location Conts Constrained");
						            	if(dataList.get(ctr+10).toLowerCase().contains("yes")) {
						            		WebElement element = driver.findElement(By.id("REQD.P.WFM_LOCTAION_CONTRACTUALLY_Y"));
						            		//((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
											 element.click();
											 System.out.println(element.isSelected());
											 if(!element.isSelected()) {
												 element.click();
											 }
						            	}
						            	if(dataList.get(ctr+10).toLowerCase().contains("no")) {
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
										
										if (ctr==10) {
											field.clear();
										}
										
										if(ctr==16 || ctr==17) {
			
											Thread.sleep(200);
											field.clear();
											field.sendKeys(dataList.get(ctr+10).trim());
											field.clear();
											field.sendKeys(dataList.get(ctr+10).trim());
											field.sendKeys(Keys.TAB);
										} else {
											Thread.sleep(200);
											field.sendKeys(dataList.get(ctr+10).trim());
											field.sendKeys(Keys.TAB);
										}
										
										alertHandler();	if (!error.isEmpty()) {
											System.out.println("[ERROR]:"+error);
											break;}
										
										invalidataHandler();if (!error.isEmpty()) {
											System.out.println("[ERROR]:"+error);
											break;}
									}
						            alertHandler();	if (!error.isEmpty()) {
										System.out.println("[ERROR]:"+error);
										break;}
									
									invalidataHandler();if (!error.isEmpty()) {
										System.out.println("[ERROR]:"+error);
										break;}
									
						        } catch (Exception e) {
//						        	if (!error.isEmpty()) {
//						        	System.out.println("[ERROR]:"+error);
//						        	}
						        }      
						     }
						   }				
	
				break;
					
		}
		}
			
		} catch (Exception e) {
//			if (!error.isEmpty()) {
//	        	System.out.println("[ERROR]:"+error);
//	        	}
		}
		
	  }
		
	}
	
    public static void populate_ReadyApproval(String reqIDVal, String fteDateVal, List<String> dataArryVal) throws Throwable {
		
    	   
    	for (int x = 0; x < 10; x++) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, 5);
			//Check ProjId with value?
			if (!reqIDVal.isEmpty()) {
					invalidataHandler();
					statusElemWait();
					currentStatus = statusWait();

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
                	  if (currentStatus.trim().contains("In Planning")){
                		  System.out.println("RECORD ["+id+"] - REQUEST ID ["+requestIdStr+"] >> " + currentStatus);
                		  break;
                	  }
                	  
				statusElemWait();currentStatus = statusWait(); 
		    	if (currentStatus.trim().contains("Ready for Approval")){

					//Check FTE with value?
					if (!fteDateVal.isEmpty()) {
						try {
                           	System.out.println("RECORD ["+id+"] - REQUEST ID ["+requestIdStr+"] >> Deleting FTE RECORDS"); 
								editFTE_deleteExisiting();
							} catch (Throwable e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
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
									break;}
							}
						}
					 }
				   }
					//Populate create fields
					for (int ctr = 1; ctr <= 33	; ctr++) {
						 String ctrStr=Integer.toString(ctr);
						
					if (!dataList.get(ctr+10).isEmpty()) {
						 try (InputStream input = new FileInputStream("src/main/resources/properties/elements.properties")) {
					            Properties prop = new Properties();
					            prop.load(input);
					            
					            System.out.println(ctr+"|"+prop.getProperty(ctrStr)+"|"+dataList.get(ctr+10));
								
					            if (ctr==2) {
					            	System.out.println("Country");
									Select DropDown = new Select(driver.findElement(By.id("REQD.P.COUNTRY")));

									DropDown.selectByIndex(0);
									DropDown.selectByVisibleText(dataList.get(ctr+10));
								} else if(ctr==12) {
					            	System.out.println("Reason Position Needed");
									Select DropDown = new Select(driver.findElement(By.id("REQD.P.WFM_REASON_POSITION_NEEDED")));

									DropDown.selectByIndex(0);
									DropDown.selectByVisibleText(dataList.get(ctr+10));
								} else if(ctr==11) {
					            	System.out.println("Bill Type");
									Select DropDown = new Select(driver.findElement(By.id("REQD.P.WFM_BILL_TYPE")));

									DropDown.selectByIndex(0);
									DropDown.selectByVisibleText(dataList.get(ctr+10));
								} else if(ctr==10) {
					            	System.out.println("Resource Type");
									Select DropDown = new Select(driver.findElement(By.id("REQD.P.WFM_RESOURCE_TYPE")));

									DropDown.selectByIndex(0);
									DropDown.selectByVisibleText(dataList.get(ctr+10));
									
								} else if(ctr==21) {
					            	System.out.println("Language Aptitude");
									Select DropDown = new Select(driver.findElement(By.id("REQD.P.SPOKEN_LANG_APTITUDE")));

									DropDown.selectByIndex(0);
									DropDown.selectByVisibleText(dataList.get(ctr+10));	
								} else if(ctr==23) {
					            	System.out.println("Primary Skill Aptitude");
									Select DropDown = new Select(driver.findElement(By.id("REQD.P.PRIMARY_SKILL_APTITUDE")));

									DropDown.selectByIndex(0);
									DropDown.selectByVisibleText(dataList.get(ctr+10));
									
								} else if(ctr==25) {
					            	System.out.println("Secondary Skill Aptitude");
									Select DropDown = new Select(driver.findElement(By.id("REQD.P.SECONDARY_SKILL_APTITUDE")));

									DropDown.selectByIndex(0);
									DropDown.selectByVisibleText(dataList.get(ctr+10));	
									
								} else if(ctr==27) {
					            	System.out.println("Position Work Location");
									Select DropDown = new Select(driver.findElement(By.id("REQD.P.WFM_LOC_ALIGN_TO_WPG")));

									DropDown.selectByIndex(0);
									DropDown.selectByVisibleText(dataList.get(ctr+10));
									
								} else if(ctr==26) {
					            	System.out.println("Location Conts Constrained");
					            	if(dataList.get(ctr+10).toLowerCase().contains("yes")) {
					            		WebElement element = driver.findElement(By.id("REQD.P.WFM_LOCTAION_CONTRACTUALLY_Y"));
										 element.click();
										 System.out.println(element.isSelected());
										 if(!element.isSelected()) {
											 element.click();
										 }
					            	}
					            	if(dataList.get(ctr+10).toLowerCase().contains("no")) {
					            		WebElement element = driver.findElement(By.id("REQD.P.WFM_LOCTAION_CONTRACTUALLY_N"));
										 element.click();
										 System.out.println(element.isSelected());
										 if(!element.isSelected()) {
											 element.click();
										 }
					            	}
					            	
								} else if(ctr==32) {
					            	System.out.println("Location Conts Constrained");
					            	if(dataList.get(ctr+10).toLowerCase().contains("yes")) {
					            		WebElement element = driver.findElement(By.id("REQD.P.WFM_PROJECT_SOLD_Y"));
										 element.click();
										 System.out.println(element.isSelected());
										 if(!element.isSelected()) {
											 element.click();
										 }
					            	}
					            	if(dataList.get(ctr+10).toLowerCase().contains("no")) {
					            		WebElement element = driver.findElement(By.id("REQD.P.WFM_PROJECT_SOLD_N"));
										 element.click();
										 System.out.println(element.isSelected());
										 if(!element.isSelected()) {
											 element.click();
										 }
					            	}
					            	
								} else if(ctr==33) {
					            	System.out.println("Location Conts Constrained");
					            	if(dataList.get(ctr+10).toLowerCase().contains("yes")) {
					            		WebElement element = driver.findElement(By.id("REQD.P.WFM_EARLY_STAFF_FLAG_Y"));
										 element.click();
										 System.out.println(element.isSelected());
										 if(!element.isSelected()) {
											 element.click();
										 }
					            	}
					            	if(dataList.get(ctr+10).toLowerCase().contains("no")) {
					            		WebElement element = driver.findElement(By.id("REQD.P.WFM_EARLY_STAFF_FLAG_N"));
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
									
									if (ctr==10) {
										field.clear();
									}
									
									if(ctr==17 || ctr==18) {
										Thread.sleep(200);
										field.clear();
										field.sendKeys(dataList.get(ctr+10).trim());
										field.clear();
										field.sendKeys(dataList.get(ctr+10).trim());
										field.sendKeys(Keys.TAB);
									} else {
										Thread.sleep(200);
										field.sendKeys(dataList.get(ctr+10).trim());
										field.sendKeys(Keys.TAB);
									}
									
										alertHandler();	if (!error.isEmpty()) {
										System.out.println("[ERROR]:"+error);
										break;}
										
									
										invalidataHandler();if (!error.isEmpty()) {
											System.out.println("[ERROR]:"+error);
										}
									
								
								}
					            
					            
								alertHandler();	if (!error.isEmpty()) {
									System.out.println("[ERROR]:"+error);
									break;}
								
								invalidataHandler();if (!error.isEmpty()) {
									System.out.println("[ERROR]:"+error);
									break;}
								
								
					        } catch (Exception e) {
					        	//System.out.println("[ERROR]:"+error);
					        }      
					     }
					   }				

			break;
				
	}}
			
		} catch (Exception e) {
			//System.out.println("[ERROR]:"+error);
		}
		
	  }
		
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
	public static WebElement approveADLDmdPlanner() {
		for (int x = 0; x < 20; x++) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, 15);
			By elemPath = By.xpath("//a//div[contains(text(), 'Approve')]");
			WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
			wait.until(ExpectedConditions.elementToBeClickable(elem));
			WebElement element = driver.findElement(By.xpath("//a//div[contains(text(), 'Approve')]"));
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
			WebElement element = driver.findElement(By.xpath("//*[@id=\"DB1_2\"]"));
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
			By elemPath = By.id("DB1_2");
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
	    	username.sendKeys("franz.valois");
	    	password.sendKeys("Gingerbread0929@");
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
			Thread.sleep(2000);
			endRec = Instant.now();timeElapsedRec = Duration.between(startRec, endRec);
			 if (!error.isEmpty()) {
				 update.setString(1, error);
				}else {
				update.setString(1, "DONE");	
			 }

			 update.setString(2, requestIdStr);
	         update.setString(3, currentStatus);
	         update.setLong(4, timeElapsedRec.toMillis());
		     update.setString(5, thread);
	         update.setString(6, id);
	         Thread.sleep(2000);
	         break;
		} catch (Exception e) {
			System.out.println("[RETRY] UPDATE FAILED");
		}
		}		
	}

    public static  void locCons(List<String> dataArryVal) {
  	   try {
  		if(dataList.get(38).toLowerCase().contains("no")) {
          	WebElement element = driver.findElement(By.id("REQD.P.WFM_LOCTAION_CONTRACTUALLY_N"));
             	System.out.println("RECORD ["+id+"] - PROJECT ID ["+requestIdStr+"] >> [Location Constrained >> No]");
             	element.click();
             	//Clicking Client interview as well
             	System.out.println("Client Interview");
 				Select DropDown = new Select(driver.findElement(By.id("REQD.P.CLIENT_INTRW")));
 				DropDown.selectByIndex(0);
 				DropDown.selectByVisibleText(dataList.get(44));
 				
          	}
      	else {
          	WebElement element = driver.findElement(By.id("REQD.P.WFM_LOCTAION_CONTRACTUALLY_Y"));
          	System.out.println("RECORD ["+id+"] - PROJECT ID ["+requestIdStr+"] >> [Location Constrained >> Yes]");
          	element.click();
          	//Clicking Client interview as well
             	System.out.println("Client Interview");
 				Select DropDown = new Select(driver.findElement(By.id("REQD.P.CLIENT_INTRW")));
 				DropDown.selectByIndex(0);
 				DropDown.selectByVisibleText(dataList.get(44));
          	            	
          	}
  	   }catch(Exception e) {
  		   System.out.println("Contractually constraint button not found/Client interview drop down not found");
  	   }
          	
          	}
 
    public static WebElement indicatorES(List<String> dataArryVal) {
    	for (int x= 0; x< 20; x++) {
    		try {
    		if(dataList.get(42).toLowerCase().contains("no")) {
            	//Early Staffing Indicator to No
            	WebDriverWait wait = new WebDriverWait(driver, 10);
            	By elemPath = By.id("REQD.P.WFM_EARLY_STAFF_FLAG_N");
            	WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
            	wait.until(ExpectedConditions.elementToBeClickable(elem));
            	WebElement element = driver.findElement(By.id("REQD.P.WFM_EARLY_STAFF_FLAG_N"));
            	System.out.println("RECORD ["+id+"] - PROJECT ID ["+requestIdStr+"] >> [Early Staffing >> No]");
            	return element;
            	}
        	else {
            	//Early Staffing Indicator to Yes
            	WebDriverWait wait = new WebDriverWait(driver, 10);
            	By elemPath = By.id("REQD.P.WFM_EARLY_STAFF_FLAG_Y");
            	WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
            	wait.until(ExpectedConditions.elementToBeClickable(elem));
            	WebElement element = driver.findElement(By.id("REQD.P.WFM_EARLY_STAFF_FLAG_Y"));
            	System.out.println("RECORD ["+id+"] - PROJECT ID ["+requestIdStr+"] >> [Early Staffing >> Yes]");
            	return element;
            	}
            	}catch (Exception e) {
            	driver.navigate().refresh();
            	System.out.println("[WAITING] Ready for Approval");
            	}
            	}
            	return null;
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
		for (int x = 0; x < 5; x++) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, 10);
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
				searchTextBoxPrimarySkill.clear();
				searchTextBoxPrimarySkill.sendKeys("DXC-ITIL GENERAL");

				searchTextBoxPrimarySkill.sendKeys(Keys.TAB);
				
				System.out.println("Using Default Value for Secondary Skill");
				searchTextBoxSecondarySkill.clear();
				searchTextBoxSecondarySkill.sendKeys("DXC-MICROSOFT OFFICE SUITE");

				searchTextBoxSecondarySkill.sendKeys(Keys.TAB);
				//clear skill error
				error="";
			}
			
			if(typeValueSecondary.isEmpty())
			{
				
				System.out.println("Using Default Value for Secondary Skill");
				searchTextBoxSecondarySkill.sendKeys("DXC-MICROSOFT OFFICE SUITE");

				searchTextBoxSecondarySkill.sendKeys(Keys.TAB);
				//clear skill error
				error="";
			}			
			By elemPath = By.xpath("//a//div[contains(text(), 'Complete PLM')]");
			WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
			wait.until(ExpectedConditions.elementToBeClickable(elem));
			WebElement element = driver.findElement(By.xpath("//a//div[contains(text(), 'Complete PLM')]"));
			System.out.println("RECORD ["+id+"] - REQUEST ID ["+requestIdStr+"] >> [COMPLETE PLM]");
			return element;		
			
		} catch (Exception e) {
			//driver.navigate().refresh();
			System.out.println("[ERROR] COMPLETE PLM BUTTON NOT CLICKABLE - APPLYING DEFAULT VALUES");
			
		}
//		//check if status is changed
//		currentStatus = statusWait();
//		if(!currentStatus.trim().contains("In Planning")) {
//			return null;
//		}
		}
		return null;
	}

	public static WebElement releaseForAppvl() {
		for (int x = 0; x < 20; x++) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, 20);
			By elemPath = By.xpath("//a//div[contains(text(), 'Release for Approval')]");
			WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
			wait.until(ExpectedConditions.elementToBeClickable(elem));
			WebElement element = driver.findElement(By.xpath("//a//div[contains(text(), 'Release for Approval')]"));
			System.out.println("RECORD ["+id+"] - PROJECT ID ["+requestIdStr+"] >> [RELEASE FOR APPROVAL]");
			return element;
		} catch (Exception e) {
			//driver.navigate().refresh();
			error="[ERROR] CHECK PLM - ISSUE WHEN DEMAND MOVED FOR APPROVAL";
			statusElemWait();currentStatus = statusWait();
			if(!currentStatus.trim().contains("Ready for Approval")) {
				break;
			}
			System.out.println("[WAITING] RELEASE FOR APPROVAL BUTTON");
		}
		}
		return null;
	}
	
//	   public static boolean approveAEBtn() {
//			for (int x = 0; x < 5; x++) {
//			try {
//				WebDriverWait wait = new WebDriverWait(driver, 5);
//				By elemPath = By.id("DB1_0");
//				WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
//				System.out.println("Approval Button Activated: "+ elem.isDisplayed());
//				if (elem.isDisplayed()) {
//					return true;
//				}else{
//
//					error="Approval button not active";
//					return false;
//				}
//			} catch (Exception e) {
//			}
//			}
//			return false;
//		}
	   
		 public static WebElement approveAE() {
		  		for (int x = 0; x < 20; x++) {
		  		try {
		  			WebDriverWait wait = new WebDriverWait(driver, 15);
		  			By elemPath = By.xpath("//a//div[contains(text(), 'Approve')]");
		  			WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
		  			wait.until(ExpectedConditions.elementToBeClickable(elem));
		  			WebElement element = driver.findElement(By.xpath("//a//div[contains(text(), 'Approve')]"));
		  			System.out.println("RECORD ["+id+"] - PROJECT ID ["+requestIdStr+"] >> [Approved ADL]");
		  			return element;
		  		} catch (Exception e) {
		  			driver.navigate().refresh();
		  			statusElemWait();currentStatus = statusWait();
					if(!currentStatus.trim().contains("Pending AE Approval")) {
						break;
					}
		  			System.out.println("[WAITING] Approval BUTTON");
		  		}
		  		}
		  		return null;
		  	}	   
	public static WebElement approveADL() {
		for (int x = 0; x < 20; x++) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, 15);
			By elemPath = By.xpath("//a//div[contains(text(), 'Approve')]");
			WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
			wait.until(ExpectedConditions.elementToBeClickable(elem));
			WebElement element = driver.findElement(By.xpath("//a//div[contains(text(), 'Approve')]"));
			System.out.println("RECORD ["+id+"] - PROJECT ID ["+requestIdStr+"] >> [Approved ADL]");
			return element;
		} catch (Exception e) {
			driver.navigate().refresh();
			statusElemWait();currentStatus = statusWait();
			if(!currentStatus.trim().contains("Pending ADL Approval")) {
				break;
			}
			System.out.println("[WAITING] Approval BUTTON");
		}
		}
		return null;
	}
	
	public static WebElement moveToSp() {
		for (int x = 0; x < 20; x++) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, 15);
				By elemPath = By.xpath("//a//div[contains(text(), 'Move to SP')]");
				WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
				wait.until(ExpectedConditions.elementToBeClickable(elem));
				WebElement element = driver.findElement(By.xpath("//a//div[contains(text(), 'Move to SP')]"));
				System.out.println("RECORD ["+id+"] - PROJECT ID ["+requestIdStr+"] >> [Move to SP]");
				return element;
			
		} catch (Exception e) {
			driver.navigate().refresh();
			System.out.println("[WAITING] Move to SP BUTTON");
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
			viewButtonExpand().click();Thread.sleep(1000);
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
	
	public static WebElement viewButtonExpand() {
		for (int x = 0; x < 20; x++) { 
			try {
				WebDriverWait wait = new WebDriverWait(driver, 5);			
				By elemPath = By.id("BT_VIEW_P_1");
				WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
				wait.until(ExpectedConditions.elementToBeClickable(elem));
				WebElement element = driver.findElement(By.id("BT_VIEW_P_1"));
				return element;
			} catch (Exception e) {
				System.out.println("[WAITING] View Button");
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
			//Too many result rows were returned for SQL in Rule 90
			for (int x = 0; x < 3; x++) {
			try {
				WebDriverWait wait = new WebDriverWait(driver, 3);
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
				WebDriverWait wait = new WebDriverWait(driver, 3);
				Alert alert = wait.until(ExpectedConditions.alertIsPresent());
				alert.accept();
				break;
			} catch (Exception e) {
			}
		   }
		
	}
	
		
	
		public static void invalidataHandler() {
			try {
				Thread.sleep(3000);
				List<WebElement> iframe=driver.findElements(By.tagName("iframe"));
				 for(int i=0; i<=iframe.size(); i++){
					 if (iframe.get(i).getAttribute("id").equals("autoCompleteDialogIF")){
						 	Thread.sleep(1000);
						    iframe.get(i).sendKeys(Keys.ESCAPE);
					        Thread.sleep(1000);
					      //error="[Error] Data provided missing or not found";
					     System.out.println("RECORD ["+id+"] - PROJECT ID ["+requestIdStr+"] >> [DATA ISSUE]");
					     ongoingUpate();
		                 update.executeUpdate();
						 break;
					 }
				 }
			} catch (Exception e) {
			}
		 }
		
		public static WebElement reDefine() {
			for (int x = 0; x < 20; x++) {
			try {
				WebDriverWait wait = new WebDriverWait(driver, 15);
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
		for (int x = 0; x < 10; x++) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, 10);
			By elemPath = By.xpath("//div[contains(text(), 'ReDefine Demand')]");
			WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
			if (elem.isDisplayed()) {
				return true;
			}else{
				return false;
			}
		} catch (Exception e) {
			driver.navigate().refresh();
			error="[ERROR] FAILED TO REDEFINE DEMAND!!!";
		}
		}
		return false;
	}
	
	public static boolean reworkOnPLM() {
		for (int x = 0; x < 5; x++) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, 10);
			By elemPath = By.xpath("//*[@id=\"DB0_1\"]");
			WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(elemPath));
			if (elem.isDisplayed()) {
				return true;
			}else{
				return false;
			}
		} catch (Exception e) {
			error="[ERROR] FAILED REWORK ON PLM - BUTTON MISSING";
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
	
//	public static boolean approveBtn() {
//		for (int x = 0; x < 20; x++) {
//		try {
//			Thread.sleep(100);
//			WebDriverWait wait = new WebDriverWait(driver, 10);
//			By elemPath = By.id("DB1_0");
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
	
	public static boolean accessError() {
		for (int x = 0; x < 20; x++) {
		try {
			Thread.sleep(100);
			WebDriverWait wait = new WebDriverWait(driver, 5);
			Boolean isPresent = driver.findElements(By.xpath("//*[@id=\"page-min-width-div\"]/div[5]/div/table/tbody/tr[3]/td[3]/pre")).size() > 0;
			System.out.println("ispresent"+isPresent);
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

